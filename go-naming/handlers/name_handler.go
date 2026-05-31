package handlers

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"go-naming/database"
	"go-naming/models"
	"go-naming/services"
	"log"
	"net/http"
	"os"
	"sort"
	"strconv"
	"strings"
	"unicode/utf8"

	"github.com/lib/pq"
)

type NameInputResolveResponse struct {
	Input               string               `json:"input"`
	InputType           string               `json:"input_type"`
	FirstName           string               `json:"first_name,omitempty"`
	Surname             string               `json:"surname,omitempty"`
	SearchMode          string               `json:"search_mode,omitempty"`
	SeedName            string               `json:"seed_name,omitempty"`
	SemanticQuery       string               `json:"semantic_query,omitempty"`
	SemanticMeaning     string               `json:"semantic_meaning,omitempty"`
	ExistsInDatabase    bool                 `json:"exists_in_database"`
	CanDecode           bool                 `json:"can_decode"`
	CanRankFromTemplate bool                 `json:"can_rank_from_template"`
	SuggestionStrategy  string               `json:"suggestion_strategy"`
	DBMeaning           string               `json:"db_meaning,omitempty"`
	Intent              *services.Result     `json:"intent,omitempty"`
	Decode              *models.DecodeResult `json:"decode,omitempty"`
}

func looksLikeTypedThaiName(input string) bool {
	input = strings.TrimSpace(input)
	if input == "" || strings.ContainsAny(input, " \t\n\r") {
		return false
	}
	runeCount := utf8.RuneCountInString(input)
	if runeCount < 2 || runeCount > 12 {
		return false
	}
	for _, r := range input {
		if r < 'ก' || r > '๙' {
			return false
		}
	}
	return true
}

func GetNameInputResolveHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	input := strings.TrimSpace(r.URL.Query().Get("input"))
	if input == "" {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "input parameter required"})
		return
	}
	day := strings.TrimSpace(r.URL.Query().Get("day"))

	classification := services.ClassifyInput(input, database.DB)
	lookupName := input
	if strings.TrimSpace(classification.SeedName) != "" {
		lookupName = strings.TrimSpace(classification.SeedName)
	} else if classification.Type == "full_name" && strings.TrimSpace(classification.FirstName) != "" {
		lookupName = strings.TrimSpace(classification.FirstName)
	} else if classification.Type == "single_name" && strings.TrimSpace(classification.FirstName) != "" {
		lookupName = strings.TrimSpace(classification.FirstName)
	}

	var dbMeaning string
	existsInDB := false
	if err := database.DB.QueryRow("SELECT COALESCE(meaning, '') FROM names_miracle WHERE thname = $1 LIMIT 1", lookupName).Scan(&dbMeaning); err == nil && strings.TrimSpace(dbMeaning) != "" {
		existsInDB = true
		dbMeaning = strings.TrimSpace(dbMeaning)
	}

	intent, err := services.DetectNameIntent(r.Context(), database.DB, input)
	if err != nil {
		log.Printf("GetNameInputResolveHandler intent error input=%q: %v", input, err)
		intent = services.Result{Mode: "MEANING", Confidence: 0, Candidates: []string{}, BestScore: 0}
	}
	if classification.Type == "full_name" {
		intent = services.Result{
			Mode:       "NAME",
			Confidence: classification.Confidence,
			Candidates: []string{classification.FirstName},
			BestScore:  classification.Confidence,
		}
	}
	typedName := looksLikeTypedThaiName(input)
	inputType := "meaning"
	switch {
	case classification.Type == "full_name":
		inputType = "full_name"
	case classification.Type == "single_name" || existsInDB || typedName || intent.Mode == "NAME":
		inputType = "name"
	case classification.Type == "meaning":
		inputType = "meaning"
	case intent.Mode == "HYBRID":
		inputType = "hybrid"
	}

	canDecode := inputType == "name" || inputType == "hybrid" || inputType == "full_name"
	canRankFromTemplate := existsInDB || inputType == "meaning" || inputType == "full_name"
	suggestionStrategy := "semantic"
	switch {
	case inputType == "full_name":
		suggestionStrategy = "hybrid"
	case existsInDB:
		suggestionStrategy = "hybrid"
	case inputType == "name" && !existsInDB:
		suggestionStrategy = "pg_trgm"
	case inputType == "hybrid":
		suggestionStrategy = "hybrid"
	}

	var decode *models.DecodeResult
	if canDecode {
		decodeTarget := lookupName
		if decodeTarget == "" {
			decodeTarget = input
		}
		if decoded, err := services.DecodeName(decodeTarget, day); err == nil {
			decode = decoded
		} else {
			log.Printf("GetNameInputResolveHandler decode error input=%q target=%q: %v", input, decodeTarget, err)
		}
	}

	resp := NameInputResolveResponse{
		Input:               input,
		InputType:           inputType,
		FirstName:           classification.FirstName,
		Surname:             classification.Surname,
		SearchMode:          classification.SearchMode,
		SeedName:            classification.SeedName,
		SemanticQuery:       classification.SemanticQuery,
		SemanticMeaning:     classification.SemanticQuery,
		ExistsInDatabase:    existsInDB,
		CanDecode:           canDecode,
		CanRankFromTemplate: canRankFromTemplate,
		SuggestionStrategy:  suggestionStrategy,
		DBMeaning:           dbMeaning,
		Intent:              &intent,
		Decode:              decode,
	}
	jsonResponse(w, http.StatusOK, resp)
}

