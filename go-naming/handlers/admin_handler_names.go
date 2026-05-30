package handlers

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"log"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/lib/pq"
)

const adminNamesLimit = 200

type adminNameInput struct {
	Name    string `json:"name"`
	ThName  string `json:"th_name"`
	Thname  string `json:"thname"`
	Meaning string `json:"meaning"`
	Gender  string `json:"gender"`
}

type adminNameRecord struct {
	NameID            int      `json:"name_id"`
	ThName            string   `json:"th_name"`
	Meaning           string   `json:"meaning"`
	Gender            string   `json:"gender"`
	SatSum            int      `json:"sat_sum"`
	ShaSum            int      `json:"sha_sum"`
	TSat              []string `json:"t_sat"`
	TSha              []string `json:"t_sha"`
	PhoneticScore     *int     `json:"phonetic_score,omitempty"`
	PhoneticSummary   string   `json:"phonetic_summary"`
	PhoneticProcessed bool     `json:"phonetic_processed"`
}

type adminNamePairPreview struct {
	Pair      string `json:"pair"`
	PairType  string `json:"pair_type"`
	PairPoint int    `json:"pair_point"`
	IsGood    bool   `json:"is_good"`
}

type adminNameAnalysisPreview struct {
	Name   string                 `json:"name"`
	SatSum int                    `json:"sat_sum"`
	ShaSum int                    `json:"sha_sum"`
	Sat    []adminNamePairPreview `json:"sat"`
	Sha    []adminNamePairPreview `json:"sha"`
}

// AnalyzeNameHandler previews numerology and shadow pairs before an admin saves a name.
func AnalyzeNameHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var input adminNameInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid JSON"})
		return
	}

	name := normalizeAdminThaiName(firstNonEmpty(input.Name, input.ThName, input.Thname))
	if name == "" {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "กรุณาระบุชื่อก่อน"})
		return
	}

	decoded, err := services.DecodeName(name, "")
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	satPairs, err := loadAdminPairPreview(decoded.SatPairs)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	shaPairs, err := loadAdminPairPreview(decoded.ShaPairs)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	jsonResponse(w, http.StatusOK, adminNameAnalysisPreview{
		Name:   name,
		SatSum: decoded.TotalSat,
		ShaSum: decoded.TotalSha,
		Sat:    satPairs,
		Sha:    shaPairs,
	})
}

// GenerateNameMeaningHandler uses Typhoon to draft a Thai meaning for an admin-entered name.
func GenerateNameMeaningHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var input adminNameInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid JSON"})
		return
	}

	name := normalizeAdminThaiName(firstNonEmpty(input.Name, input.ThName, input.Thname))
	if name == "" {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "กรุณาระบุชื่อก่อน"})
		return
	}

	meaning, err := generateAdminNameMeaning(r.Context(), name)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	jsonResponse(w, http.StatusOK, map[string]string{
		"status":  "success",
		"meaning": meaning,
	})
}

