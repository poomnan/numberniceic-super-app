package handlers

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"log"
	"net/http"
	"os"
	"strings"
	"sync"
	"time"

	"github.com/lib/pq"
)

// ─── In-memory cache (TTL 30 min) ────────────────────────────────────────────

type cacheEntry struct {
	data      NameKeywordsResponse
	expiresAt time.Time
}

var labCache sync.Map // key: string(name) → cacheEntry

func getCached(name string) (NameKeywordsResponse, bool) {
	if v, ok := labCache.Load(name); ok {
		entry := v.(cacheEntry)
		if time.Now().Before(entry.expiresAt) {
			return entry.data, true
		}
		labCache.Delete(name)
	}
	return NameKeywordsResponse{}, false
}

func setCached(name string, data NameKeywordsResponse) {
	labCache.Store(name, cacheEntry{data: data, expiresAt: time.Now().Add(30 * time.Minute)})
}

// ─── AI response structure ────────────────────────────────────────────────────

type NameAnalysis struct {
	Roots          string   `json:"roots"`           // e.g. "ณ เดช เดชน์"  → for pg_trgm
	MeaningSummary string   `json:"meaning_summary"` // terse meaning   → for embedding
	Keywords       []string `json:"keywords"`        // display chips
}

// ─── API response structure ───────────────────────────────────────────────────

type NameKeywordsResponse struct {
	Name           string       `json:"name"`
	Roots          string       `json:"roots"`
	MeaningSummary string       `json:"meaning_summary"`
	Keywords       []string     `json:"keywords"`
	Similar        []HybridItem `json:"similar"`
}

type HybridItem struct {
	Name          string   `json:"name"`
	Meaning       string   `json:"meaning"`
	Gender        string   `json:"gender"`
	SatSum        int      `json:"sat_sum"`
	ShaSum        int      `json:"sha_sum"`
	RootScore     float64  `json:"root_score"`     // trigram similarity
	SemanticScore float64  `json:"semantic_score"` // 1 - cosine_distance
	FinalRank     float64  `json:"final_rank"`     // weighted hybrid score
	KakiChars     []string `json:"kaki_chars"`     // kaki chars found in this name for the selected day
}

// ─── Handler ─────────────────────────────────────────────────────────────────