// GetNameSuggestionsHandler returns name suggestions based on semantic similarity (embeddings)
func GetNameSuggestionsHandler(w http.ResponseWriter, r *http.Request) {
	q := r.URL.Query().Get("q")
	mean := r.URL.Query().Get("meaning")
	if q == "" && mean == "" {
		jsonResponse(w, http.StatusOK, map[string]interface{}{"ideas": []string{}, "names": []string{}})
		return
	}

	// Use meaning for semantic search if available, otherwise use name
	queryText := q
	if mean != "" {
		queryText = mean
	} else if !strings.Contains(q, " ") && q != "" {
		// Fallback: If it's a single word (likely a name) and meaning is missing, lookup
		var dbMeaning string
		err := database.DB.QueryRow("SELECT meaning FROM names_miracle WHERE thname = $1 LIMIT 1", q).Scan(&dbMeaning)
		if err == nil && dbMeaning != "" {
			queryText = dbMeaning
			log.Printf("GetNameSuggestionsHandler: Auto-fetched Semantic Meaning for '%s': '%s'", q, queryText)
		}
	}

	// Define response structure
	type SuggestionItem struct {
		ID                int    `json:"id"`
		Name              string `json:"name"`
		Meaning           string `json:"meaning"`
		PhoneticSummary   string `json:"phonetic_summary,omitempty"`
		PhoneticScore     *int   `json:"phonetic_score,omitempty"`
		PronunciationEase *int   `json:"pronunciation_ease,omitempty"`
		EuphonyScore      *int   `json:"euphony_score,omitempty"`
		RhythmScore       *int   `json:"rhythm_score,omitempty"`
		RankScore         int    `json:"rank_score"`
	}
	type SuggestionRes struct {
		Ideas []string         `json:"ideas"`
		Names []SuggestionItem `json:"names"`
	}

	var res SuggestionRes
	res.Ideas = []string{}
	res.Names = []SuggestionItem{}

	normalizedQuery := strings.TrimSpace(q)
	candidates := make(map[string]suggestionCandidate)

	// 1. Semantic search for meanings (embeddings). If embedding is unavailable,
	// keep going with pg_trgm so typed names that are not in DB still get suggestions.
	embedding, err := services.GetEmbedding(queryText)
	hasEmbedding := err == nil
	if err != nil {
		log.Printf("Error getting embedding for suggestions: %v", err)
	}

	vectorStr := ""
	if hasEmbedding {
		vectorStr = formatVector(embedding)
	}
	addCandidate := func(
		id int,
		name, meaning string,
		score float64,
		phoneticSummary sql.NullString,
		phoneticScore, pronunciationEase, euphonyScore, rhythmScore sql.NullInt64,
	) {
		name = strings.TrimSpace(name)
		if name == "" {
			return
		}

		cleanMeaning := cleanSuggestionMeaning(name, meaning)
		if strings.TrimSpace(cleanMeaning) == "" {
			return
		}

		if name == normalizedQuery {
			score += 10000
		}

		tokenCoverage := countTokenCoverage(cleanMeaning, extractSuggestionTokens(queryText, q))
		score += float64(tokenCoverage * 25)
		score += suggestionPhoneticBonus(phoneticScore, pronunciationEase, euphonyScore, rhythmScore)

		if existing, ok := candidates[name]; !ok || score > existing.Score {
			candidates[name] = suggestionCandidate{
				ID:                id,
				Name:              name,
				Meaning:           cleanMeaning,
				Score:             score,
				PhoneticSummary:   strings.TrimSpace(phoneticSummary.String),
				PhoneticScore:     nullIntToPtr(phoneticScore),
				PronunciationEase: nullIntToPtr(pronunciationEase),
				EuphonyScore:      nullIntToPtr(euphonyScore),
				RhythmScore:       nullIntToPtr(rhythmScore),
			}
		}
	}

	// 2. Exact/near-exact seed so the typed name can surface first if present.
	if normalizedQuery != "" {
		exactRows, exactErr := database.DB.Query(`
				SELECT name_id, thname, COALESCE(meaning, ''), COALESCE(phonetic_summary, ''),
				       phonetic_score, pronunciation_ease, euphony_score, rhythm_score
				FROM names_miracle
				WHERE thname = $1
				LIMIT 3
		`, normalizedQuery)
		if exactErr == nil {
			defer exactRows.Close()
			for exactRows.Next() {
				var id int
				var name, meaning string
				var phoneticSummary sql.NullString
				var phoneticScore, pronunciationEase, euphonyScore, rhythmScore sql.NullInt64
				if err := exactRows.Scan(
					&id, &name, &meaning, &phoneticSummary,
					&phoneticScore, &pronunciationEase, &euphonyScore, &rhythmScore,
				); err == nil {
					addCandidate(id, name, meaning, 5000, phoneticSummary, phoneticScore, pronunciationEase, euphonyScore, rhythmScore)
				}
			}
		}

		trigramRows, trigramErr := database.DB.Query(`
				SELECT name_id, thname, COALESCE(meaning, ''),
				       GREATEST(COALESCE(similarity(thname, $1), 0), COALESCE(word_similarity(thname, $1), 0)) AS trigram_score,
				       COALESCE(phonetic_summary, ''),
				       phonetic_score, pronunciation_ease, euphony_score, rhythm_score
				FROM names_miracle
				WHERE thname != $1
				  AND char_length(meaning) > 10
				  AND GREATEST(COALESCE(similarity(thname, $1), 0), COALESCE(word_similarity(thname, $1), 0)) > 0
				ORDER BY trigram_score DESC, char_length(thname) ASC, name_id ASC
				LIMIT 120
		`, normalizedQuery)
		if trigramErr == nil {
			defer trigramRows.Close()
			for trigramRows.Next() {
				var id int
				var name, meaning string
				var trigramScore float64
				var phoneticSummary sql.NullString
				var phoneticScore, pronunciationEase, euphonyScore, rhythmScore sql.NullInt64
				if err := trigramRows.Scan(
					&id, &name, &meaning, &trigramScore, &phoneticSummary,
					&phoneticScore, &pronunciationEase, &euphonyScore, &rhythmScore,
				); err == nil {
					addCandidate(id, name, meaning, 1400+(trigramScore*450), phoneticSummary, phoneticScore, pronunciationEase, euphonyScore, rhythmScore)
				}
			}
		} else {
			log.Printf("GetNameSuggestionsHandler: pg_trgm suggestion query failed for '%s': %v", normalizedQuery, trigramErr)
		}
	}

	// 3. Semantic core search from the meaning embedding.
	if hasEmbedding {
		semRows, semErr := database.DB.Query(`
			SELECT name_id, thname, COALESCE(meaning, ''), COALESCE((meaning_vector <=> $1), 1), COALESCE(phonetic_summary, ''),
			       phonetic_score, pronunciation_ease, euphony_score, rhythm_score
			FROM names_miracle 
			WHERE meaning_vector IS NOT NULL AND meaning != thname AND char_length(meaning) > 10
			ORDER BY meaning_vector <=> $1 
		LIMIT 200
	`, vectorStr)

		if semErr == nil {
			defer semRows.Close()
			for semRows.Next() {
				var id int
				var name, meaning string
				var distance float64
				var phoneticSummary sql.NullString
				var phoneticScore, pronunciationEase, euphonyScore, rhythmScore sql.NullInt64
				if err := semRows.Scan(
					&id, &name, &meaning, &distance, &phoneticSummary,
					&phoneticScore, &pronunciationEase, &euphonyScore, &rhythmScore,
				); err == nil {
					addCandidate(id, name, meaning, 1000-(distance*100), phoneticSummary, phoneticScore, pronunciationEase, euphonyScore, rhythmScore)
				}
			}
		} else {
			log.Printf("GetNameSuggestionsHandler: semantic suggestion query failed: %v", semErr)
		}
	}

	// 4. Keyword expansion widens the pool for names with sparse semantic neighbors.
	searchTokens := extractSuggestionTokens(queryText, q)
	if len(searchTokens) > 0 {
		patterns := make([]string, 0, len(searchTokens))
		for _, token := range searchTokens {
			patterns = append(patterns, "%"+token+"%")
		}

		keywordRows, keywordErr := database.DB.Query(`
				SELECT name_id, thname, COALESCE(meaning, ''), COALESCE(word_similarity(thname, $2), 0), COALESCE(phonetic_summary, ''),
				       phonetic_score, pronunciation_ease, euphony_score, rhythm_score
				FROM names_miracle
				WHERE char_length(meaning) > 10
				  AND (meaning ILIKE ANY($1) OR thname ILIKE ANY($1))
			ORDER BY COALESCE(word_similarity(thname, $2), 0) DESC, name_id ASC
			LIMIT 160
		`, pq.Array(patterns), normalizedQuery)
		if keywordErr == nil {
			defer keywordRows.Close()
			for keywordRows.Next() {
				var id int
				var name, meaning string
				var rootScore float64
				var phoneticSummary sql.NullString
				var phoneticScore, pronunciationEase, euphonyScore, rhythmScore sql.NullInt64
				if err := keywordRows.Scan(
					&id, &name, &meaning, &rootScore, &phoneticSummary,
					&phoneticScore, &pronunciationEase, &euphonyScore, &rhythmScore,
				); err == nil {
					addCandidate(id, name, meaning, 650+(rootScore*100), phoneticSummary, phoneticScore, pronunciationEase, euphonyScore, rhythmScore)
				}
			}
		}
	}

	ordered := make([]suggestionCandidate, 0, len(candidates))
	for _, candidate := range candidates {
		ordered = append(ordered, candidate)
	}

	sort.SliceStable(ordered, func(i, j int) bool {
		if ordered[i].Score == ordered[j].Score {
			return ordered[i].ID < ordered[j].ID
		}
		return ordered[i].Score > ordered[j].Score
	})

	seenMeaningKeys := make(map[string]bool)
	for _, candidate := range ordered {
		if len(res.Names) >= 18 {
			break
		}

		meaningKey := normalizeSuggestionMeaning(candidate.Meaning)
		isExact := candidate.Name == normalizedQuery
		if !isExact && meaningKey != "" && seenMeaningKeys[meaningKey] {
			continue
		}

		res.Names = append(res.Names, SuggestionItem{
			ID:                candidate.ID,
			Name:              candidate.Name,
			Meaning:           candidate.Meaning,
			PhoneticSummary:   candidate.PhoneticSummary,
			PhoneticScore:     candidate.PhoneticScore,
			PronunciationEase: candidate.PronunciationEase,
			EuphonyScore:      candidate.EuphonyScore,
			RhythmScore:       candidate.RhythmScore,
			RankScore:         int(candidate.Score + 0.5),
		})

		if meaningKey != "" {
			seenMeaningKeys[meaningKey] = true
		}
	}

	// If diversity is too strict for a sparse query, backfill from the ranked pool.
	if len(res.Names) < 10 {
		seenNames := make(map[string]bool)
		for _, item := range res.Names {
			seenNames[item.Name] = true
		}
		for _, candidate := range ordered {
			if len(res.Names) >= 18 {
				break
			}
			if seenNames[candidate.Name] {
				continue
			}
			res.Names = append(res.Names, SuggestionItem{
				ID:                candidate.ID,
				Name:              candidate.Name,
				Meaning:           candidate.Meaning,
				PhoneticSummary:   candidate.PhoneticSummary,
				PhoneticScore:     candidate.PhoneticScore,
				PronunciationEase: candidate.PronunciationEase,
				EuphonyScore:      candidate.EuphonyScore,
				RhythmScore:       candidate.RhythmScore,
				RankScore:         int(candidate.Score + 0.5),
			})
			seenNames[candidate.Name] = true
		}
	}

	// Heuristic for "Ideas": Populate some relevant meaning tags based on the query or results
	if len(res.Names) > 0 {
		potentialIdeas := []string{}
		ideaMap := make(map[string]bool)

		// Map of names to exclude from ideas
		nameMap := make(map[string]bool)
		for _, n := range res.Names {
			nameMap[n.Name] = true
		}

		// Add query as the first idea if it's short
		cleanQ := strings.TrimSpace(q)
		if !strings.Contains(cleanQ, " ") && len([]rune(cleanQ)) < 8 {
			potentialIdeas = append(potentialIdeas, cleanQ)
			ideaMap[cleanQ] = true
		}

		// Common stop words to exclude from ideas
		stopWords := map[string]bool{
			"แปลว่า": true, "หมายถึง": true, "คือ": true, "ผู้ที่": true, "ความ": true, "การ": true,
		}

		// Collect short tokens from meanings as ideas, excluding names
		for _, n := range res.Names {
			mTokens := strings.Fields(strings.ReplaceAll(strings.ReplaceAll(n.Meaning, ",", " "), ".", " "))
			for _, t := range mTokens {
				t = strings.Trim(t, "()[]{}")
				if len([]rune(t)) >= 2 && len([]rune(t)) <= 10 && !ideaMap[t] && !nameMap[t] && !stopWords[t] {
					potentialIdeas = append(potentialIdeas, t)
					ideaMap[t] = true
					if len(potentialIdeas) >= 6 {
						break
					}
				}
			}
			if len(potentialIdeas) >= 6 {
				break
			}
		}
		res.Ideas = potentialIdeas
	}

	jsonResponse(w, http.StatusOK, res)
}