// AddNameHandler adds a complete names_miracle row from the admin panel.
func AddNameHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var input adminNameInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid JSON"})
		return
	}

	name := normalizeAdminThaiName(firstNonEmpty(input.Name, input.ThName, input.Thname))
	meaning := strings.TrimSpace(input.Meaning)
	gender := normalizeAdminNameGender(input.Gender)

	if name == "" {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "กรุณาระบุชื่อ"})
		return
	}

	if meaning == "" {
		generatedMeaning, err := generateAdminNameMeaning(r.Context(), name)
		if err != nil {
			jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "สร้างความหมายไม่สำเร็จ: " + err.Error()})
			return
		}
		meaning = generatedMeaning
	}

	var existingID int
	err := database.DB.QueryRow("SELECT name_id FROM names_miracle WHERE thname = $1 LIMIT 1", name).Scan(&existingID)
	if err == nil {
		jsonResponse(w, http.StatusConflict, map[string]interface{}{
			"error":   "ชื่อนี้มีอยู่ในระบบแล้ว",
			"name_id": existingID,
		})
		return
	}
	if err != sql.ErrNoRows {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	decoded, err := services.DecodeName(name, "")
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	tSat, err := loadAdminPairTypes(decoded.SatPairs)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	tSha, err := loadAdminPairTypes(decoded.ShaPairs)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	kakiFlags, err := buildAdminKakiFlags(name)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	embeddingText := name
	if meaning != "" {
		embeddingText = name + ": " + meaning
	}
	embedding, err := services.GetEmbedding(embeddingText)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "สร้าง embedding ไม่สำเร็จ: " + err.Error()})
		return
	}
	if len(embedding) != 1536 {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": fmt.Sprintf("embedding dimension ไม่ถูกต้อง: %d", len(embedding))})
		return
	}

	vectorLiteral := formatAdminVectorSQL(embedding)

	var phoneticScore interface{}
	var pronunciationEase interface{}
	var euphonyScore interface{}
	var rhythmScore interface{}
	var phoneticSummary interface{}
	var phoneticLabels interface{}
	var phoneticIssues interface{}
	var phoneticStyleTone interface{}
	var phoneticRaw interface{}
	var phoneticError interface{}
	var phoneticVersion interface{}
	var phoneticUpdatedAt interface{}
	phoneticProcessed := false
	phoneticRetryCount := 0

	if strings.TrimSpace(os.Getenv("TYPHOON_API_KEY")) != "" {
		client, err := services.NewTyphoonPhoneticClientFromEnv(45 * time.Second)
		if err != nil {
			phoneticError = err.Error()
			phoneticRetryCount = 1
			phoneticUpdatedAt = time.Now().UTC()
		} else {
			ctx, cancel := context.WithTimeout(r.Context(), 45*time.Second)
			evaluation, rawResponse, err := client.EvaluateThaiName(ctx, name)
			cancel()

			if err != nil {
				log.Printf("Admin AddName phonetic skipped for %s: %v", name, err)
				phoneticError = err.Error()
				phoneticRetryCount = 1
				phoneticUpdatedAt = time.Now().UTC()
				if len(rawResponse) > 0 {
					rawJSON, marshalErr := json.Marshal(map[string]interface{}{
						"source":            "admin_names_add",
						"version":           client.Version,
						"provider_response": json.RawMessage(rawResponse),
						"error":             err.Error(),
						"saved_at":          time.Now().UTC(),
					})
					if marshalErr == nil {
						phoneticRaw = string(rawJSON)
					}
				}
			} else {
				labelsJSON, _ := json.Marshal(evaluation.Labels)
				issuesJSON, _ := json.Marshal(evaluation.Issues)
				styleJSON, _ := json.Marshal(evaluation.StyleTone)
				rawJSON, marshalErr := json.Marshal(map[string]interface{}{
					"source":            "admin_names_add",
					"version":           client.Version,
					"provider_response": json.RawMessage(rawResponse),
					"parsed":            evaluation,
					"saved_at":          time.Now().UTC(),
				})

				phoneticScore = evaluation.Score.Overall
				pronunciationEase = evaluation.Score.PronunciationEase
				euphonyScore = evaluation.Score.Euphony
				rhythmScore = evaluation.Score.Rhythm
				phoneticSummary = evaluation.Summary
				phoneticLabels = string(labelsJSON)
				phoneticIssues = string(issuesJSON)
				phoneticStyleTone = string(styleJSON)
				if marshalErr == nil {
					phoneticRaw = string(rawJSON)
				}
				phoneticProcessed = true
				phoneticVersion = client.Version
				phoneticUpdatedAt = time.Now().UTC()
			}
		}
	}

	query := fmt.Sprintf(`
		INSERT INTO names_miracle (
			thname, satnum, shanum,
			k_sunday, k_monday, k_tuesday, k_wednesday1, k_wednesday2,
			k_thursday, k_friday, k_saturday,
			t_sat, t_sha, meaning, meaning_vector, gender, sat_sum, sha_sum,
			phonetic_score, pronunciation_ease, euphony_score, rhythm_score,
			phonetic_summary, phonetic_labels, phonetic_issues, phonetic_style_tone,
			phonetic_raw, phonetic_processed, phonetic_error, phonetic_retry_count,
			phonetic_version, phonetic_updated_at
		)
		VALUES (
			$1, $2, $3,
			$4, $5, $6, $7, $8,
			$9, $10, $11,
			$12, $13, $14, %s::vector, $15, $16, $17,
			$18, $19, $20, $21,
			$22, $23::jsonb, $24::jsonb, $25::jsonb,
			$26::jsonb, $27, $28, $29,
			$30, $31
		)
		RETURNING name_id
	`, vectorLiteral)

	var insertedID int
	err = database.DB.QueryRow(
		query,
		name,
		pq.Array(decoded.SatPairs),
		pq.Array(decoded.ShaPairs),
		kakiFlags["k_sunday"],
		kakiFlags["k_monday"],
		kakiFlags["k_tuesday"],
		kakiFlags["k_wednesday1"],
		kakiFlags["k_wednesday2"],
		kakiFlags["k_thursday"],
		kakiFlags["k_friday"],
		kakiFlags["k_saturday"],
		pq.Array(tSat),
		pq.Array(tSha),
		meaning,
		gender,
		decoded.TotalSat,
		decoded.TotalSha,
		phoneticScore,
		pronunciationEase,
		euphonyScore,
		rhythmScore,
		phoneticSummary,
		phoneticLabels,
		phoneticIssues,
		phoneticStyleTone,
		phoneticRaw,
		phoneticProcessed,
		phoneticError,
		phoneticRetryCount,
		phoneticVersion,
		phoneticUpdatedAt,
	).Scan(&insertedID)

	if err != nil {
		if pqErr, ok := err.(*pq.Error); ok && pqErr.Code == "23505" {
			jsonResponse(w, http.StatusConflict, map[string]string{"error": "ชื่อนี้มีอยู่ในระบบแล้ว"})
			return
		}
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	jsonResponse(w, http.StatusOK, map[string]interface{}{
		"status":               "success",
		"id":                   insertedID,
		"phonetic_processed":   phoneticProcessed,
		"phonetic_retry_count": phoneticRetryCount,
	})
}

