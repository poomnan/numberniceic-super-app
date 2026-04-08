package handlers

import (
	"encoding/json"
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"log"
	"net/http"
	"sort"
	"strings"

	"github.com/lib/pq"
)

// --- Mobile Name Search API ---

// MobileSearchRequest defines the input for mobile app name search
type MobileSearchRequest struct {
	Keyword         string `json:"keyword"`
	Lastname        string `json:"lastname"`
	Day             string `json:"day"`
	Gender          string `json:"gender"`
	SemanticMeaning string `json:"semantic_meaning"`
	MeaningIntent   string `json:"meaning_intent"`
	EntryMode       string `json:"entry_mode"`
	FilterSat       bool   `json:"filter_sat"`
	FilterSha       bool   `json:"filter_sha"`
	FilterKaki      bool   `json:"filter_kaki"`
	SimilarMode     bool   `json:"similar_mode"`
	Limit           int    `json:"limit"`
}

// CharHighlight represents a single character with its kaki status
type CharHighlight struct {
	Char   string `json:"char"`
	IsKaki bool   `json:"is_kaki"`
}

// MobileNameResult represents a single name result for mobile
type MobileNameResult struct {
	Name              string          `json:"name"`
	Meaning           string          `json:"meaning"`
	Gender            string          `json:"gender"`
	SatSum            int             `json:"sat_sum"`
	ShaSum            int             `json:"sha_sum"`
	TotalSat          int             `json:"total_sat"`
	TotalSha          int             `json:"total_sha"`
	Distance          float64         `json:"distance"`
	RootScore         float64         `json:"root_score"`
	SemanticScore     float64         `json:"semantic_score"`
	HybridScore       float64         `json:"hybrid_score"`
	BonusCalculated   float64         `json:"bonus_calculated"`
	IsSatGood         bool            `json:"is_sat_good"`
	IsShaGood         bool            `json:"is_sha_good"`
	IsTotalSatGood    bool            `json:"is_total_sat_good"`
	IsTotalShaGood    bool            `json:"is_total_sha_good"`
	SatPairType       string          `json:"sat_pair_type"`
	ShaPairType       string          `json:"sha_pair_type"`
	TotalSatPairType  string          `json:"total_sat_pair_type"`
	TotalShaPairType  string          `json:"total_sha_pair_type"`
	SatPairPoint      int             `json:"sat_pair_point"`
	ShaPairPoint      int             `json:"sha_pair_point"`
	TotalSatPairPoint int             `json:"total_sat_pair_point"`
	TotalShaPairPoint int             `json:"total_sha_pair_point"`
	KakiHighlight     []CharHighlight `json:"kaki_highlight"`
	FinalRankScore    int             `json:"final_rank_score"`
	SemanticRankScore int             `json:"semantic_rank_score"`
	NumerologyScore   int             `json:"numerology_rank_score"`
	PairTypeBonus     int             `json:"pair_type_bonus"`
	PairPointBonus    int             `json:"pair_point_bonus"`
	LengthBonus       int             `json:"length_bonus"`
	KakiBonus         int             `json:"kaki_bonus"`
	SatBonus          int             `json:"sat_bonus"`
	ShaBonus          int             `json:"sha_bonus"`
	DoubleBonus       int             `json:"double_bonus"`
	RankReasons       []string        `json:"rank_reasons"`
}

// ... unchanged ...
// KakiInfo provides metadata about kaki characters for the selected day
type KakiInfo struct {
	Day         string   `json:"day"`
	DayThai     string   `json:"day_th"`
	Description string   `json:"description"`
	KakiChars   []string `json:"kaki_chars"`
}

// MobileSearchResponse is the full response for mobile
type MobileSearchResponse struct {
	Success       bool               `json:"success"`
	Error         any                `json:"error,omitempty"`
	Results       []MobileNameResult `json:"results"`
	KakiInfo      *KakiInfo          `json:"kaki_info,omitempty"`
	RetrievalMeta *RetrievalMeta     `json:"retrieval_meta,omitempty"`
	Total         int                `json:"total"`
}

type RetrievalMeta struct {
	EntryMode         string  `json:"entry_mode"`
	RetrievalStrategy string  `json:"retrieval_strategy"`
	MeaningContext    string  `json:"meaning_context"`
	SemanticWeight    float64 `json:"semantic_weight"`
	WordWeight        float64 `json:"word_weight"`
	CandidateLimit    int     `json:"candidate_limit"`
}