// GetNameKeywordsHandler implements a 3-phase pipeline:
//  1. AI → structured JSON  { roots, meaning_summary, keywords }
//  2. Embedding  meaning_summary → vector
//  3. Hybrid SQL  word_similarity(roots, thname)*0.4  +  (1-cosine)*0.6  = final_rank
func GetNameKeywordsHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	name := strings.TrimSpace(r.URL.Query().Get("name"))
	if name == "" {
		http.Error(w, "name parameter required", http.StatusBadRequest)
		return
	}

	goodSat := r.URL.Query().Get("good_sat") == "true"
	goodSha := r.URL.Query().Get("good_sha") == "true"
	day := r.URL.Query().Get("day")
	filterKaki := r.URL.Query().Get("filter_kaki") == "true"

	// Build kaki column filter (same dayMap as demo_search_handler)
	kakiFilter := ""
	if filterKaki && day != "" {
		dayMap := map[string]string{
			"Sunday":     "k_sunday",
			"Monday":     "k_monday",
			"Tuesday":    "k_tuesday",
			"Wednesday1": "k_wednesday1",
			"Wednesday2": "k_wednesday2",
			"Thursday":   "k_thursday",
			"Friday":     "k_friday",
			"Saturday":   "k_saturday",
		}
		if col, ok := dayMap[day]; ok {
			kakiFilter = fmt.Sprintf(" AND %s = false", col)
		}
	}

	// ── Check Cache (Include all filters in key) ─────────────────────────
	cacheKey := fmt.Sprintf("%s|sat:%v|sha:%v|day:%s|kaki:%v", name, goodSat, goodSha, day, filterKaki)
	if cachedData, ok := getCached(cacheKey); ok {
		log.Printf("[name-keywords] Cache HIT for '%s'", cacheKey)
		jsonResponse(w, http.StatusOK, cachedData)
		return
	}

	apiKey := os.Getenv("OPENAI_API_KEY")
	if apiKey == "" {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "API Key missing"})
		return
	}

	// ── Phase 1+2 · AI + Embedding in TRUE PARALLEL ──────────────────────────
	start := time.Now()
	// Start AI and Embedding simultaneously to cut latency.

	// Ultra-fast prompt: strictly JSON, Thai instructions to ensure Thai output.
	etymologyPrompt := fmt.Sprintf(`Role: คุณคือผู้เชี่ยวชาญด้านนิรุกติศาสตร์ (Etymology) และภาษาศาสตร์ไทย

Task: จาก "ชื่อ" ที่กำหนดให้ ให้ทำ 3 อย่างดังนี้:
1. ถอดรากศัพท์แยกเป็นส่วนๆ (Sub-words)
2. สรุปความหมายโดยรวมแบบกะทัดรัด (เพื่อเอาไปทำ Vector)
3. ส่งผลลัพธ์กลับมาในรูปแบบ JSON เท่านั้น

Input Name: %s

JSON Format:
{
"roots": "ส่วนประกอบของชื่อที่ใช้หาคำใกล้เคียงได้ เช่น 'ณ เดช เดชน์'",
"meaning_summary": "นิยามสั้นๆ ของความหมายเพื่อใช้ค้นหาเชิงบริบท",
"keywords": ["คำหลัก1", "คำหลัก2"]
}`, name)

	type aiResult struct {
		raw string
		err error
	}
	type embedResult struct {
		vec []float64
		err error
	}

	// Fire both concurrently
	aiCh := make(chan aiResult, 1)
	go func() {
		raw, err := callOpenAIFast(apiKey, etymologyPrompt, 300)
		aiCh <- aiResult{raw, err}
	}()

	// 1. Wait for AI
	aiRes := <-aiCh
	log.Printf("[name-keywords] AI took %v", time.Since(start))
	rawJSON := aiRes.raw
	if aiRes.err != nil {
		log.Printf("[name-keywords] AI error for '%s': %v", name, aiRes.err)
		rawJSON = fmt.Sprintf(`{"roots":"%s","meaning_summary":"%s","keywords":["มงคล","ความดี"]}`, name, name)
	}

	// Strip markdown fences if any
	rawJSON = strings.TrimSpace(rawJSON)
	rawJSON = strings.TrimPrefix(rawJSON, "```json")
	rawJSON = strings.TrimPrefix(rawJSON, "```")
	rawJSON = strings.TrimSuffix(rawJSON, "```")
	rawJSON = strings.TrimSpace(rawJSON)

	// NameAnalysis is the internal structure for AI parsing.
	type NameAnalysisRaw struct {
		Roots          interface{} `json:"roots"`
		MeaningSummary string      `json:"meaning_summary"`
		Keywords       []string    `json:"keywords"`
	}

	var rawAnalysis NameAnalysisRaw
	if err := json.Unmarshal([]byte(rawJSON), &rawAnalysis); err != nil {
		log.Printf("[name-keywords] JSON parse error: %v | raw: %s", err, rawJSON)
		rawAnalysis = NameAnalysisRaw{
			Roots:          name,
			MeaningSummary: name,
			Keywords:       []string{"มงคล", "ความดี", "เจริญรุ่งเรือง"},
		}
	}

	// Sanitize roots: if array, join it
	var cleanRootsStr string
	switch v := rawAnalysis.Roots.(type) {
	case string:
		cleanRootsStr = v
	case []interface{}:
		var parts []string
		for _, p := range v {
			if s, ok := p.(string); ok {
				parts = append(parts, s)
			}
		}
		cleanRootsStr = strings.Join(parts, " ")
	default:
		cleanRootsStr = name
	}

	analysis := NameAnalysis{
		Roots:          cleanRootsStr,
		MeaningSummary: rawAnalysis.MeaningSummary,
		Keywords:       rawAnalysis.Keywords,
	}

	log.Printf("[name-keywords] '%s' → roots='%s' meaning='%s' kw=%v",
		name, analysis.Roots, analysis.MeaningSummary, analysis.Keywords)

	// 2. Fetch Embedding for MeaningSummary
	var embedding []float64
	embedStart := time.Now()
	// Fallback to name if meaning_summary is empty
	embedText := analysis.MeaningSummary
	if embedText == "" {
		embedText = name
	}
	vec, err := services.GetEmbedding(embedText)
	if err == nil {
		embedding = vec
	} else {
		log.Printf("[name-keywords] Name embed error: %v", err)
		embedding = make([]float64, 1536)
	}
	log.Printf("[name-keywords] Embedding ready after %v", time.Since(embedStart))

	vectorStr := formatVector(embedding)

	// Clean roots: remove commas/punctuation so pg_trgm gets clean Thai tokens
	cleanRoots := strings.NewReplacer(",", " ", ".", " ", ";", " ", "/", " ").Replace(analysis.Roots)
	cleanRoots = strings.Join(strings.Fields(cleanRoots), " ")

	// ── Phase 3 · Hybrid SQL (Trigram 40% + Vector 60%) ──────────────────────
	//
	// SQL Optimization: Apply filters DIRECTLY in candidate queries to ensure we get
	// enough results when filters are on.

	var goodSums []int
	if goodSat || goodSha {
		goodSums, _ = services.GetGoodSums()
	}

	// Dynamic Auspicious Bonus — loaded from DB pairtype
	d10Sums, d8Sums, d5Sums, _ := services.GetSumsByPairType()
	aucBonusSQL := buildAuspiciousBonus(d10Sums, d8Sums, d5Sums)

	filterSQL := ""
	queryArgs := []interface{}{cleanRoots, vectorStr, name}
	argIdx := 4

	if goodSat {
		filterSQL += fmt.Sprintf(" AND sat_sum = ANY($%d) ", argIdx)
		queryArgs = append(queryArgs, pq.Array(goodSums))
		argIdx++
	}
	if goodSha {
		filterSQL += fmt.Sprintf(" AND sha_sum = ANY($%d) ", argIdx)
		queryArgs = append(queryArgs, pq.Array(goodSums))
		argIdx++
	}
	// kaki filter uses a boolean column (no query arg needed)
	filterSQL += kakiFilter

	query := fmt.Sprintf(`
		WITH ai_output AS (
			SELECT $1::text AS ai_roots
		),
		target_vector AS (
			SELECT $2::vector AS vec 
		),
		top_semantic AS (
			SELECT thname AS name, COALESCE(meaning, '') AS meaning, COALESCE(gender, '') AS gender, sat_sum, sha_sum,
				word_similarity((SELECT ai_roots FROM ai_output), thname) AS root_score,
				GREATEST(0, 1 - (meaning_vector <=> (SELECT vec FROM target_vector))) AS semantic_score
			FROM names_miracle
			WHERE thname != $3 %s
			ORDER BY meaning_vector <=> (SELECT vec FROM target_vector)
			LIMIT 100
		),
		top_trigram AS (
			SELECT thname AS name, COALESCE(meaning, '') AS meaning, COALESCE(gender, '') AS gender, sat_sum, sha_sum,
				word_similarity((SELECT ai_roots FROM ai_output), thname) AS root_score,
				GREATEST(0, 1 - (meaning_vector <=> (SELECT vec FROM target_vector))) AS semantic_score
			FROM names_miracle
			WHERE thname != $3 %s
			ORDER BY thname <-> (SELECT ai_roots FROM ai_output)
			LIMIT 100
		),
		combined AS (
			SELECT * FROM top_semantic
			UNION
			SELECT * FROM top_trigram
		),
		ranked AS (
			SELECT
				name, meaning, gender, sat_sum, sha_sum, root_score, semantic_score,
				((root_score * 0.4) + (semantic_score * 0.6)) AS base_score,
				-- Auspicious Bonus: based on pairtype from DB (D10 > D8 > D5)
				%s AS auspicious_bonus,
				-- Length Adjust: ชื่อ 2-3 พยางค์ (<=8 char) ง่ายต่อการเรียก = บวก, ยาวมาก (>=12) = ลบ
				CASE
					WHEN char_length(name) <= 6  THEN 0.07
					WHEN char_length(name) <= 8  THEN 0.04
					WHEN char_length(name) <= 10 THEN 0.0
					WHEN char_length(name) <= 12 THEN -0.02
					ELSE -0.05
				END AS length_adjust
			FROM combined
		)
		SELECT
			name, meaning, gender, sat_sum, sha_sum, root_score, semantic_score,
			LEAST(1.0, base_score + auspicious_bonus + length_adjust) AS final_rank
		FROM ranked
		ORDER BY final_rank DESC
		LIMIT 15;
	`, filterSQL, filterSQL, aucBonusSQL)

	queryStart := time.Now()
	rows, err := database.DB.Query(query, queryArgs...)
	if err != nil {
		log.Printf("[name-keywords] Hybrid query error: %v | SQL: %s", err, query)
		jsonResponse(w, http.StatusOK, NameKeywordsResponse{
			Name:           name,
			Roots:          analysis.Roots,
			MeaningSummary: analysis.MeaningSummary,
			Keywords:       analysis.Keywords,
			Similar:        []HybridItem{},
		})
		return
	}
	log.Printf("[name-keywords] DB Queries took %v", time.Since(queryStart))
	defer rows.Close()

	var similar []HybridItem
	for rows.Next() {
		var item HybridItem
		if err := rows.Scan(
			&item.Name, &item.Meaning, &item.Gender,
			&item.SatSum, &item.ShaSum,
			&item.RootScore, &item.SemanticScore, &item.FinalRank,
		); err != nil {
			log.Printf("[name-keywords] row scan error: %v", err)
			continue
		}
		similar = append(similar, item)
	}
	if similar == nil {
		similar = []HybridItem{}
	}

	// Mark kaki chars in each result when day is selected (even if filter_kaki=false)
	// This allows frontend to highlight kaki chars without blocking them
	if day != "" {
		kakiSet := loadKakiSet(day)
		for i := range similar {
			var found []string
			seen := make(map[string]bool)
			for _, r := range []rune(similar[i].Name) {
				ch := string(r)
				if kakiSet[ch] && !seen[ch] {
					found = append(found, ch)
					seen[ch] = true
				}
			}
			if found != nil {
				similar[i].KakiChars = found
			} else {
				similar[i].KakiChars = []string{}
			}
		}
	}

	finalResponse := NameKeywordsResponse{
		Name:           name,
		Roots:          analysis.Roots,
		MeaningSummary: analysis.MeaningSummary,
		Keywords:       analysis.Keywords,
		Similar:        similar,
	}

	// Save to Cache (using cacheKey instead of name)
	setCached(cacheKey, finalResponse)

	jsonResponse(w, http.StatusOK, finalResponse)
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

