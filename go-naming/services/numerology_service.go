package services

import (
	"fmt"
	"go-naming/database"
	"go-naming/models"
	"strings"

	"github.com/lib/pq"
)

// DecodeName splits a Thai name into characters and looks up their numerology values
func DecodeName(name string, birthDay string) (*models.DecodeResult, error) {
	runes := []rune(name)
	result := &models.DecodeResult{
		Name:       name,
		Characters: make([]models.CharValue, 0, len(runes)),
	}

	// Step 4 Preparation: Fetch inauspicious characters for the day
	kakiMap := make(map[string]bool)
	if birthDay != "" {
		// Helper map for day translation
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

		loadKMap := func(day string) {
			// Querying using 'kakis' as per the schema provided in the Skill
			rows, err := database.DB.Query("SELECT kakis FROM kakis_day WHERE LOWER(day) = LOWER($1) OR day_th = $2", day, dayThai[day])
			if err != nil {
				fmt.Printf("DEBUG: Error querying kakis_day: %v\n", err)
				return
			}
			defer rows.Close()
			for rows.Next() {
				var kakiChar string
				if err := rows.Scan(&kakiChar); err == nil {
					k := strings.TrimSpace(kakiChar)
					// Handle cases where multiple characters might be in one field (though user says it's one per record)
					for _, r := range k {
						kakiMap[string(r)] = true
					}
				}
			}
		}

		loadKMap(birthDay)

		var keys []string
		for k := range kakiMap {
			keys = append(keys, k)
		}
		fmt.Printf("DEBUG: Loaded KakiMap for Day %s: %v (Count: %d)\n", birthDay, keys, len(kakiMap))
	}

	for _, r := range runes {
		char := string(r)
		cv := models.CharValue{Char: char}

		// Lookup SAT value
		err := database.DB.QueryRow("SELECT sat_value FROM sat_nums WHERE char_key = $1", char).Scan(&cv.SatValue)
		if err != nil {
			cv.SatValue = 0
		}

		// Lookup SHA value
		err = database.DB.QueryRow("SELECT sha_value FROM sha_nums WHERE char_key = $1", char).Scan(&cv.ShaValue)
		if err != nil {
			cv.ShaValue = 0
		}

		// Step 4: Kalakini Check
		if kakiMap[char] {
			cv.IsKaki = true
			fmt.Printf("DEBUG: Found Kaki character: %s for Day: %s\n", char, birthDay)
		}

		result.Characters = append(result.Characters, cv)
		result.TotalSat += cv.SatValue
		result.TotalSha += cv.ShaValue
	}
	// fmt.Printf("DEBUG: DecodeName for %s on %s - KakiMap Size: %d\n", name, birthDay, len(kakiMap))

	// Step 2: Sliding Window Pairing
	result.SatPairs = CreatePairs(result.TotalSat)
	result.ShaPairs = CreatePairs(result.TotalSha)

	// Step 3: Predictive Analysis
	result.SatDetails = analyzePairs(result.SatPairs)
	result.ShaDetails = analyzePairs(result.ShaPairs)

	return result, nil
}

// analyzePairs lookups each pair in the 'numbers' table
func analyzePairs(pairs []string) []models.PairAnalysis {
	details := make([]models.PairAnalysis, 0, len(pairs))
	for _, p := range pairs {
		analysis := models.PairAnalysis{PairNumber: p}

		// Lookup in 'numbers' table
		// Spec: (D10, D8, D5 = Good) | (R10, R7, R5 = Bad)
		err := database.DB.QueryRow("SELECT pairtype, pairpoint FROM numbers WHERE pairnumber = $1", p).
			Scan(&analysis.PairType, &analysis.PairPoint)
		analysis.PairType = strings.TrimSpace(analysis.PairType)

		if err == nil {
			// Determine if it's Good/Bad based on PairType (Starts with 'D' is Good)
			if len(analysis.PairType) > 0 && analysis.PairType[0] == 'D' {
				analysis.IsGood = true
			}
		} else {
			// If not found, we can leave it as empty/false or handle as unknown
			analysis.PairType = "Unknown"
		}

		details = append(details, analysis)
	}
	return details
}

// CreatePairs converts sum into string pairs based on business rules
func CreatePairs(sum int) []string {
	if sum < 0 {
		return []string{}
	}

	if sum < 10 {
		// Rule: If Sum < 10 (e.g., 1): ["01"] (Zero-padded)
		return []string{fmt.Sprintf("0%d", sum)}
	}

	s := fmt.Sprintf("%d", sum)
	if sum >= 10 && sum <= 99 {
		// Rule: If Sum 10-99 (e.g., 41): ["41"]
		return []string{s}
	}

	// Rule: If Sum >= 100 (e.g., 641): ["64", "41"]
	var pairs []string
	for i := 0; i < len(s)-1; i++ {
		pairs = append(pairs, s[i:i+2])
	}
	return pairs
}