type suggestionCandidate struct {
	ID                int
	Name              string
	Meaning           string
	Score             float64
	PhoneticSummary   string
	PhoneticScore     *int
	PronunciationEase *int
	EuphonyScore      *int
	RhythmScore       *int
}

func nullIntToPtr(value sql.NullInt64) *int {
	if !value.Valid {
		return nil
	}

	intValue := int(value.Int64)
	return &intValue
}

func suggestionPhoneticBonus(
	phoneticScore, pronunciationEase, euphonyScore, rhythmScore sql.NullInt64,
) float64 {
	bonus := 0.0
	if phoneticScore.Valid {
		bonus += float64(phoneticScore.Int64) * 0.9
	}
	if pronunciationEase.Valid {
		bonus += float64(pronunciationEase.Int64) * 0.25
	}
	if euphonyScore.Valid {
		bonus += float64(euphonyScore.Int64) * 0.2
	}
	if rhythmScore.Valid {
		bonus += float64(rhythmScore.Int64) * 0.15
	}
	if phoneticScore.Valid || pronunciationEase.Valid || euphonyScore.Valid || rhythmScore.Valid {
		bonus += 12
	}
	return bonus
}

func cleanSuggestionMeaning(name, meaning string) string {
	cleanMeaning := strings.TrimSpace(meaning)

	prefixes := []string{"แปลว่า", "หมายถึง", "คือ"}
	for _, prefix := range prefixes {
		idx := strings.Index(cleanMeaning, prefix)
		if idx != -1 && idx < 30 {
			cleanMeaning = strings.TrimSpace(cleanMeaning[idx+len(prefix):])
			break
		}
	}

	cleanMeaning = strings.TrimPrefix(cleanMeaning, name+" ")
	cleanMeaning = strings.Join(strings.Fields(cleanMeaning), " ")

	return cleanMeaning
}