func setMobileCORSHeaders(w http.ResponseWriter) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
}

// calculateBonus calculates the bonus score based on frontend ranking logic
func calculateBonus(name string, isSatGood, isShaGood, isTotalSatGood, isTotalShaGood bool, kakiHighlight []CharHighlight) float64 {
	bonus := 0.0

	// SAT good bonus
	if isSatGood {
		bonus += 20.0
	}

	// SHA good bonus
	if isShaGood {
		bonus += 20.0
	}

	// Double Lucky bonus (both SAT and SHA good)
	if isSatGood && isShaGood {
		bonus += 80.0
	}

	// Kaki free bonus (no kaki characters)
	hasKaki := false
	for _, h := range kakiHighlight {
		if h.IsKaki {
			hasKaki = true
			break
		}
	}
	if !hasKaki {
		bonus += 10.0
	}

	// Length bonus
	nameLen := len([]rune(name))
	if nameLen == 2 {
		bonus += 40.0
	} else if nameLen == 3 {
		bonus += 30.0
	} else if nameLen == 4 {
		bonus += 20.0
	} else if nameLen == 5 {
		bonus += 10.0
	} else if nameLen >= 6 {
		bonus -= 10.0
	}

	// Short Name Quality Bonus (for short names with good quality)
	if nameLen <= 3 && (isSatGood || isShaGood) {
		bonus += 30.0
	}

	return bonus
}

func calculateFinalRankScoreAndReasons(r *MobileNameResult, showMatching bool) (int, []string) {
	similarity := (100 * (1 - r.Distance))
	if similarity < 0 {
		similarity = 0
	}
	if similarity > 100 {
		similarity = 100
	}

	satPass := r.IsSatGood
	shaPass := r.IsShaGood
	satPairType := r.SatPairType
	shaPairType := r.ShaPairType
	satPairPoint := r.SatPairPoint
	shaPairPoint := r.ShaPairPoint
	if showMatching {
		satPass = r.IsTotalSatGood
		shaPass = r.IsTotalShaGood
		satPairType = r.TotalSatPairType
		shaPairType = r.TotalShaPairType
		satPairPoint = r.TotalSatPairPoint
		shaPairPoint = r.TotalShaPairPoint
	}

	satBonus := 0
	if satPass {
		satBonus = 20
	}
	shaBonus := 0
	if shaPass {
		shaBonus = 20
	}
	doubleBonus := 0
	if satPass && shaPass {
		doubleBonus = 50
	}
	pairTypeBonus := pairTypeTierBonus(satPairType) + pairTypeTierBonus(shaPairType)
	pairPointBonus := pairPointRankBonus(satPairPoint) + pairPointRankBonus(shaPairPoint)

	kakiBonus := 0
	if len(r.KakiHighlight) > 0 {
		hasKaki := false
		for _, h := range r.KakiHighlight {
			if h.IsKaki {
				hasKaki = true
				break
			}
		}
		if !hasKaki {
			kakiBonus = 10
		}
	}

	lengthBonus := 0
	nameLen := len([]rune(r.Name))
	if nameLen <= 4 {
		lengthBonus = 15
	} else if nameLen == 5 {
		lengthBonus = 10
	} else if nameLen == 6 {
		lengthBonus = 5
	} else if nameLen >= 9 {
		lengthBonus = -5
	}

	rawScore := similarity + float64(satBonus+shaBonus+doubleBonus+kakiBonus+lengthBonus+pairTypeBonus+pairPointBonus)
	finalScore := int((rawScore * 100.0) / 283.0)
	if finalScore < 0 {
		finalScore = 0
	}
	if finalScore > 100 {
		finalScore = 100
	}

	r.SemanticRankScore = int(similarity + 0.5)
	r.NumerologyScore = satBonus + shaBonus + doubleBonus + kakiBonus + lengthBonus + pairTypeBonus + pairPointBonus
	r.PairTypeBonus = pairTypeBonus
	r.PairPointBonus = pairPointBonus
	r.LengthBonus = lengthBonus
	r.KakiBonus = kakiBonus
	r.SatBonus = satBonus
	r.ShaBonus = shaBonus
	r.DoubleBonus = doubleBonus

	reasons := []string{
		fmt.Sprintf("ความใกล้เคียงความหมาย %.0f/100", similarity),
	}
	if satBonus > 0 {
		reasons = append(reasons, fmt.Sprintf("เลขศาสตร์ผ่าน +%d", satBonus))
	}
	if shaBonus > 0 {
		reasons = append(reasons, fmt.Sprintf("พลังเงาผ่าน +%d", shaBonus))
	}
	if doubleBonus > 0 {
		reasons = append(reasons, fmt.Sprintf("ผ่านทั้งเลขศาสตร์และพลังเงา +%d", doubleBonus))
	}
	if pairTypeBonus > 0 {
		reasons = append(reasons, fmt.Sprintf("ระดับคู่เลข/คู่เงา %+d", pairTypeBonus))
	}
	if pairPointBonus != 0 {
		reasons = append(reasons, fmt.Sprintf("คะแนนละเอียด pairpoint %+d", pairPointBonus))
	}
	if kakiBonus > 0 {
		reasons = append(reasons, fmt.Sprintf("ปลอดกาลกิณี +%d", kakiBonus))
	}
	if lengthBonus != 0 {
		reasons = append(reasons, fmt.Sprintf("คะแนนความยาวชื่อ %+d", lengthBonus))
	}

	return finalScore, reasons
}