// GetPairMeaningsMap fetches all pair numbers and returns a map of pair->meaning (Good/Bad)
func GetPairMeaningsMap() (map[string]string, error) {
	query := "SELECT pairnumber, pairtype FROM numbers"
	rows, err := database.DB.Query(query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	meaningMap := make(map[string]string)
	for rows.Next() {
		var num, pType string
		if err := rows.Scan(&num, &pType); err == nil {
			num = strings.TrimSpace(num)
			pt := strings.TrimSpace(pType)
			if len(pt) > 0 {
				m := ""
				if pt[0] == 'D' {
					m = "Good (" + pt + ")"
				} else if pt[0] == 'R' {
					m = "Bad (" + pt + ")"
				} else {
					m = pt
				}
				meaningMap[num] = m
				// Also store non-padded version if it starts with 0
				if len(num) == 2 && num[0] == '0' {
					meaningMap[num[1:]] = m
				}
			}
		}
	}
	return meaningMap, nil
}

// GetPairTypesMap returns a mapping of pair number -> pairtype (D5, R7 etc.)
func GetPairTypesMap() (map[string]string, error) {
	query := "SELECT pairnumber, pairtype FROM numbers"
	rows, err := database.DB.Query(query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	typeMap := make(map[string]string)
	for rows.Next() {
		var num, pType string
		if err := rows.Scan(&num, &pType); err == nil {
			num = strings.TrimSpace(num)
			pt := strings.TrimSpace(pType)
			typeMap[num] = pt
			// Also store non-padded version if it starts with 0
			if len(num) == 2 && num[0] == '0' {
				typeMap[num[1:]] = pt
			}
		}
	}
	return typeMap, nil
}

// GetPairPointsMap returns a mapping of pair number -> pairpoint.
func GetPairPointsMap() (map[string]int, error) {
	query := "SELECT pairnumber, pairpoint FROM numbers"
	rows, err := database.DB.Query(query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	pointMap := make(map[string]int)
	for rows.Next() {
		var num string
		var point int
		if err := rows.Scan(&num, &point); err == nil {
			num = strings.TrimSpace(num)
			pointMap[num] = point
			if len(num) == 2 && num[0] == '0' {
				pointMap[num[1:]] = point
			}
		}
	}
	return pointMap, nil
}

// GetGoodSums returns all sums (1-200) where every pair is Good (D-series)
// Used for DB-level filtering: WHERE sat_sum IN (...) AND sha_sum IN (...)
func GetGoodSums() ([]int, error) {
	meanings, err := GetPairMeaningsMap()
	if err != nil {
		return nil, err
	}
	var goodSums []int
	for i := 1; i <= 200; i++ {
		pairs := CreatePairs(i)
		if len(pairs) == 0 {
			continue
		}
		allGood := true
		for _, p := range pairs {
			m, ok := meanings[p]
			if !ok || !strings.Contains(m, "Good") {
				allGood = false
				break
			}
		}
		if allGood {
			goodSums = append(goodSums, i)
		}
	}
	return goodSums, nil
}

// GetSumsByPairType returns sums (1-200) grouped by best qualifying pairtype.
// A sum is assigned to the highest tier where ALL its pairs qualify.
// Tiers: D10 > D8 > D5 (only D-series are Good; R-series are excluded)
func GetSumsByPairType() (d10 []int, d8 []int, d5 []int, err error) {
	// Fetch pairtype for every pair number
	rows, err := database.DB.Query("SELECT pairnumber, pairtype FROM numbers")
	if err != nil {
		return nil, nil, nil, err
	}
	defer rows.Close()

	pairTypes := make(map[string]string)
	for rows.Next() {
		var num, pt string
		if err := rows.Scan(&num, &pt); err == nil {
			pairTypes[strings.TrimSpace(num)] = strings.TrimSpace(pt)
		}
	}

	// Helper: tier score (higher = better), 0 = not D-series
	tierScore := func(pt string) int {
		switch pt {
		case "D10":
			return 3
		case "D8":
			return 2
		case "D5":
			return 1
		default:
			return 0 // R-series or unknown
		}
	}

	for i := 1; i <= 200; i++ {
		pairs := CreatePairs(i)
		if len(pairs) == 0 {
			continue
		}
		// Find the minimum tier across all pairs (weakest link)
		minTier := 3
		allGood := true
		for _, p := range pairs {
			pt, ok := pairTypes[p]
			if !ok {
				allGood = false
				break
			}
			score := tierScore(pt)
			if score == 0 {
				allGood = false
				break
			}
			if score < minTier {
				minTier = score
			}
		}
		if !allGood {
			continue
		}
		switch minTier {
		case 3:
			d10 = append(d10, i)
		case 2:
			d8 = append(d8, i)
		case 1:
			d5 = append(d5, i)
		}
	}
	return d10, d8, d5, nil
}

// GetPairDescriptionsMap returns a map of pairnumber -> miracledesc
func GetPairDescriptionsMap() (map[string]string, error) {
	query := "SELECT pairnumber, COALESCE(miracledesc, '') FROM numbers"
	rows, err := database.DB.Query(query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	descMap := make(map[string]string)
	for rows.Next() {
		var num, desc string
		if err := rows.Scan(&num, &desc); err == nil {
			descMap[strings.TrimSpace(num)] = strings.TrimSpace(desc)
		}
	}
	return descMap, nil
}

// RecommendNames searches for names based on intent, surname sum, and day of birth
func RecommendNames(intent string, surname string, day string, limit int) ([]models.RecommendResult, error) {
	// 1. Calculate Surname Sums
	surSat := 0
	surSha := 0
	if surname != "" {
		res, _ := DecodeName(surname, "")
		surSat = res.TotalSat
		surSha = res.TotalSha
	}

	// 2. Fetch Meanings Map (Includes Good/Bad info)
	meanings, err := GetPairMeaningsMap()
	if err != nil {
		return nil, err
	}

	// Calculate Surname Meanings once
	var surSatMeanings, surShaMeanings []string
	if surname != "" {
		for _, pair := range CreatePairs(surSat) {
			if m, ok := meanings[pair]; ok {
				surSatMeanings = append(surSatMeanings, fmt.Sprintf("%s: %s", pair, m))
			} else {
				surSatMeanings = append(surSatMeanings, fmt.Sprintf("%s: Unknown", pair))
			}
		}
		for _, pair := range CreatePairs(surSha) {
			if m, ok := meanings[pair]; ok {
				surShaMeanings = append(surShaMeanings, fmt.Sprintf("%s: %s", pair, m))
			} else {
				surShaMeanings = append(surShaMeanings, fmt.Sprintf("%s: Unknown", pair))
			}
		}
	}

	// 3. Kalakini Filter preparation
	kakiColumn := ""
	if day != "" {
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
		kakiColumn = dayMap[day]
	}

	// 4. Vector Search (if intent provided)
	var embedding []float64
	if intent != "" {
		embedding, _ = GetEmbedding(intent)
	}

	// 5. Query Database
	// Fetch raw satnum/shanum arrays to process in Go
	query := `
		SELECT name_id, COALESCE(thname, ''),
		       satnum, shanum
	`
	if len(embedding) > 0 {
		query += fmt.Sprintf(", (meaning_vector <=> '%s') as distance ", formatVector(embedding))
	}
	query += " FROM names_miracle WHERE 1=1 "

	// Filter Kalakini
	if kakiColumn != "" {
		query += fmt.Sprintf(" AND %s = false ", kakiColumn)
	}

	if len(embedding) > 0 {
		query += " ORDER BY distance ASC "
	}
	query += fmt.Sprintf(" LIMIT %d", limit*5)

	// Execute query
	rows, err := database.DB.Query(query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var results []models.RecommendResult
	for rows.Next() {
		var r models.RecommendResult
		var satNumArr, shaNumArr pq.Int64Array // Use pq.Int64Array for scanning
		var distance interface{}

		var err error
		if len(embedding) > 0 {
			err = rows.Scan(&r.NameID, &r.ThName, &satNumArr, &shaNumArr, &distance)
			if d, ok := distance.(float64); ok {
				r.Distance = d
			}
		} else {
			err = rows.Scan(&r.NameID, &r.ThName, &satNumArr, &shaNumArr)
		}

		if err != nil {
			continue
		}

		// Set Surname info
		r.SurnameSatSum = surSat
		r.SurnameShaSum = surSha
		r.SurnameSatMeanings = surSatMeanings
		r.SurnameShaMeanings = surShaMeanings

		// Calculate Name Sums and Pair Meanings
		nameSatSum := 0
		for _, v := range satNumArr {
			nameSatSum += int(v)
			key := fmt.Sprintf("%02d", v)
			if m, ok := meanings[key]; ok {
				r.SatMeanings = append(r.SatMeanings, fmt.Sprintf("%s: %s", key, m))
			} else {
				r.SatMeanings = append(r.SatMeanings, fmt.Sprintf("%s: Unknown", key))
			}
		}
		r.NameSatSum = nameSatSum
		r.SatSum = nameSatSum + surSat // Total

		nameShaSum := 0
		for _, v := range shaNumArr {
			nameShaSum += int(v)
			key := fmt.Sprintf("%02d", v)
			if m, ok := meanings[key]; ok {
				r.ShaMeanings = append(r.ShaMeanings, fmt.Sprintf("%s: %s", key, m))
			} else {
				r.ShaMeanings = append(r.ShaMeanings, fmt.Sprintf("%s: Unknown", key))
			}
		}
		r.NameShaSum = nameShaSum
		r.ShaSum = nameShaSum + surSha // Total

		// Total Sat Meanings
		totalSatPairs := CreatePairs(r.SatSum)
		for _, pair := range totalSatPairs {
			if m, ok := meanings[pair]; ok {
				r.TotalSatMeanings = append(r.TotalSatMeanings, fmt.Sprintf("%s: %s", pair, m))
			} else {
				r.TotalSatMeanings = append(r.TotalSatMeanings, fmt.Sprintf("%s: Unknown", pair))
			}
		}

		// Total Sha Meanings
		totalShaPairs := CreatePairs(r.ShaSum)
		for _, pair := range totalShaPairs {
			if m, ok := meanings[pair]; ok {
				r.TotalShaMeanings = append(r.TotalShaMeanings, fmt.Sprintf("%s: %s", pair, m))
			} else {
				r.TotalShaMeanings = append(r.TotalShaMeanings, fmt.Sprintf("%s: Unknown", pair))
			}
		}

		// Check Goodness
		isSatAllGood := true
		for _, m := range r.TotalSatMeanings {
			if strings.Contains(m, "Bad") {
				isSatAllGood = false
				break
			}
		}
		r.IsGoodSat = isSatAllGood

		isShaAllGood := true
		for _, m := range r.TotalShaMeanings {
			if strings.Contains(m, "Bad") {
				isShaAllGood = false
				break
			}
		}
		r.IsGoodSha = isShaAllGood

		if surname != "" {
			if !r.IsGoodSat && !r.IsGoodSha {
				continue
			}
		}

		results = append(results, r)
		if len(results) >= limit {
			break
		}
	}

	return results, nil
}

// RecommendNamesFast uses the optimized Reverse Numerology Intersection algorithm
func RecommendNamesFast(intent string, surname string, day string, limit int) ([]models.RecommendResult, error) {
	// 1. Calculate Surname Sums
	surSat := 0
	surSha := 0
	if surname != "" {
		res, _ := DecodeName(surname, "")
		surSat = res.TotalSat
		surSha = res.TotalSha
	}

	// 2. Identify Perfect Sums (Target)
	meanings, err := GetPairMeaningsMap()
	if err != nil {
		return nil, err
	}

	perfectSums := make(map[int]bool)
	for i := 1; i <= 200; i++ {
		pairs := CreatePairs(i)
		if len(pairs) == 0 {
			continue
		}
		isGood := true
		for _, p := range pairs {
			m, ok := meanings[p]
			if !ok || !strings.Contains(m, "Good") {
				isGood = false
				break
			}
		}
		if isGood {
			perfectSums[i] = true
		}
	}

	// 3. Determine Required Name Sums
	var validSatNames []int
	var validShaNames []int

	for sum := range perfectSums {
		// Target = Name + Surname  =>  Name = Target - Surname
		if need := sum - surSat; need > 0 {
			validSatNames = append(validSatNames, need)
		}
		if need := sum - surSha; need > 0 {
			validShaNames = append(validShaNames, need)
		}
	}

	if len(validSatNames) == 0 || len(validShaNames) == 0 {
		return []models.RecommendResult{}, nil
	}

	// 4. Build Query
	kakiColumn := ""
	if day != "" {
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
		kakiColumn = dayMap[day]
	}

	var embedding []float64
	if intent != "" {
		embedding, _ = GetEmbedding(intent)
	}

	query := `
		SELECT name_id, COALESCE(thname, ''),
		       satnum, shanum
	`
	if len(embedding) > 0 {
		query += fmt.Sprintf(", (meaning_vector <=> '%s') as distance ", formatVector(embedding))
	}
	query += " FROM names_miracle WHERE 1=1 "

	// Optimized Filters
	query += " AND sat_sum = ANY($1) "
	query += " AND sha_sum = ANY($2) "

	if kakiColumn != "" {
		query += fmt.Sprintf(" AND %s = false ", kakiColumn)
	}

	if len(embedding) > 0 {
		query += " ORDER BY distance ASC "
	}
	query += fmt.Sprintf(" LIMIT %d", limit)

	rows, err := database.DB.Query(query, pq.Array(validSatNames), pq.Array(validShaNames))
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	// 5. Parse Results
	var surSatMeanings, surShaMeanings []string
	if surname != "" {
		for _, pair := range CreatePairs(surSat) {
			if m, ok := meanings[pair]; ok {
				surSatMeanings = append(surSatMeanings, fmt.Sprintf("%s: %s", pair, m))
			} else {
				surSatMeanings = append(surSatMeanings, fmt.Sprintf("%s: Unknown", pair))
			}
		}
		for _, pair := range CreatePairs(surSha) {
			if m, ok := meanings[pair]; ok {
				surShaMeanings = append(surShaMeanings, fmt.Sprintf("%s: %s", pair, m))
			} else {
				surShaMeanings = append(surShaMeanings, fmt.Sprintf("%s: Unknown", pair))
			}
		}
	}

	var results []models.RecommendResult
	for rows.Next() {
		var r models.RecommendResult
		var satNumArr, shaNumArr pq.Int64Array
		var distance interface{}

		var err error
		if len(embedding) > 0 {
			err = rows.Scan(&r.NameID, &r.ThName, &satNumArr, &shaNumArr, &distance)
			if d, ok := distance.(float64); ok {
				r.Distance = d
			}
		} else {
			err = rows.Scan(&r.NameID, &r.ThName, &satNumArr, &shaNumArr)
		}

		if err != nil {
			continue
		}

		r.SurnameSatSum = surSat
		r.SurnameShaSum = surSha
		r.SurnameSatMeanings = surSatMeanings
		r.SurnameShaMeanings = surShaMeanings

		nameSatSum := 0
		for _, v := range satNumArr {
			nameSatSum += int(v)
			key := fmt.Sprintf("%02d", v)
			if m, ok := meanings[key]; ok {
				r.SatMeanings = append(r.SatMeanings, fmt.Sprintf("%s: %s", key, m))
			} else {
				r.SatMeanings = append(r.SatMeanings, fmt.Sprintf("%s: Unknown", key))
			}
		}
		r.NameSatSum = nameSatSum
		r.SatSum = nameSatSum + surSat

		nameShaSum := 0
		for _, v := range shaNumArr {
			nameShaSum += int(v)
			key := fmt.Sprintf("%02d", v)
			if m, ok := meanings[key]; ok {
				r.ShaMeanings = append(r.ShaMeanings, fmt.Sprintf("%s: %s", key, m))
			} else {
				r.ShaMeanings = append(r.ShaMeanings, fmt.Sprintf("%s: Unknown", key))
			}
		}
		r.NameShaSum = nameShaSum
		r.ShaSum = nameShaSum + surSha

		// Total Meanings
		totalSatPairs := CreatePairs(r.SatSum)
		for _, pair := range totalSatPairs {
			if m, ok := meanings[pair]; ok {
				r.TotalSatMeanings = append(r.TotalSatMeanings, fmt.Sprintf("%s: %s", pair, m))
			} else {
				r.TotalSatMeanings = append(r.TotalSatMeanings, fmt.Sprintf("%s: Unknown", pair))
			}
		}

		totalShaPairs := CreatePairs(r.ShaSum)
		for _, pair := range totalShaPairs {
			if m, ok := meanings[pair]; ok {
				r.TotalShaMeanings = append(r.TotalShaMeanings, fmt.Sprintf("%s: %s", pair, m))
			} else {
				r.TotalShaMeanings = append(r.TotalShaMeanings, fmt.Sprintf("%s: Unknown", pair))
			}
		}

		r.IsGoodSat = true
		r.IsGoodSha = true
		results = append(results, r)
	}

	return results, nil
}

// GetGoodNumbers returns all pair numbers that are marked as Good (D-series)
func GetGoodNumbers() ([]string, error) {
	rows, err := database.DB.Query("SELECT pairnumber FROM numbers WHERE pairtype IN ('D10', 'D8', 'D5')")
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var nums []string
	for rows.Next() {
		var n string
		if err := rows.Scan(&n); err != nil {
			continue
		}
		nums = append(nums, n)
	}
	return nums, nil
}

func formatVector(v []float64) string {
	strs := make([]string, len(v))
	for i, val := range v {
		strs[i] = fmt.Sprintf("%f", val)
	}
	return "[" + strings.Join(strs, ",") + "]"
}