func normalizeSuggestionMeaning(meaning string) string {
	return strings.Join(strings.Fields(strings.TrimSpace(meaning)), " ")
}

func extractSuggestionTokens(texts ...string) []string {
	stopWords := map[string]bool{
		"แปลว่า":  true,
		"หมายถึง": true,
		"คือ":     true,
		"ผู้":     true,
		"ผู้ที่":  true,
		"ผู้มี":   true,
		"มี":      true,
		"อัน":     true,
		"ด้วย":    true,
		"แห่ง":    true,
		"ความ":    true,
		"การ":     true,
		"และ":     true,
		"กับ":     true,
		"ของ":     true,
		"ที่":     true,
		"ชื่อ":    true,
	}
	phraseBreakers := []string{
		"แปลว่า",
		"หมายถึง",
		"ผู้ที่",
		"ผู้มี",
		"อัน",
		"และ",
		"ด้วย",
		"แห่ง",
		"คือ",
		"ความ",
		"การ",
	}
	commonMeaningTerms := map[string]bool{
		"ชีวิต":    true,
		"งดงาม":    true,
		"เปี่ยม":   true,
		"พลัง":     true,
		"หวัง":     true,
		"ความหวัง": true,
	}

	tokenSet := make(map[string]bool)
	tokens := make([]string, 0, 6)
	replacer := strings.NewReplacer(",", " ", ".", " ", "(", " ", ")", " ")

	for _, text := range texts {
		normalized := replacer.Replace(text)
		for _, breaker := range phraseBreakers {
			normalized = strings.ReplaceAll(normalized, breaker, " ")
		}
		for _, token := range strings.Fields(normalized) {
			token = strings.TrimSpace(token)
			if token == "" || stopWords[token] {
				continue
			}
			token = strings.TrimPrefix(token, "มีความ")
			token = strings.TrimPrefix(token, "ความ")
			token = strings.TrimPrefix(token, "การ")
			if strings.HasPrefix(token, "มี") {
				withoutPrefix := strings.TrimPrefix(token, "มี")
				if commonMeaningTerms[withoutPrefix] {
					token = withoutPrefix
				}
			}
			runeLen := len([]rune(token))
			if runeLen < 2 || runeLen > 12 {
				continue
			}
			if tokenSet[token] {
				continue
			}
			tokenSet[token] = true
			tokens = append(tokens, token)
			if len(tokens) >= 6 {
				return tokens
			}
		}
	}

	return tokens
}