// ListNamesHandler lists the latest names for the admin panel.
func ListNamesHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	rows, err := database.DB.Query(`
		SELECT name_id, COALESCE(thname, ''), COALESCE(meaning, ''), COALESCE(gender, ''),
		       COALESCE(sat_sum, 0), COALESCE(sha_sum, 0),
		       COALESCE(t_sat, ARRAY[]::text[]), COALESCE(t_sha, ARRAY[]::text[]),
		       phonetic_score, COALESCE(phonetic_summary, ''), COALESCE(phonetic_processed, false)
		FROM names_miracle
		ORDER BY name_id DESC
		LIMIT $1
	`, adminNamesLimit)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	defer rows.Close()

	records, err := scanAdminNameRows(rows)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	jsonResponse(w, http.StatusOK, records)
}

// SearchNamesHandler searches names by Thai name or meaning.
func SearchNamesHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	q := strings.TrimSpace(r.URL.Query().Get("q"))
	if q == "" {
		ListNamesHandler(w, r)
		return
	}

	pattern := "%" + q + "%"
	prefix := q + "%"
	rows, err := database.DB.Query(`
		SELECT name_id, COALESCE(thname, ''), COALESCE(meaning, ''), COALESCE(gender, ''),
		       COALESCE(sat_sum, 0), COALESCE(sha_sum, 0),
		       COALESCE(t_sat, ARRAY[]::text[]), COALESCE(t_sha, ARRAY[]::text[]),
		       phonetic_score, COALESCE(phonetic_summary, ''), COALESCE(phonetic_processed, false)
		FROM names_miracle
		WHERE thname ILIKE $1 OR meaning ILIKE $1
		ORDER BY
			CASE
				WHEN thname = $2 THEN 0
				WHEN thname ILIKE $3 THEN 1
				ELSE 2
			END,
			name_id DESC
		LIMIT $4
	`, pattern, q, prefix, adminNamesLimit)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	defer rows.Close()

	records, err := scanAdminNameRows(rows)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	jsonResponse(w, http.StatusOK, records)
}