func normalizedMeaningKey(meaning string) string {
	meaning = strings.TrimSpace(strings.ToLower(meaning))
	replacer := strings.NewReplacer(
		"\n", " ",
		"\r", " ",
		"\t", " ",
		"  ", " ",
	)
	for {
		next := replacer.Replace(meaning)
		if next == meaning {
			break
		}
		meaning = next
	}
	return meaning
}

func pairTypeTierBonus(pairType string) int {
	switch strings.ToUpper(strings.TrimSpace(pairType)) {
	case "D10":
		return 14
	case "D8":
		return 8
	case "D5":
		return 3
	default:
		return 0
	}
}

func pairPointRankBonus(pairPoint int) int {
	switch {
	case pairPoint >= 80:
		return 20
	case pairPoint >= 65:
		return 14
	case pairPoint >= 50:
		return 9
	case pairPoint >= 30:
		return 5
	case pairPoint >= 10:
		return 2
	case pairPoint <= -20:
		return -8
	case pairPoint < 0:
		return -4
	default:
		return 0
	}
}

// MobileSearchHandler handles POST /api/v1/name-search
func MobileSearchHandler(w http.ResponseWriter, r *http.Request) {
	setMobileCORSHeaders(w)
	defer func() {
		if rec := recover(); rec != nil {
			log.Printf("MobileSearchHandler panic recovered: %v", rec)
			jsonResponse(w, http.StatusInternalServerError, MobileSearchResponse{
				Success: false,
				Error:   map[string]any{"message": "เกิดข้อผิดพลาดชั่วคราว กรุณาลองใหม่อีกครั้ง"},
				Results: []MobileNameResult{},
				Total:   0,
			})
		}
	}()
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusNoContent)
		return
	}
	if r.Method != http.MethodPost {
		jsonResponse(w, http.StatusMethodNotAllowed, MobileSearchResponse{Success: false, Error: map[string]any{"message": "Method not allowed"}, Results: []MobileNameResult{}, Total: 0})
		return
	}
	log.Printf("MobileSearchHandler: request received")

	var req MobileSearchRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		jsonResponse(w, http.StatusBadRequest, MobileSearchResponse{Success: false, Error: map[string]any{"message": "Invalid JSON body"}, Results: []MobileNameResult{}, Total: 0})
		return
	}

	log.Printf("Search param: Keyword='%s', Meaning='%s'", req.Keyword, req.SemanticMeaning)

	var meaningDB string
	if req.SemanticMeaning == "" && req.Keyword != "" {
		// Fallback: If frontend didn't send semantic meaning, fetch it from DB using the keyword
		err := database.DB.QueryRow("SELECT meaning FROM names_miracle WHERE thname = $1 LIMIT 1", req.Keyword).Scan(&meaningDB)
		if err == nil && meaningDB != "" {
			req.SemanticMeaning = meaningDB
			log.Printf("Auto-fetched Semantic Meaning: '%s'", req.SemanticMeaning)
		}
	}

	searchContext := services.BuildMeaningSearchContext(
		services.MeaningSearchPipelineInput{
			Keyword:         req.Keyword,
			Lastname:        req.Lastname,
			SemanticMeaning: req.SemanticMeaning,
			MeaningIntent:   req.MeaningIntent,
			EntryMode:       req.EntryMode,
			SimilarMode:     req.SimilarMode,
			Limit:           req.Limit,
		},
		meaningDB,
	)

	if searchContext.WordContext == "" &&
		searchContext.MeaningContext == "" &&
		!searchContext.IsBroadSearch {
		jsonResponse(w, http.StatusOK, MobileSearchResponse{Success: true, Results: []MobileNameResult{}, Total: 0})
		return
	}

	limit := req.Limit
	if limit <= 0 || limit > 100 {
		limit = 50
	}

	// 1. Get Numerology for Lastname
	lastnameSat := 0
	lastnameSha := 0
	if req.Lastname != "" {
		res, err := services.DecodeName(req.Lastname, "")
		if err == nil {
			lastnameSat = res.TotalSat
			lastnameSha = res.TotalSha
		}
	}

	// 2. Get Good Sums
	goodSums, err := services.GetGoodSums()
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, MobileSearchResponse{Success: false, Error: map[string]any{"message": "Failed to calculate good sums"}, Results: []MobileNameResult{}, Total: 0})
		return
	}
	goodSumMap := make(map[int]bool)
	for _, sum := range goodSums {
		goodSumMap[sum] = true
	}

	// 2.5 Get pair metadata for granular coloring and ranking
	pairTypeMap, _ := services.GetPairTypesMap()
	pairPointMap, _ := services.GetPairPointsMap()

	targetSatSums := []int{}
	targetShaSums := []int{}

	if req.SimilarMode && req.Lastname != "" {
		for _, s := range goodSums {
			if s > lastnameSat {
				baseSat := s - lastnameSat
				if !req.FilterSat || goodSumMap[baseSat] {
					targetSatSums = append(targetSatSums, baseSat)
				}
			}
			if s > lastnameSha {
				baseSha := s - lastnameSha
				if !req.FilterSha || goodSumMap[baseSha] {
					targetShaSums = append(targetShaSums, baseSha)
				}
			}
		}
	} else {
		targetSatSums = goodSums
		targetShaSums = goodSums
	}

	if len(targetSatSums) == 0 {
		targetSatSums = []int{-1}
	}
	if len(targetShaSums) == 0 {
		targetShaSums = []int{-1}
	}

	// 3. Prepare Kaki filter & Highlight map
	dayMap := map[string]string{
		"Sunday": "k_sunday", "Monday": "k_monday", "Tuesday": "k_tuesday",
		"Wednesday1": "k_wednesday1", "Wednesday2": "k_wednesday2",
		"Thursday": "k_thursday", "Friday": "k_friday", "Saturday": "k_saturday",
	}
	kakiColumn := dayMap[req.Day]

	kakiCharMap := make(map[string]bool)
	var kakiCharList []string
	if req.Day != "" {
		dayThaiMap := map[string]string{
			"Sunday": "อาทิตย์", "Monday": "จันทร์", "Tuesday": "อังคาร",
			"Wednesday1": "พุธกลางวัน", "Wednesday2": "พุธกลางคืน",
			"Thursday": "พฤหัสบดี", "Friday": "ศุกร์", "Saturday": "เสาร์",
		}
		rows, err := database.DB.Query(
			"SELECT kakis FROM kakis_day WHERE LOWER(day) = LOWER($1) OR day_th = $2",
			req.Day, dayThaiMap[req.Day],
		)
		if err == nil {
			defer rows.Close()
			for rows.Next() {
				var k string
				if rows.Scan(&k) == nil {
					k = strings.TrimSpace(k)
					for _, r := range k {
						ch := string(r)
						if !kakiCharMap[ch] {
							kakiCharMap[ch] = true
							kakiCharList = append(kakiCharList, ch)
						}
					}
				}
			}
		}
	}

	// 4. Get Embedding
	embedding, err := services.GetEmbedding(searchContext.MeaningContext)
	if err != nil {
		log.Printf("OpenAI embedding failed: %v", err)
		embedding = make([]float64, 1536)
	}
	log.Printf(
		"MeaningContext: '%s', Strategy='%s', EmbeddingPreview: %v",
		searchContext.MeaningContext,
		searchContext.RetrievalStrategy,
		previewEmbedding(embedding, 5),
	)

	buildQuery := func(relaxFilters bool) (string, []interface{}) {
		isBroadSearch := searchContext.IsBroadSearch

		query := `
			SELECT name_id, thname, meaning, gender,
			       sat_sum, sha_sum, `
		query += searchContext.SelectProjection()

		query += `0.0 as bonus_calculated
			FROM names_miracle 
			WHERE 1=1
		`
		args := []interface{}{formatVector(embedding), searchContext.WordContext}
		argCounter := 3

		// Enforce conditions if Filter is ON OR if we are in "รวมให้เป็น ชื่อดี" (SimilarMode) mode
		var satCond, shaCond string
		if req.FilterSat || req.SimilarMode {
			satCond = fmt.Sprintf("sat_sum = ANY($%d::int[])", argCounter)
			args = append(args, pq.Array(targetSatSums))
			argCounter++
		}
		if req.FilterSha || req.SimilarMode {
			shaCond = fmt.Sprintf("sha_sum = ANY($%d::int[])", argCounter)
			args = append(args, pq.Array(targetShaSums))
			argCounter++
		}

		// CRITICAL: If SimilarMode is active, we MUST have both (if both are provided)
		// but buildQuery's standard AND logic covers this.

		filterCond := ""
		if satCond != "" && shaCond != "" {
			// Logic: If SimilarMode is ON but user hasn't turned on BOTH quality filters,
			// we use OR to find "Partially Good" names. This prevents the "Excellent Gate"
			// on the frontend from hiding every single result (since Double-Green is premium).
			if req.SimilarMode && (!req.FilterSat || !req.FilterSha) {
				filterCond = "(" + satCond + " OR " + shaCond + ")"
			} else if relaxFilters {
				filterCond = "(" + satCond + " OR " + shaCond + ")"
			} else {
				filterCond = "(" + satCond + " AND " + shaCond + ")"
			}
		} else if satCond != "" {
			filterCond = satCond
		} else if shaCond != "" {
			filterCond = shaCond
		}

		if filterCond != "" {
			if req.Keyword != "" && !req.SimilarMode {
				query += " AND (" + filterCond + " OR thname = $" + fmt.Sprintf("%d", argCounter) + ")"
				args = append(args, req.Keyword)
				argCounter++
			} else {
				query += " AND (" + filterCond + ")"
			}
		}

		if req.FilterKaki && kakiColumn != "" {
			query += fmt.Sprintf(" AND %s = false", kakiColumn)
		}

		if req.Lastname != "" {
			// Exclude the exact matching name/surname from being recommended as the first name
			query += " AND thname != $" + fmt.Sprintf("%d", argCounter)
			args = append(args, req.Lastname)
			argCounter++
		}

		// PRIORITY: Prioritize semantic meaning matches so they make the LIMIT cut
		if isBroadSearch {
			// In broad discovery, we can't sort by meaning. Use name_id as a base.
			query += " ORDER BY name_id DESC "
		} else {
			query += " ORDER BY " + searchContext.OrderByExpression() + " "
		}
		limitVal := searchContext.CandidateLimit
		query += fmt.Sprintf(" LIMIT %d", limitVal)

		log.Printf("DB Query: %s, Args: %+v", query, args)
		return query, args
	}

	runAndScan := func(relax bool) ([]MobileNameResult, error) {
		query, args := buildQuery(relax)
		rows, err := database.DB.Query(query, args...)
		if err != nil {
			return nil, err
		}
		defer rows.Close()

		var out []MobileNameResult
		for rows.Next() {
			var r MobileNameResult
			var id int
			if err := rows.Scan(&id, &r.Name, &r.Meaning, &r.Gender, &r.SatSum, &r.ShaSum, &r.Distance, &r.RootScore, &r.SemanticScore, &r.HybridScore, &r.BonusCalculated); err != nil {
				continue
			}

			r.IsSatGood = goodSumMap[r.SatSum]
			r.IsShaGood = goodSumMap[r.ShaSum]

			if req.Lastname != "" {
				r.TotalSat = r.SatSum + lastnameSat
				r.TotalSha = r.ShaSum + lastnameSha
				r.IsTotalSatGood = goodSumMap[r.TotalSat]
				r.IsTotalShaGood = goodSumMap[r.TotalSha]

				if pairTypeMap != nil {
					r.TotalSatPairType = pairTypeMap[fmt.Sprintf("%d", r.TotalSat)]
					r.TotalShaPairType = pairTypeMap[fmt.Sprintf("%d", r.TotalSha)]
				}
				if pairPointMap != nil {
					r.TotalSatPairPoint = pairPointMap[fmt.Sprintf("%d", r.TotalSat)]
					r.TotalShaPairPoint = pairPointMap[fmt.Sprintf("%d", r.TotalSha)]
				}
			}

			if pairTypeMap != nil {
				r.SatPairType = pairTypeMap[fmt.Sprintf("%d", r.SatSum)]
				r.ShaPairType = pairTypeMap[fmt.Sprintf("%d", r.ShaSum)]
			}
			if pairPointMap != nil {
				r.SatPairPoint = pairPointMap[fmt.Sprintf("%d", r.SatSum)]
				r.ShaPairPoint = pairPointMap[fmt.Sprintf("%d", r.ShaSum)]
			}

			// Highlight Kaki: Individual codepoints (runes).
			// We no longer group them because the App will handle shaping with CustomPainter.
			if len(kakiCharMap) > 0 {
				for _, rn := range r.Name {
					ch := string(rn)
					r.KakiHighlight = append(r.KakiHighlight, CharHighlight{
						Char:   ch,
						IsKaki: kakiCharMap[ch],
					})
				}
			}

			// Calculate bonus score based on frontend logic
			r.BonusCalculated = calculateBonus(r.Name, r.IsSatGood, r.IsShaGood, r.IsTotalSatGood, r.IsTotalShaGood, r.KakiHighlight)
			r.FinalRankScore, r.RankReasons = calculateFinalRankScoreAndReasons(&r, req.SimilarMode && req.Lastname != "")

			out = append(out, r)
		}
		return out, nil
	}

	runDesperateSearch := func() ([]MobileNameResult, error) {
		query := "SELECT name_id, thname, meaning, gender, sat_sum, sha_sum, " +
			searchContext.SelectProjection() +
			"0.0 as bonus_calculated FROM names_miracle WHERE 1=1"
		args := []interface{}{formatVector(embedding), searchContext.WordContext}
		argCounter := 3

		// Gender filter removed per user request

		if req.FilterKaki && kakiColumn != "" {
			query += fmt.Sprintf(" AND %s = false", kakiColumn)
		}

		// DESPERATE LEVEL: Ignore Sat/Sha but keep Kaki/Gender

		if req.Lastname != "" {
			// Exclude the exact matching name/surname from being recommended as the first name
			query += " AND thname != $" + fmt.Sprintf("%d", argCounter)
			args = append(args, req.Lastname)
			argCounter++
		}

		query += fmt.Sprintf(
			" ORDER BY %s LIMIT %d",
			searchContext.OrderByExpression(),
			searchContext.CandidateLimit,
		)

		rows, err := database.DB.Query(query, args...)
		if err != nil {
			return nil, err
		}
		defer rows.Close()

		var out []MobileNameResult
		for rows.Next() {
			var r MobileNameResult
			var id int
			if err := rows.Scan(&id, &r.Name, &r.Meaning, &r.Gender, &r.SatSum, &r.ShaSum, &r.Distance, &r.RootScore, &r.SemanticScore, &r.HybridScore, &r.BonusCalculated); err != nil {
				continue
			}

			r.IsSatGood = goodSumMap[r.SatSum]
			r.IsShaGood = goodSumMap[r.ShaSum]

			if req.Lastname != "" {
				r.TotalSat = r.SatSum + lastnameSat
				r.TotalSha = r.ShaSum + lastnameSha
				r.IsTotalSatGood = goodSumMap[r.TotalSat]
				r.IsTotalShaGood = goodSumMap[r.TotalSha]

				if pairTypeMap != nil {
					r.TotalSatPairType = pairTypeMap[fmt.Sprintf("%d", r.TotalSat)]
					r.TotalShaPairType = pairTypeMap[fmt.Sprintf("%d", r.TotalSha)]
				}
				if pairPointMap != nil {
					r.TotalSatPairPoint = pairPointMap[fmt.Sprintf("%d", r.TotalSat)]
					r.TotalShaPairPoint = pairPointMap[fmt.Sprintf("%d", r.TotalSha)]
				}
			}

			if pairTypeMap != nil {
				r.SatPairType = pairTypeMap[fmt.Sprintf("%d", r.SatSum)]
				r.ShaPairType = pairTypeMap[fmt.Sprintf("%d", r.ShaSum)]
			}
			if pairPointMap != nil {
				r.SatPairPoint = pairPointMap[fmt.Sprintf("%d", r.SatSum)]
				r.ShaPairPoint = pairPointMap[fmt.Sprintf("%d", r.ShaSum)]
			}

			// Highlight Kaki: Individual codepoints (runes).
			if len(kakiCharMap) > 0 {
				for _, rn := range r.Name {
					ch := string(rn)
					r.KakiHighlight = append(r.KakiHighlight, CharHighlight{
						Char:   ch,
						IsKaki: kakiCharMap[ch],
					})
				}
			}

			// Calculate bonus score based on frontend logic
			r.BonusCalculated = calculateBonus(r.Name, r.IsSatGood, r.IsShaGood, r.IsTotalSatGood, r.IsTotalShaGood, r.KakiHighlight)
			r.FinalRankScore, r.RankReasons = calculateFinalRankScoreAndReasons(&r, req.SimilarMode && req.Lastname != "")

			out = append(out, r)
		}
		return out, nil
	}

	var results []MobileNameResult

	// We use the package-level DB. If you want transaction isolation for ef_search,
	// you would need to pass tx into runAndScan. Here we just set it globally or skip it.
	// For simplicity, we fallback to standard execution.
	results, err = runAndScan(false)
	log.Printf("Primary results: %d (Err: %v)", len(results), err)
	if err != nil || len(results) == 0 {
		log.Printf("Primary search empty (err=%v), running fallback (Desperate)...", err)
		results, _ = runDesperateSearch()
		log.Printf("Fallback results: %d", len(results))
	}

	if results == nil {
		results = []MobileNameResult{}
	}

	sort.SliceStable(results, func(i, j int) bool {
		mi := normalizedMeaningKey(results[i].Meaning)
		mj := normalizedMeaningKey(results[j].Meaning)
		if mi != "" && mi == mj && results[i].NumerologyScore != results[j].NumerologyScore {
			return results[i].NumerologyScore > results[j].NumerologyScore
		}
		if results[i].FinalRankScore != results[j].FinalRankScore {
			return results[i].FinalRankScore > results[j].FinalRankScore
		}
		if results[i].NumerologyScore != results[j].NumerologyScore {
			return results[i].NumerologyScore > results[j].NumerologyScore
		}
		if results[i].SemanticScore != results[j].SemanticScore {
			return results[i].SemanticScore > results[j].SemanticScore
		}
		if results[i].HybridScore != results[j].HybridScore {
			return results[i].HybridScore > results[j].HybridScore
		}
		return results[i].Name < results[j].Name
	})

	// 5. Build Final Response
	resp := MobileSearchResponse{
		Success: true,
		Results: results,
		Total:   len(results),
		RetrievalMeta: &RetrievalMeta{
			EntryMode:         string(searchContext.EntryMode),
			RetrievalStrategy: searchContext.RetrievalStrategy,
			MeaningContext:    searchContext.MeaningContext,
			SemanticWeight:    searchContext.SemanticWeight,
			WordWeight:        searchContext.WordWeight,
			CandidateLimit:    searchContext.CandidateLimit,
		},
	}

	if req.Day != "" {
		dayThaiMap := map[string]string{
			"Sunday": "อาทิตย์", "Monday": "จันทร์", "Tuesday": "อังคาร",
			"Wednesday1": "พุธกลางวัน", "Wednesday2": "พุธกลางคืน",
			"Thursday": "พฤหัสบดี", "Friday": "ศุกร์", "Saturday": "เสาร์",
		}
		descMap := map[string]string{
			"Sunday": "ศ ษ ส ห ฬ ฮ", "Monday": "สระทั้งหมด + ตัวการันต์",
			"Tuesday": "ก ข ค ฆ ง", "Wednesday1": "จ ฉ ช ซ ฌ ญ",
			"Wednesday2": "บ ป ผ ฝ พ ฟ ภ ม", "Thursday": "ด ต ถ ท ธ น",
			"Friday": "ย ร ล ว", "Saturday": "ฎ ฏ ฐ ฑ ฒ ณ",
		}
		resp.KakiInfo = &KakiInfo{
			Day:         req.Day,
			DayThai:     dayThaiMap[req.Day],
			Description: descMap[req.Day],
			KakiChars:   kakiCharList,
		}
	}

	jsonResponse(w, http.StatusOK, resp)
}

func previewEmbedding(embedding []float64, size int) []float64 {
	if len(embedding) == 0 || size <= 0 {
		return []float64{}
	}
	if len(embedding) < size {
		size = len(embedding)
	}
	return embedding[:size]
}
