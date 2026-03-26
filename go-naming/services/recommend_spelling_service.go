package services

import (
	"fmt"
	"go-naming/database"
	"go-naming/models"
)

// RecommendSpelling finds names that sound similar (Phonetic Match) to the input name
// but have better numerology scores.
func RecommendSpelling(inputName string, day string, limit int) ([]models.SpellingRecommendation, error) {
	targetCode := GetThaiPhoneticCode(inputName)
	if targetCode == "" {
		return nil, fmt.Errorf("invalid name for phonetic coding")
	}

	// Fetch Meanings Map
	meanings, err := GetPairMeaningsMap()
	if err != nil {
		return nil, err
	}

	firstChar := ""
	for _, r := range inputName {
		firstChar = string(r)
		break
	}

	// Kalakini Column Mapping
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

	// Query candidates
	query := `
		SELECT name_id, thname, satnum, shanum
		FROM names_miracle
		WHERE thname LIKE $1
	`
	if kakiColumn != "" {
		query += fmt.Sprintf(" AND %s = false ", kakiColumn)
	}
	query += " LIMIT 2000 "

	rows, err := database.DB.Query(query, firstChar+"%")
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var candidates []models.SpellingRecommendation
	count := 0

	for rows.Next() {
		var n models.NameMiracle
		if err := rows.Scan(&n.NameID, &n.ThName, &n.SatNum, &n.ShaNum); err != nil {
			continue
		}

		// 1. Phonetic Check
		if GetThaiPhoneticCode(n.ThName) == targetCode {
			// Enhance with meanings
			rec := models.SpellingRecommendation{NameMiracle: n}

			// Resolve SAT Meanings
			for _, val := range n.SatNum {
				key := fmt.Sprintf("%02d", val) // Pad 5 -> "05"
				if m, ok := meanings[key]; ok {
					rec.SatMeanings = append(rec.SatMeanings, fmt.Sprintf("%s: %s", key, m))
				} else {
					rec.SatMeanings = append(rec.SatMeanings, fmt.Sprintf("%s: Unknown", key))
				}
			}

			// Resolve SHA Meanings
			for _, val := range n.ShaNum {
				key := fmt.Sprintf("%02d", val)
				if m, ok := meanings[key]; ok {
					rec.ShaMeanings = append(rec.ShaMeanings, fmt.Sprintf("%s: %s", key, m))
				} else {
					rec.ShaMeanings = append(rec.ShaMeanings, fmt.Sprintf("%s: Unknown", key))
				}
			}

			candidates = append(candidates, rec)
			count++
			if count >= limit {
				break
			}
		}
	}

	return candidates, nil
}