func buildInClause(nums []int) string {
	if len(nums) == 0 {
		return ""
	}
	parts := make([]string, len(nums))
	for i, v := range nums {
		parts[i] = fmt.Sprintf("%d", v)
	}
	return strings.Join(parts, ",")
}

// fallbackVectorQuery runs a vector-only search when the hybrid query fails (e.g. pg_trgm not available)
func fallbackVectorQuery(excludeName, vectorStr, goodSumFilter string) (*sql.Rows, error) {
	query := fmt.Sprintf(`
		SELECT
			nm.thname,
			COALESCE(nm.meaning, '')                                   AS meaning,
			COALESCE(nm.gender, '')                                    AS gender,
			nm.sat_sum,
			nm.sha_sum,
			0.0                                                        AS root_score,
			GREATEST(0, 1-(nm.meaning_vector <=> $1::vector))          AS semantic_score,
			GREATEST(0, 1-(nm.meaning_vector <=> $1::vector))          AS final_rank
		FROM names_miracle nm
		WHERE nm.meaning_vector IS NOT NULL
		  AND char_length(COALESCE(nm.meaning, '')) >= 20
		  AND nm.thname != $2
		  AND length(nm.thname) >= 2
		  AND nm.thname ~ '^[^a-zA-Z]+$'
		  %s
		  AND (nm.meaning_vector <=> $1::vector) < 0.55
		ORDER BY final_rank DESC
		LIMIT 15
	`, goodSumFilter)
	return database.DB.Query(query, vectorStr, excludeName)
}