func countTokenCoverage(meaning string, tokens []string) int {
	if len(tokens) == 0 {
		return 0
	}

	coverage := 0
	for _, token := range tokens {
		if strings.Contains(meaning, token) {
			coverage++
		}
	}
	return coverage
}

// Helper to send JSON response (duplicated here to avoid cycle import)
func jsonResponse(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(data); err != nil {
		log.Printf("Error encoding response: %v", err)
	}
}

// SearchFeelingRequest defines the input for semantic search
type SearchFeelingRequest struct {
	Feeling string `json:"feeling"`
	Limit   int    `json:"limit"`
}

// SearchFeelingHandler handles searching names by semantic meaning or feeling
func SearchFeelingHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	feelingQuery := r.URL.Query().Get("q")
	if feelingQuery == "" {
		http.Error(w, "Query parameter 'q' (feeling) is required", http.StatusBadRequest)
		return
	}

	limitParam := r.URL.Query().Get("limit")
	limit := 30
	if l, err := strconv.Atoi(limitParam); err == nil && l > 0 {
		limit = l
	}

	goodOnly := r.URL.Query().Get("good_only") == "true"

	type NumQuality struct {
		IsGood bool `json:"is_good"`
		Level  int  `json:"level"` // 1-10
	}

	type SearchResult struct {
		ID       int        `json:"id"`
		Name     string     `json:"name"`
		Meaning  string     `json:"meaning"`
		Distance float64    `json:"distance"`
		Score    int        `json:"score"` // 100 - (distance * 100)
		SatSum   int        `json:"sat_sum"`
		ShaSum   int        `json:"sha_sum"`
		SatQual  NumQuality `json:"sat_qual"`
		ShaQual  NumQuality `json:"sha_qual"`
	}

	// 1. Get Embedding for the intensity/feeling
	embedding, err := services.GetEmbedding(feelingQuery)
	if err != nil {
		log.Printf("Error getting embedding: %v", err)
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Failed to generate embedding"})
		return
	}

	// 2. Fetch Meanings Map for Numerology Scoring
	meanings, _ := services.GetPairMeaningsMap()

	// 3. Multi-pass Sweep Logic
	results := []SearchResult{}
	offset := 0
	batchSize := 1000
	maxTotalCandidates := 20000 // Increased safety cap for more exhaustive search

	vectorStr := formatVector(embedding)

	for len(results) < limit && offset < maxTotalCandidates {
		fetchNow := batchSize
		if !goodOnly {
			fetchNow = limit // If not goodOnly, one tiny fetch is enough
		}

		query := `
			SELECT name_id, thname, COALESCE(meaning, ''), 
			       satnum, shanum,
			       (meaning_vector <=> $1) as distance
			FROM names_miracle
			WHERE meaning_vector IS NOT NULL
			ORDER BY distance ASC, name_id ASC
			LIMIT $2 OFFSET $3
		`

		rows, err := database.DB.Query(query, vectorStr, fetchNow, offset)
		if err != nil {
			log.Printf("Error querying vector at offset %d: %v", offset, err)
			break
		}

		foundInBatch := 0
		for rows.Next() {
			foundInBatch++
			var res SearchResult
			var satArr, shaArr pq.Int64Array
			if err := rows.Scan(&res.ID, &res.Name, &res.Meaning, &satArr, &shaArr, &res.Distance); err != nil {
				continue
			}

			// Calculate sums and quality
			satSum := 0
			for _, v := range satArr {
				satSum += int(v)
			}
			res.SatSum = satSum

			shaSum := 0
			for _, v := range shaArr {
				shaSum += int(v)
			}
			res.ShaSum = shaSum

			res.SatQual = getScoreQuality(res.SatSum, meanings)
			res.ShaQual = getScoreQuality(res.ShaSum, meanings)

			// Filtering
			if goodOnly {
				if !res.SatQual.IsGood || !res.ShaQual.IsGood {
					continue
				}
			}

			// Add result
			res.Score = int((1.0 - res.Distance) * 100)
			if res.Score < 0 {
				res.Score = 0
			}

			results = append(results, res)
			if len(results) >= limit {
				break
			}
		}
		rows.Close()

		if foundInBatch < fetchNow {
			// No more rows in DB
			break
		}

		if !goodOnly {
			// We already have what we need for standard search
			break
		}

		offset += batchSize
	}

	if results == nil {
		results = []SearchResult{}
	}

	jsonResponse(w, http.StatusOK, results)
}

