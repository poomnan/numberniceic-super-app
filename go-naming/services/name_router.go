package services

import (
	"context"
	"database/sql"
	"strings"
	"unicode/utf8"
)

// NameType represents the classification result.
type NameType string

const (
	FirstName NameType = "FIRST_NAME"
	LastName  NameType = "LAST_NAME"
	Meaning   NameType = "MEANING"
)

// RouteResult holds the classification output and confidence score.
type RouteResult struct {
	Type       NameType `json:"type"`
	Confidence float64  `json:"confidence"`
	Reason     string   `json:"reason"`
}

// RouteThaiInput analyzes Thai text input and classifies it as FIRST_NAME, LAST_NAME, or MEANING.
// It is designed to be highly performant and uses database lookups combined with structural rules.
func RouteThaiInput(ctx context.Context, db *sql.DB, input string) (RouteResult, error) {
	// 1. Normalization
	normalized := strings.TrimSpace(input)
	if normalized == "" {
		return RouteResult{
			Type:       Meaning,
			Confidence: 0.0,
			Reason:     "empty input",
		}, nil
	}

	runeCount := utf8.RuneCountInString(normalized)
	hasSpace := strings.Contains(normalized, " ")

	// 2. Perform switch-case checks on input characteristics
	switch {
	// Condition 1: Length and space trick early exit.
	// If there's a space and runecount is > 15 (e.g. name with long descriptive suffix or sentence),
	// or if the single word is extremely long (> 25), route to MEANING immediately.
	case (hasSpace && runeCount > 15) || runeCount > 25:
		return RouteResult{
			Type:       Meaning,
			Confidence: 0.99,
			Reason:     "length exceeds threshold, treated as descriptive phrase/meaning",
		}, nil

	// Check for non-Thai characters (excluding spaces). Thai Unicode block is U+0E00 to U+0E7F.
	case hasNonThaiChars(normalized):
		return RouteResult{
			Type:       Meaning,
			Confidence: 0.95,
			Reason:     "contains non-Thai characters",
		}, nil

	// Condition 3: Semantic check for grammatical markers, verbs, and conjunctions.
	case hasExplicitGrammarOrVerbs(normalized):
		return RouteResult{
			Type:       Meaning,
			Confidence: 0.98,
			Reason:     "matches grammatical/semantic meaning markers",
		}, nil

	// Condition 2: Database lookup of 300,000 names (FIRST_NAME exact match check).
	// If it exists in the first name database, it's highly likely a FIRST_NAME.
	case checkNameInDB(ctx, db, normalized):
		return RouteResult{
			Type:       FirstName,
			Confidence: 1.0,
			Reason:     "found exact match in first names database",
		}, nil

	// Condition 2 (cont.): Pattern matching for surname prefix/suffix.
	case hasStrongSurnamePrefix(normalized) || hasStrongSurnameSuffix(normalized):
		return RouteResult{
			Type:       LastName,
			Confidence: 0.98,
			Reason:     "matches strong surname prefix or suffix patterns",
		}, nil

	// Heuristics for weak/shared prefixes and suffixes
	case hasWeakSurnamePattern(normalized):
		// Surnames are typically longer (average 6-12 runes) compared to first names (3-6 runes).
		// Also, if it has a first name prefix, we resolve conflicts.
		if hasFirstNamePrefix(normalized) && runeCount <= 5 {
			return RouteResult{
				Type:       FirstName,
				Confidence: 0.85,
				Reason:     "matches first name prefix (length <= 5)",
			}, nil
		}
		if runeCount >= 6 {
			return RouteResult{
				Type:       LastName,
				Confidence: 0.90,
				Reason:     "matches surname pattern with length >= 6 runes",
			}, nil
		}
		return RouteResult{
			Type:       FirstName,
			Confidence: 0.75,
			Reason:     "defaulted to first name for short word matching weak pattern",
		}, nil

	// First name prefix detection
	case hasFirstNamePrefix(normalized):
		return RouteResult{
			Type:       FirstName,
			Confidence: 0.90,
			Reason:     "matches first name prefix pattern",
		}, nil

	// Heuristic fallbacks for inputs with space that didn't match name patterns
	case hasSpace:
		return RouteResult{
			Type:       Meaning,
			Confidence: 0.85,
			Reason:     "contains spaces and did not match name patterns",
		}, nil

	// Heuristics based on length
	case runeCount >= 8:
		// Surnames are generally longer and less likely to be arbitrary first names if not in DB.
		return RouteResult{
			Type:       LastName,
			Confidence: 0.80,
			Reason:     "long single word not found in first names database",
		}, nil

	default:
		// Default fallback for short words
		return RouteResult{
			Type:       FirstName,
			Confidence: 0.70,
			Reason:     "short single word defaulting to first name",
		}, nil
	}
}

// hasNonThaiChars returns true if the input contains characters outside the Thai Unicode range
// (excluding spaces, which are allowed).
func hasNonThaiChars(s string) bool {
	for _, r := range s {
		if r == ' ' || r == '\t' || r == '\n' || r == '\r' {
			continue
		}
		// Thai script block is U+0E00 to U+0E7F
		if r < 0x0E00 || r > 0x0E7F {
			return true
		}
	}
	return false
}