// buildAuspiciousBonus generates a CASE WHEN SQL expression for sat_sum bonus
// based on pairtype tiers from the DB. D10 = +0.15, D8 = +0.10, D5 = +0.05
func buildAuspiciousBonus(d10, d8, d5 []int) string {
	toList := func(nums []int) string {
		if len(nums) == 0 {
			return ""
		}
		parts := make([]string, len(nums))
		for i, n := range nums {
			parts[i] = fmt.Sprintf("%d", n)
		}
		return strings.Join(parts, ",")
	}

	d10List := toList(d10)
	d8List := toList(d8)
	d5List := toList(d5)

	sql := "CASE"
	if d10List != "" {
		sql += fmt.Sprintf("\n\t\t\t\t\tWHEN sat_sum IN (%s) THEN 0.15", d10List)
	}
	if d8List != "" {
		sql += fmt.Sprintf("\n\t\t\t\t\tWHEN sat_sum IN (%s) THEN 0.10", d8List)
	}
	if d5List != "" {
		sql += fmt.Sprintf("\n\t\t\t\t\tWHEN sat_sum IN (%s) THEN 0.05", d5List)
	}
	sql += "\n\t\t\t\t\tELSE 0\n\t\t\t\tEND"
	return sql
}

// loadKakiSet queries the kakis_day table and returns a set of kaki characters for the given day.
// The day parameter should be in English format (e.g. "Monday", "Tuesday").
func loadKakiSet(day string) map[string]bool {
	dayThai := map[string]string{
		"Sunday":     "อาทิตย์",
		"Monday":     "จันทร์",
		"Tuesday":    "อังคาร",
		"Wednesday1": "พุธกลางวัน",
		"Wednesday2": "พุธกลางคืน",
		"Thursday":   "พฤหัสบดี",
		"Friday":     "ศุกร์",
		"Saturday":   "เสาร์",
	}
	kakiSet := make(map[string]bool)
	rows, err := database.DB.Query(
		"SELECT kakis FROM kakis_day WHERE LOWER(day) = LOWER($1) OR day_th = $2",
		day, dayThai[day],
	)
	if err != nil {
		return kakiSet
	}
	defer rows.Close()
	for rows.Next() {
		var kakiChar string
		if err := rows.Scan(&kakiChar); err == nil {
			for _, r := range strings.TrimSpace(kakiChar) {
				kakiSet[string(r)] = true
			}
		}
	}
	return kakiSet
}