// Helper to determine quality and level
func getScoreQuality(sum int, meanings map[string]string) struct {
	IsGood bool `json:"is_good"`
	Level  int  `json:"level"`
} {
	out := struct {
		IsGood bool `json:"is_good"`
		Level  int  `json:"level"`
	}{IsGood: true, Level: 5}

	pairs := services.CreatePairs(sum)
	if len(pairs) == 0 {
		return out
	}

	totalLevel := 0
	countBad := 0

	for _, p := range pairs {
		m, ok := meanings[p]
		if !ok {
			continue
		}

		// Extract Level from "Good (D10)" or "Bad (R5)"
		levelStr := ""
		for _, char := range m {
			if char >= '0' && char <= '9' {
				levelStr += string(char)
			}
		}
		lvl, _ := strconv.Atoi(levelStr)
		if lvl == 0 {
			lvl = 5
		}
		totalLevel += lvl

		if strings.HasPrefix(m, "Bad") {
			countBad++
		}
	}

	out.IsGood = countBad == 0 // All must be good
	out.Level = totalLevel / len(pairs)
	if out.Level > 10 {
		out.Level = 10
	}
	if out.Level == 0 {
		out.Level = 1
	}

	return out
}

// formatVector converts []float64 to PostgreSQL vector string format "[v1,v2,...]"
func formatVector(embedding []float64) string {
	var strs []string
	for _, val := range embedding {
		strs = append(strs, fmt.Sprintf("%f", val))
	}
	return "[" + strings.Join(strs, ",") + "]"
}

// GetNamesResponse defines the structure for list response with pagination
type GetNamesResponse struct {
	Data       []models.NameMiracle `json:"data"`
	NextCursor int                  `json:"next_cursor,omitempty"`
}