// DeleteNameHandler deletes a names_miracle row by name_id.
func DeleteNameHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost && r.Method != http.MethodDelete {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	id := 0
	if rawID := strings.TrimSpace(r.URL.Query().Get("id")); rawID != "" {
		parsed, err := strconv.Atoi(rawID)
		if err != nil {
			jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid name id"})
			return
		}
		id = parsed
	}

	if id == 0 {
		var body struct {
			ID     int `json:"id"`
			NameID int `json:"name_id"`
		}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid JSON"})
			return
		}
		id = body.ID
		if id == 0 {
			id = body.NameID
		}
	}

	if id <= 0 {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid name id"})
		return
	}

	result, err := database.DB.Exec("DELETE FROM names_miracle WHERE name_id = $1", id)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	affected, _ := result.RowsAffected()
	if affected == 0 {
		jsonResponse(w, http.StatusNotFound, map[string]string{"error": "Name not found"})
		return
	}

	jsonResponse(w, http.StatusOK, map[string]string{"status": "success"})
}

func scanAdminNameRows(rows *sql.Rows) ([]adminNameRecord, error) {
	records := make([]adminNameRecord, 0)
	for rows.Next() {
		var rec adminNameRecord
		var tSat pq.StringArray
		var tSha pq.StringArray
		var score sql.NullInt64

		if err := rows.Scan(
			&rec.NameID,
			&rec.ThName,
			&rec.Meaning,
			&rec.Gender,
			&rec.SatSum,
			&rec.ShaSum,
			&tSat,
			&tSha,
			&score,
			&rec.PhoneticSummary,
			&rec.PhoneticProcessed,
		); err != nil {
			return nil, err
		}

		rec.TSat = []string(tSat)
		rec.TSha = []string(tSha)
		if score.Valid {
			value := int(score.Int64)
			rec.PhoneticScore = &value
		}
		records = append(records, rec)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	return records, nil
}

func loadAdminPairTypes(pairs []string) ([]string, error) {
	out := make([]string, len(pairs))
	if len(pairs) == 0 {
		return out, nil
	}

	seen := make(map[string]struct{}, len(pairs))
	uniquePairs := make([]string, 0, len(pairs))
	for _, pair := range pairs {
		pair = strings.TrimSpace(pair)
		if pair == "" {
			continue
		}
		if _, ok := seen[pair]; ok {
			continue
		}
		seen[pair] = struct{}{}
		uniquePairs = append(uniquePairs, pair)
	}
	if len(uniquePairs) == 0 {
		return out, nil
	}

	rows, err := database.DB.Query(`
		SELECT TRIM(pairnumber), TRIM(pairtype)
		FROM numbers
		WHERE TRIM(pairnumber) = ANY($1)
	`, pq.Array(uniquePairs))
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	typeByPair := make(map[string]string, len(uniquePairs))
	for rows.Next() {
		var pair string
		var pairType string
		if err := rows.Scan(&pair, &pairType); err != nil {
			return nil, err
		}
		typeByPair[strings.TrimSpace(pair)] = strings.TrimSpace(pairType)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}

	for i, pair := range pairs {
		if pairType, ok := typeByPair[strings.TrimSpace(pair)]; ok && pairType != "" {
			out[i] = pairType
		} else {
			out[i] = "Unknown"
		}
	}
	return out, nil
}

func loadAdminPairPreview(pairs []string) ([]adminNamePairPreview, error) {
	out := make([]adminNamePairPreview, len(pairs))
	if len(pairs) == 0 {
		return out, nil
	}

	seen := make(map[string]struct{}, len(pairs))
	uniquePairs := make([]string, 0, len(pairs))
	for _, pair := range pairs {
		pair = strings.TrimSpace(pair)
		if pair == "" {
			continue
		}
		if _, ok := seen[pair]; ok {
			continue
		}
		seen[pair] = struct{}{}
		uniquePairs = append(uniquePairs, pair)
	}
	if len(uniquePairs) == 0 {
		return out, nil
	}

	rows, err := database.DB.Query(`
		SELECT TRIM(pairnumber), TRIM(pairtype), COALESCE(pairpoint, 0)
		FROM numbers
		WHERE TRIM(pairnumber) = ANY($1)
	`, pq.Array(uniquePairs))
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	previewByPair := make(map[string]adminNamePairPreview, len(uniquePairs))
	for rows.Next() {
		var preview adminNamePairPreview
		if err := rows.Scan(&preview.Pair, &preview.PairType, &preview.PairPoint); err != nil {
			return nil, err
		}
		preview.Pair = strings.TrimSpace(preview.Pair)
		preview.PairType = strings.TrimSpace(preview.PairType)
		preview.IsGood = strings.HasPrefix(preview.PairType, "D")
		previewByPair[preview.Pair] = preview
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}

	for i, pair := range pairs {
		cleanPair := strings.TrimSpace(pair)
		if preview, ok := previewByPair[cleanPair]; ok {
			out[i] = preview
			continue
		}
		out[i] = adminNamePairPreview{
			Pair:     cleanPair,
			PairType: "Unknown",
			IsGood:   false,
		}
	}
	return out, nil
}

func buildAdminKakiFlags(name string) (map[string]bool, error) {
	flags := map[string]bool{
		"k_sunday":     false,
		"k_monday":     false,
		"k_tuesday":    false,
		"k_wednesday1": false,
		"k_wednesday2": false,
		"k_thursday":   false,
		"k_friday":     false,
		"k_saturday":   false,
	}

	rows, err := database.DB.Query("SELECT day, COALESCE(day_th, ''), kakis FROM kakis_day")
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	nameRunes := map[rune]bool{}
	for _, r := range name {
		nameRunes[r] = true
	}

	for rows.Next() {
		var day string
		var dayThai string
		var kakis string
		if err := rows.Scan(&day, &dayThai, &kakis); err != nil {
			return nil, err
		}

		column := adminKakiColumn(day, dayThai)
		if column == "" {
			continue
		}

		for _, r := range strings.TrimSpace(kakis) {
			if nameRunes[r] {
				flags[column] = true
				break
			}
		}
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}

	return flags, nil
}

func adminKakiColumn(day string, dayThai string) string {
	switch strings.ToLower(strings.TrimSpace(day)) {
	case "sunday":
		return "k_sunday"
	case "monday":
		return "k_monday"
	case "tuesday":
		return "k_tuesday"
	case "wednesday1", "wednesday":
		return "k_wednesday1"
	case "wednesday2":
		return "k_wednesday2"
	case "thursday":
		return "k_thursday"
	case "friday":
		return "k_friday"
	case "saturday":
		return "k_saturday"
	}

	switch strings.TrimSpace(dayThai) {
	case "อาทิตย์":
		return "k_sunday"
	case "จันทร์":
		return "k_monday"
	case "อังคาร":
		return "k_tuesday"
	case "พุธกลางวัน", "พุธ":
		return "k_wednesday1"
	case "พุธกลางคืน", "ราหู":
		return "k_wednesday2"
	case "พฤหัสบดี":
		return "k_thursday"
	case "ศุกร์":
		return "k_friday"
	case "เสาร์":
		return "k_saturday"
	default:
		return ""
	}
}

func normalizeAdminNameGender(gender string) string {
	normalized := strings.TrimSpace(strings.ToLower(gender))
	switch normalized {
	case "ช", "ชาย", "male", "m":
		return "ช"
	case "ญ", "หญิง", "female", "f":
		return "ญ"
	case "ค", "คละ", "คละเพศ", "ทุกเพศ", "neutral", "unisex", "all":
		return "ค"
	default:
		return "ค"
	}
}

func normalizeAdminThaiName(name string) string {
	return strings.Join(strings.Fields(strings.TrimSpace(name)), "")
}

func generateAdminNameMeaning(parent context.Context, name string) (string, error) {
	if strings.TrimSpace(os.Getenv("TYPHOON_API_KEY")) == "" {
		return "", fmt.Errorf("TYPHOON_API_KEY is not set")
	}

	client, err := services.NewTyphoonPhoneticClientFromEnv(20 * time.Second)
	if err != nil {
		return "", err
	}

	ctx, cancel := context.WithTimeout(parent, 20*time.Second)
	defer cancel()

	meaning, _, err := client.GenerateThaiNameMeaning(ctx, name)
	if err != nil {
		return "", err
	}
	return meaning, nil
}

func firstNonEmpty(values ...string) string {
	for _, value := range values {
		if strings.TrimSpace(value) != "" {
			return value
		}
	}
	return ""
}

func formatAdminVectorSQL(vector []float64) string {
	vectorStrings := make([]string, len(vector))
	for i, v := range vector {
		vectorStrings[i] = strconv.FormatFloat(v, 'f', -1, 64)
	}
	return fmt.Sprintf("'[%s]'", strings.Join(vectorStrings, ","))
}