// checkNameInDB queries the database to see if the name exists.
func checkNameInDB(ctx context.Context, db *sql.DB, name string) bool {
	if db == nil {
		return false
	}
	var dummy int
	// Query names_miracle table
	err := db.QueryRowContext(ctx, "SELECT 1 FROM names_miracle WHERE thname = $1 LIMIT 1", name).Scan(&dummy)
	return err == nil
}

// hasExplicitGrammarOrVerbs checks if the string contains semantic keywords indicating it's a meaning description.
func hasExplicitGrammarOrVerbs(s string) bool {
	// Absolute triggers: if these words start the string, it is always a meaning
	prefixes := []string{
		"ผู้มี", "ผู้เป็น", "อันเป็น", "สิ่งที่เป็น", "ความ", "การ", "แปลว่า", "หมายถึง", "คือ", "มีลักษณะ",
	}
	for _, p := range prefixes {
		if strings.HasPrefix(s, p) {
			return true
		}
	}

	// Mid-word or standalone grammatical markers and semantic verbs
	markers := []string{
		" แปลว่า ", " หมายถึง ", " อันเป็นที่รัก ", "ประเสริฐยิ่ง", "เจริญรุ่งเรือง", "ผู้เกิดใน",
	}
	for _, m := range markers {
		if strings.Contains(s, m) {
			return true
		}
	}

	// If there's a space, check if any of the components are pure prepositions/conjunctions
	if strings.Contains(s, " ") {
		parts := strings.Fields(s)
		pureConnectors := map[string]bool{
			"และ": true, "หรือ": true, "แต่": true, "เพราะ": true, "จึง": true,
			"ที่": true, "ใน": true, "แห่ง": true, "ของ": true, "กับ": true,
			"จาก": true, "ด้วย": true, "โดย": true, "เพื่อ": true, "สำหรับ": true,
			"ถึง": true, "ผู้": true, "คน": true, "สิ่ง": true, "เป็น": true, "มี": true,
		}
		for _, part := range parts {
			if pureConnectors[part] {
				return true
			}
		}
	}

	return false
}

// hasStrongSurnamePrefix checks for prefixes unique to surnames.
func hasStrongSurnamePrefix(s string) bool {
	// ณ followed by space and characters
	if strings.HasPrefix(s, "ณ ") && utf8.RuneCountInString(s) > 2 {
		return true
	}
	strongPrefixes := []string{"เบญจ", "สุวรรณ"}
	for _, p := range strongPrefixes {
		if strings.HasPrefix(s, p) && utf8.RuneCountInString(s) >= 6 {
			return true
		}
	}
	return false
}

// hasStrongSurnameSuffix checks for suffixes unique to surnames.
func hasStrongSurnameSuffix(s string) bool {
	strongSuffixes := []string{
		"วงศ์", "พงศ์", "พงษ์", "กุล", "พันธุ์", "พันธ์", "สกุล", "ตระกูล",
	}
	for _, suffix := range strongSuffixes {
		if strings.HasSuffix(s, suffix) {
			return true
		}
	}
	return false
}

// hasWeakSurnamePattern checks for prefixes/suffixes that can appear in both first and last names.
func hasWeakSurnamePattern(s string) bool {
	weakPrefixes := []string{
		"ทอง", "รัตน", "ศรี", "เกียรติ", "เดช", "รุ่ง", "เจริญ", "พงษ์", "วิจิตร", "ประเสริฐ", "พิพัฒน์", "วร",
	}
	for _, p := range weakPrefixes {
		if strings.HasPrefix(s, p) {
			return true
		}
	}

	weakSuffixes := []string{
		"ชัย", "เดช", "ศรี", "ทอง", "เจริญ", "สุข", "ดี", "ประสิทธิ์", "รัตน์", "สวัสดิ์", "ประเสริฐ", "คง", "มั่น", "แก้ว", "จันทร์", "อินทร์", "เลิศ", "รักษา", "พรม", "พรหม", "บุตร",
	}
	for _, suffix := range weakSuffixes {
		if strings.HasSuffix(s, suffix) {
			return true
		}
	}

	return false
}

// hasFirstNamePrefix checks for common first name prefixes.
func hasFirstNamePrefix(s string) bool {
	firstNamePrefixes := []string{
		"กิตติ", "เกียรติ", "จิร", "จิระ", "ชล", "ชญา", "ณัฐ", "ณัฏฐ์", "ธน", "ธนา", "ธรรม", "ธีร", "นพ", "นรา", "นันท์", "ปิย", "ปิยะ", "พร", "พล", "พิพัฒน์", "พีร", "ภัทร", "รุ่ง", "วร", "วิชา", "ศิริ", "ศุภ", "สม", "สิริ", "สุ", "อนันต์", "อภิ", "อมร", "อิทธิ", "อุบล",
	}
	for _, p := range firstNamePrefixes {
		if strings.HasPrefix(s, p) {
			return true
		}
	}
	return false
}