// GetNames handles listing names with optimized cursor-based pagination
// Query Params:
// - q: search term for name (optional)
// - limit: number of results (default 20, max 100)
// - cursor: the last name_id seen (default 0)
func GetNames(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	queryParam := r.URL.Query().Get("q")
	limitParam := r.URL.Query().Get("limit")
	cursorParam := r.URL.Query().Get("cursor")

	limit := 20
	if l, err := strconv.Atoi(limitParam); err == nil && l > 0 {
		if l > 100 {
			limit = 100 // Hard limit to prevent fetching too many rows
		} else {
			limit = l
		}
	}

	cursor := 0
	if c, err := strconv.Atoi(cursorParam); err == nil && c >= 0 {
		cursor = c
	}

	// Base query using keyset pagination (WHERE id > cursor)
	baseQuery := `
		SELECT name_id, COALESCE(thname, '')
		FROM names_miracle
		WHERE name_id > $1
	`

	args := []interface{}{cursor}
	argCounter := 2

	if queryParam != "" {
		// If searching, we also filter by name
		baseQuery += fmt.Sprintf(" AND thname LIKE $%d", argCounter)
		args = append(args, "%"+queryParam+"%")
		argCounter++
	}

	baseQuery += fmt.Sprintf(" ORDER BY name_id ASC LIMIT $%d", argCounter)
	args = append(args, limit)

	rows, err := database.DB.Query(baseQuery, args...)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	defer rows.Close()

	var names []models.NameMiracle
	var lastID int

	for rows.Next() {
		var n models.NameMiracle
		if err := rows.Scan(&n.NameID, &n.ThName); err != nil {
			log.Printf("Error scanning row: %v", err)
			continue
		}
		names = append(names, n)
		lastID = n.NameID
	}

	if names == nil {
		names = []models.NameMiracle{}
	}

	// Prepare response wrapper
	resp := GetNamesResponse{
		Data: names,
	}

	// If we got full limit results, suggest next cursor
	if len(names) == limit {
		resp.NextCursor = lastID
	}

	jsonResponse(w, http.StatusOK, resp)
}

// GetNameDetail retrieves full details for a specific name ID
func GetNameDetail(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	idStr := r.URL.Query().Get("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, "Invalid name ID", http.StatusBadRequest)
		return
	}

	query := `
		SELECT name_id, COALESCE(thname, ''), satnum, shanum, 
		       k_sunday, k_monday, k_tuesday, k_wednesday1, k_wednesday2, 
		       k_thursday, k_friday, k_saturday, 
		       t_sat, t_sha
		FROM names_miracle 
		WHERE name_id = $1
	`

	row := database.DB.QueryRow(query, id)

	var n models.NameMiracle
	err = row.Scan(
		&n.NameID, &n.ThName, &n.SatNum, &n.ShaNum,
		&n.KSunday, &n.KMonday, &n.KTuesday, &n.KWednesday1, &n.KWednesday2,
		&n.KThursday, &n.KFriday, &n.KSaturday,
		&n.TSat, &n.TSha,
	)

	if err == sql.ErrNoRows {
		http.Error(w, "Name not found", http.StatusNotFound)
		return
	} else if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	jsonResponse(w, http.StatusOK, n)
}

// GetSimilarNames handles vector similarity search using Ollama embeddings
// Query Params:
// - name: the input name to search for similar names (e.g. "สมปอง")
// - limit: number of results (default 10)
func GetSimilarNames(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	nameQuery := r.URL.Query().Get("name")
	if nameQuery == "" {
		http.Error(w, "Query parameter 'name' is required", http.StatusBadRequest)
		return
	}

	limitParam := r.URL.Query().Get("limit")
	limit := 10
	if l, err := strconv.Atoi(limitParam); err == nil && l > 0 {
		limit = l
	}

	// 1. Get Embedding from OpenAI (aligned with stored format: "ชื่อ: [ชื่อ] ความหมาย: [ความหมาย]")
	embedding, err := services.GetEmbedding("ชื่อ: " + nameQuery)
	if err != nil {
		log.Printf("Error getting embedding: %v", err)
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Failed to generate embedding for search"})
		return
	}

	// 2. Query Database using Cosine Distance (<=>)
	// Note: We cast the parameter to vector type. Ensure pgvector extension is installed.
	// We select the distance as well to show how similar it is (0 is identical, higher is less similar)
	query := `
		SELECT name_id, COALESCE(thname, ''), satnum, shanum, 
		       (meaning_vector <=> $1) as distance
		FROM names_miracle
		ORDER BY distance ASC
		LIMIT $2
	`

	vectorStr := formatVector(embedding)
	rows, err := database.DB.Query(query, vectorStr, limit)
	if err != nil {
		log.Printf("Error querying vector: %v", err)
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Database vector query failed: " + err.Error()})
		return
	}
	defer rows.Close()

	type SimilarNameResult struct {
		models.NameMiracle
		Distance float64 `json:"distance"`
	}

	var results []SimilarNameResult

	for rows.Next() {
		var res SimilarNameResult
		// Scan basic info + distance
		if err := rows.Scan(&res.NameID, &res.ThName, &res.SatNum, &res.ShaNum, &res.Distance); err != nil {
			log.Printf("Error scanning row: %v", err)
			continue
		}
		results = append(results, res)
	}

	if results == nil {
		results = []SimilarNameResult{}
	}

	jsonResponse(w, http.StatusOK, results)
}

// DecodeNameHandler handles Step 1: Decoding & Summation
func DecodeNameHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	name := r.URL.Query().Get("name")
	if name == "" {
		http.Error(w, "Query parameter 'name' is required", http.StatusBadRequest)
		return
	}

	day := r.URL.Query().Get("day")

	result, err := services.DecodeName(name, day)
	if err != nil {
		log.Printf("Error decoding name: %v", err)
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Failed to decode name"})
		return
	}

	jsonResponse(w, http.StatusOK, result)
}

