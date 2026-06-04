package handlers

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"log"
	"net/http"
	"strings"

	"github.com/lib/pq"
)

// DemoSearchRequest defines the input for the landing page demo
type DemoSearchRequest struct {
	Keyword     string `json:"keyword"`
	Day         string `json:"day"`
	Gender      string `json:"gender"`
	Lastname    string `json:"lastname"`
	FilterSat   bool   `json:"filter_sat"`
	FilterSha   bool   `json:"filter_sha"`
	FilterKaki  bool   `json:"filter_kaki"`
	SimilarMode bool   `json:"similar_mode"`
}

// DemoSearchHandler implements the "Single Query Strategy" from SKILL.md
// It filters by Good Sums (Sat/Sha), Kalakini (Day), and Meaning (Vector) in ONE query.
func DemoSearchHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req DemoSearchRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid JSON body", http.StatusBadRequest)
		return
	}
	log.Printf("DEBUG: DemoSearchRequest: %+v", req)

	// If Keyword is empty, try to use Lastname as the search context (Meaning Search)
	searchContext := req.Keyword
	if req.Lastname != "" {
		if searchContext == "" {
			searchContext = req.Lastname
		} else if req.SimilarMode {
			// In similar mode, we combine them to find the "Meaning" with the "Style" of the Matching Name
			searchContext = fmt.Sprintf("%s %s", req.Keyword, req.Lastname)
		}
	}

	if searchContext == "" {
		jsonResponse(w, http.StatusOK, []interface{}{})
		return
	}

	lastnameSat := 0
	lastnameSha := 0
	if req.Lastname != "" {
		res, err := services.DecodeName(req.Lastname, "")
		if err == nil {
			lastnameSat = res.TotalSat
			lastnameSha = res.TotalSha
		}
	}

	goodSums, err := services.GetGoodSums()
	if err != nil {
		log.Printf("Error getting good sums: %v", err)
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Failed to calculate good sums"})
		return
	}

	kakiColumn := ""
	if req.Day != "" {
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
		kakiColumn = dayMap[req.Day]
	}

	embedding, err := services.GetEmbedding(searchContext)
	if err != nil {
		log.Printf("Error getting embedding: %v", err)
		embedding = make([]float64, 1536)
	}

	goodSumMap := make(map[int]bool)
	for _, sum := range goodSums {
		goodSumMap[sum] = true
	}

	var targetSatSums []int
	var targetShaSums []int

	if req.Lastname != "" {
		for _, sum := range goodSums {
			if sum > lastnameSat {
				targetSatSums = append(targetSatSums, sum-lastnameSat)
			}
			if sum > lastnameSha {
				targetShaSums = append(targetShaSums, sum-lastnameSha)
			}
		}
	} else {
		targetSatSums = goodSums
		targetShaSums = goodSums
	}

	// Ensure we don't pass an empty array to ANY() as that might cause the query to fail
	if len(targetSatSums) == 0 {
		targetSatSums = []int{-1}
	}
	if len(targetShaSums) == 0 {
		targetShaSums = []int{-1}
	}

	type DemoResult struct {
		Name           string  `json:"name"`
		Meaning        string  `json:"meaning"`
		Gender         string  `json:"gender"`
		SatSum         int     `json:"sat_sum"`
		ShaSum         int     `json:"sha_sum"`
		TotalSat       int     `json:"total_sat"`
		TotalSha       int     `json:"total_sha"`
		Distance       float64 `json:"distance"`
		IsGood         bool    `json:"is_good"`
		IsSatGood      bool    `json:"is_sat_good"`
		IsShaGood      bool    `json:"is_sha_good"`
		IsTotalSatGood bool    `json:"is_total_sat_good"`
		IsTotalShaGood bool    `json:"is_total_sha_good"`
	}

	buildQuery := func(includeUnisex bool, relaxFilters bool) (string, []interface{}) {
		query := `
			SELECT name_id, COALESCE(thname, ''), COALESCE(meaning, ''), COALESCE(gender, ''),
			       sat_sum, sha_sum,
			       (meaning_vector <=> $1) as distance
			FROM names_miracle
			WHERE meaning_vector IS NOT NULL
		`
		args := []interface{}{formatVector(embedding)}
		argCounter := 2

		var satCond, shaCond string
		if req.FilterSat {
			satCond = fmt.Sprintf("sat_sum = ANY($%d::int[])", argCounter)
			args = append(args, pq.Array(targetSatSums))
			argCounter++
		}
		if req.FilterSha {
			shaCond = fmt.Sprintf("sha_sum = ANY($%d::int[])", argCounter)
			args = append(args, pq.Array(targetShaSums))
			argCounter++
		}

		if satCond != "" && shaCond != "" {
			if relaxFilters {
				query += " AND (" + satCond + " OR " + shaCond + ")"
			} else {
				query += " AND " + satCond + " AND " + shaCond
			}
		} else if satCond != "" {
			query += " AND " + satCond
		} else if shaCond != "" {
			query += " AND " + shaCond
		}

		if req.FilterKaki && kakiColumn != "" {
			query += fmt.Sprintf(" AND %s = false", kakiColumn)
		}

		// Prioritize exact match if lastname exists
		if req.Lastname != "" {
			query += fmt.Sprintf(" ORDER BY (CASE WHEN thname = $%d THEN 0 ELSE 1 END), distance ASC LIMIT 20", argCounter)
			args = append(args, req.Lastname)
			argCounter++
		} else {
			query += " ORDER BY distance ASC LIMIT 20"
		}

		return query, args
	}

	runAndScan := func(includeUnisex bool, relax bool) ([]DemoResult, error) {
		query, args := buildQuery(includeUnisex, relax)
		rows, err := database.DB.Query(query, args...)
		if err != nil {
			return nil, err
		}
		defer rows.Close()

		var out []DemoResult
		for rows.Next() {
			var r DemoResult
			var id int
			err := rows.Scan(&id, &r.Name, &r.Meaning, &r.Gender, &r.SatSum, &r.ShaSum, &r.Distance)
			if err != nil {
				log.Printf("Row scan error: %v", err)
				continue
			}

			r.IsSatGood = goodSumMap[r.SatSum]
			r.IsShaGood = goodSumMap[r.ShaSum]
			r.IsGood = (!req.FilterSat || r.IsSatGood) && (!req.FilterSha || r.IsShaGood)

			if req.Lastname != "" {
				r.TotalSat = r.SatSum + lastnameSat
				r.TotalSha = r.ShaSum + lastnameSha
				r.IsTotalSatGood = goodSumMap[r.TotalSat]
				r.IsTotalShaGood = goodSumMap[r.TotalSha]
			}
			out = append(out, r)
		}
		return out, nil
	}

	runDesperateSearch := func() ([]DemoResult, error) {
		intsToString := func(a []int) string {
			if len(a) == 0 {
				return "-1"
			}
			strs := make([]string, len(a))
			for i, v := range a {
				strs[i] = fmt.Sprintf("%d", v)
			}
			return strings.Join(strs, ",")
		}
		allGoodSatStr := intsToString(targetSatSums)
		allGoodShaStr := intsToString(targetShaSums)

		distanceExpr := "0.5 as distance"
		orderBy := "distance ASC"
		args := []interface{}{}
		if len(embedding) > 0 {
			distanceExpr = "(meaning_vector <=> $1) as distance"
			orderBy = "distance ASC"
			args = append(args, formatVector(embedding))
		}
		argCounter := len(args) + 1

		query := fmt.Sprintf(`
				SELECT name_id, COALESCE(thname, ''), COALESCE(meaning, ''), COALESCE(gender, ''),
				       sat_sum, sha_sum, %s
				FROM names_miracle
				WHERE (1=1)
			`, distanceExpr)
		if len(embedding) > 0 {
			query += " AND meaning_vector IS NOT NULL"
		}

		if req.FilterKaki && kakiColumn != "" {
			query += fmt.Sprintf(" AND %s = false", kakiColumn)
		}

		if req.FilterSat {
			query += fmt.Sprintf(" AND sat_sum IN (%s)", allGoodSatStr)
		}
		if req.FilterSha {
			query += fmt.Sprintf(" AND sha_sum IN (%s)", allGoodShaStr)
		}

		if req.Lastname != "" {
			query += fmt.Sprintf(" ORDER BY (CASE WHEN thname = $%d THEN 0 ELSE 1 END), %s LIMIT 20", argCounter, orderBy)
			args = append(args, req.Lastname)
		} else {
			query += " " + orderBy + " LIMIT 20"
		}

		var rows *sql.Rows
		var err error
		if len(embedding) > 0 {
			rows, err = database.DB.Query(query, args...)
		} else {
			rows, err = database.DB.Query(query)
		}

		if err != nil {
			return nil, err
		}
		defer rows.Close()

		var out []DemoResult
		for rows.Next() {
			var r DemoResult
			var id int
			err := rows.Scan(&id, &r.Name, &r.Meaning, &r.Gender, &r.SatSum, &r.ShaSum, &r.Distance)
			if err != nil {
				continue
			}
			r.IsSatGood = goodSumMap[r.SatSum]
			r.IsShaGood = goodSumMap[r.ShaSum]
			r.IsGood = true
			if req.Lastname != "" {
				r.TotalSat = r.SatSum + lastnameSat
				r.TotalSha = r.ShaSum + lastnameSha
				r.IsTotalSatGood = goodSumMap[r.TotalSat]
				r.IsTotalShaGood = goodSumMap[r.TotalSha]
			}
			out = append(out, r)
		}
		return out, nil
	}

	includeUnisex := (req.Gender == "ช" || req.Gender == "ญ")
	results, err := runAndScan(includeUnisex, false)
	if err != nil {
		log.Printf("Warning: Strict search failed: %v. Proceeding to fallback.", err)
		results = []DemoResult{}
	}

	if len(results) == 0 && (req.FilterSat || req.FilterSha) {
		relaxedResults, err := runAndScan(includeUnisex, true)
		if err == nil && len(relaxedResults) > 0 {
			results = relaxedResults
		} else {
			desperateResults, err := runDesperateSearch()
			if err == nil && len(desperateResults) > 0 {
				results = desperateResults
			} else {
				jsonResponse(w, http.StatusOK, map[string]string{"error": "ไม่พบรายชื่อที่ตรงเงื่อนไข"})
				return
			}
		}
	}

	if results == nil {
		results = []DemoResult{}
	}
	jsonResponse(w, http.StatusOK, results)
}