// RecommendHandler handles Step 5: Advanced Search & Selection
func RecommendHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	intent := r.URL.Query().Get("intent")
	surname := r.URL.Query().Get("surname")
	day := r.URL.Query().Get("day")
	limitParam := r.URL.Query().Get("limit")

	limit := 10
	if l, err := strconv.Atoi(limitParam); err == nil && l > 0 {
		limit = l
	}

	results, err := services.RecommendNames(intent, surname, day, limit)
	if err != nil {
		log.Printf("Error recommending names: %v", err)
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Failed to recommend names"})
		return
	}

	jsonResponse(w, http.StatusOK, results)
}

// DebugEmbeddingHandler returns the raw vector for a given text
func DebugEmbeddingHandler(w http.ResponseWriter, r *http.Request) {
	text := r.URL.Query().Get("text")
	if text == "" {
		http.Error(w, "text required", http.StatusBadRequest)
		return
	}
	embedding, err := services.GetEmbedding(text)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	jsonResponse(w, http.StatusOK, map[string]interface{}{
		"text":       text,
		"vector_len": len(embedding),
		"vector":     embedding,
	})
}

// DebugDBHandler returns stats about the names_miracle table
func DebugDBHandler(w http.ResponseWriter, r *http.Request) {
	var total, nullVectors int
	err := database.DB.QueryRow("SELECT count(*), count(*) FILTER (WHERE meaning_vector IS NULL) FROM names_miracle").Scan(&total, &nullVectors)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	var sampleName string
	var sampleVectorLen int
	var sampleVectorStr string
	// Using vector_dims for pgvector instead of array_length
	err = database.DB.QueryRow("SELECT thname, vector_dims(meaning_vector), meaning_vector::text FROM names_miracle WHERE meaning_vector IS NOT NULL LIMIT 1").Scan(&sampleName, &sampleVectorLen, &sampleVectorStr)

	stats := map[string]interface{}{
		"total_rows":        total,
		"null_vectors":      nullVectors,
		"populated_percent": 0.0,
	}

	if total > 0 {
		stats["populated_percent"] = float64(total-nullVectors) / float64(total) * 100
	}

	if err == nil {
		stats["sample_name"] = sampleName
		stats["vector_dim"] = sampleVectorLen
		stats["raw_vector"] = sampleVectorStr
	} else {
		stats["sample_error"] = err.Error()
	}

	jsonResponse(w, http.StatusOK, stats)
}

// DebugLogHandler returns the last 100 lines of server.log
func DebugLogHandler(w http.ResponseWriter, r *http.Request) {
	file := "server.log"
	content, err := os.ReadFile(file)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "log not found: " + err.Error()})
		return
	}
	lines := strings.Split(string(content), "\n")
	limit := 100
	if len(lines) > limit {
		lines = lines[len(lines)-limit:]
	}
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	w.Write([]byte(strings.Join(lines, "\n")))
}

// DebugGenderLogHandler returns the last 100 lines of gender classification log
func DebugGenderLogHandler(w http.ResponseWriter, r *http.Request) {
	file := "go-naming/debug_tools/classify_gender_ai.log"
	content, err := os.ReadFile(file)
	if err != nil {
		// Try absolute path on server
		content, err = os.ReadFile("/home/tayap/go-naming/debug_tools/classify_gender_ai.log")
	}
	if err != nil {
		// Try local path for debugging if needed
		content, err = os.ReadFile("/Users/tayap/project-naming/go-naming/debug_tools/classify_gender_ai.log")
	}
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "gender log not found: " + err.Error()})
		return
	}
	lines := strings.Split(string(content), "\n")
	limit := 100
	if len(lines) < limit {
		limit = len(lines)
	}
	output := lines[len(lines)-limit:]
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	w.Write([]byte(strings.Join(output, "\n")))
}

// DebugGenderStatsHandler returns statistics about gender classification
func DebugGenderStatsHandler(w http.ResponseWriter, r *http.Request) {
	var total, male, female, neutral, remains int

	// Query summary stats
	err := database.DB.QueryRow(`
		SELECT 
			COUNT(*),
			COUNT(*) FILTER (WHERE gender = 'male'),
			COUNT(*) FILTER (WHERE gender = 'female'),
			COUNT(*) FILTER (WHERE gender = 'neutral'),
			COUNT(*) FILTER (WHERE gender IS NULL)
		FROM names_miracle
	`).Scan(&total, &male, &female, &neutral, &remains)

	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	// ในที่นี้เราถือว่าชื่อที่เป็น male หรือ female คือชื่อที่จำแนกแล้วชัดเจน
	processed := male + female

	jsonResponse(w, http.StatusOK, map[string]interface{}{
		"total_rows":           total,
		"classified_male":      male,
		"classified_female":    female,
		"neutral_or_pending":   neutral + remains,
		"clear_gender_count":   processed,
		"percent_clear_gender": float64(processed) / float64(total) * 100,
		"status":               "Running on Mac M1 - Watch the counts increase!",
	})
}
