package services

import (
	"context"
	"database/sql"
	"strings"
	"unicode/utf8"
)

type InputClassification struct {
	Type       string   `json:"type"`       // "single_name", "full_name", "meaning"
	Confidence float64  `json:"confidence"` // 0.0–1.0
	FirstName  string   `json:"first_name,omitempty"`
	Surname    string   `json:"surname,omitempty"`
	Signals    []string `json:"signals"` // ["exact_db_match", "structural", "rhyme"]
}

// ClassifyInput classifies user input into single_name, full_name, or meaning.
func ClassifyInput(input string, db *sql.DB) InputClassification {
	input = strings.TrimSpace(input)
	if input == "" {
		return InputClassification{
			Type:       "meaning",
			Confidence: 0.0,
			Signals:    []string{},
		}
	}

	signals := []string{}

	// 1. DATABASE EXACT MATCH (Highest Priority)
	if exactNameExists(db, input) {
		signals = append(signals, "exact_db_match")
		return InputClassification{
			Type:       "single_name",
			Confidence: 1.0,
			FirstName:  input,
			Signals:    signals,
		}
	}

	parts := strings.Fields(input)
	tokenNameMatches := exactNameTokenMatches(db, parts)

	// 2. STRUCTURAL CHECKS (Layer 1)
	isFullName, first, last := looksLikeThaiFullNameLocal(input)
	isSingleName := looksLikeTypedThaiNameLocal(input)

	var rhyme bool
	if isFullName {
		rhyme = hasThaiNameRhyme(first, last)
		signals = append(signals, "structural_full_name")
		if rhyme {
			signals = append(signals, "rhyme")
		}
	} else {
		// Try to check rhyme on raw 2 parts
		parts := strings.Fields(input)
		if len(parts) == 2 {
			rhyme = hasThaiNameRhyme(parts[0], parts[1])
		}
		if isSingleName {
			signals = append(signals, "structural_single_name")
		}
	}

	meaningSignals := meaningPhraseSignals(input)

	if len(parts) == 2 && isFullName {
		if result, ok := classifyTwoPartThaiInput(input, first, last, tokenNameMatches[first], tokenNameMatches[last], rhyme, meaningSignals, signals); ok {
			return result
		}
	}

	// Strong structural names should win before weak semantic keywords. When a
	// two-part phrase has no name-like rhyme and carries meaning vocabulary,
	// treat it as a semantic query instead of a full name.
	if isFullName {
		if !rhyme && len(meaningSignals) > 0 {
			signals = append(signals, meaningSignals...)
			return InputClassification{
				Type:       "meaning",
				Confidence: 0.94,
				Signals:    uniqueSignals(signals),
			}
		}
		confidence := 0.90
		if rhyme {
			confidence = 1.0
		}
		return InputClassification{
			Type:       "full_name",
			Confidence: confidence,
			FirstName:  first,
			Surname:    last,
			Signals:    signals,
		}
	}

	if len(meaningSignals) > 0 {
		signals = append(signals, meaningSignals...)
		confidence := 0.88
		if containsSignal(meaningSignals, "meaning_keywords") {
			confidence = 0.94
		}
		if containsSignal(meaningSignals, "descriptive_phrase") ||
			containsSignal(meaningSignals, "long_phrase") {
			confidence = 0.96
		}
		return InputClassification{
			Type:       "meaning",
			Confidence: confidence,
			Signals:    signals,
		}
	}

	// 3. DATABASE CHECKS (Layer 2)
	existsInDB := exactNameExists(db, input)
	if existsInDB {
		signals = append(signals, "exact_db_match")
	}

	// Check if any word appears in a meaning column
	var existsInMeaning bool
	if db != nil {
		err := db.QueryRow("SELECT EXISTS(SELECT 1 FROM names_miracle WHERE meaning ILIKE $1 LIMIT 1)", "%"+input+"%").Scan(&existsInMeaning)
		if err == nil && existsInMeaning {
			signals = append(signals, "meaning_db_match")
		}
	}

	if isSingleName {
		if existsInDB {
			return InputClassification{
				Type:       "single_name",
				Confidence: 1.0,
				FirstName:  input,
				Signals:    signals,
			}
		}
		// Rescue borderline case if rhyme was detected on a 2-part input
		parts := strings.Fields(input)
		if len(parts) == 2 && rhyme {
			signals = append(signals, "rhyme_rescue")
			return InputClassification{
				Type:       "full_name",
				Confidence: 0.90,
				FirstName:  parts[0],
				Surname:    parts[1],
				Signals:    signals,
			}
		}
		// Assume valid single name
		return InputClassification{
			Type:       "single_name",
			Confidence: 0.80,
			FirstName:  input,
			Signals:    signals,
		}
	}

	// Call DetectNameIntent as supplementary check
	ctx, cancel := context.WithTimeout(context.Background(), nameIntentQueryTimeout)
	defer cancel()
	intent, err := DetectNameIntent(ctx, db, input)
	if err == nil {
		if intent.Mode == nameIntentModeName {
			signals = append(signals, "intent_name")
			return InputClassification{
				Type:       "single_name",
				Confidence: intent.Confidence,
				Signals:    signals,
			}
		}
		if intent.Mode == nameIntentModeHybrid {
			signals = append(signals, "intent_hybrid")
			return InputClassification{
				Type:       "single_name",
				Confidence: intent.Confidence,
				Signals:    signals,
			}
		}
	}

	// Everything else -> meaning
	confidence := 0.70
	if existsInMeaning {
		confidence = 0.85
	}
	return InputClassification{
		Type:       "meaning",
		Confidence: confidence,
		Signals:    signals,
	}
}

// Local Structural Helpers to avoid cyclic package dependency
func exactNameExists(db *sql.DB, name string) bool {
	name = strings.TrimSpace(name)
	if db == nil || name == "" {
		return false
	}
	var exists bool
	err := db.QueryRow("SELECT EXISTS(SELECT 1 FROM names_miracle WHERE thname = $1 LIMIT 1)", name).Scan(&exists)
	return err == nil && exists
}

func exactNameTokenMatches(db *sql.DB, tokens []string) map[string]bool {
	matches := map[string]bool{}
	if db == nil || len(tokens) == 0 {
		return matches
	}
	seen := map[string]bool{}
	for _, token := range tokens {
		token = strings.TrimSpace(token)
		if token == "" || seen[token] {
			continue
		}
		seen[token] = true
		if exactNameExists(db, token) {
			matches[token] = true
		}
	}
	return matches
}

func classifyTwoPartThaiInput(input, first, last string, firstInDB, lastInDB, rhyme bool, meaningSignals []string, baseSignals []string) (InputClassification, bool) {
	signals := append([]string{}, baseSignals...)
	if firstInDB {
		signals = append(signals, "first_token_db_match")
	}
	if lastInDB {
		signals = append(signals, "second_token_db_match")
	}

	if firstInDB && len(meaningSignals) > 0 && !rhyme {
		signals = append(signals, meaningSignals...)
		return InputClassification{
			Type:       "single_name",
			Confidence: 0.98,
			FirstName:  first,
			Signals:    uniqueSignals(signals),
		}, true
	}
	if lastInDB && !firstInDB && len(meaningSignals) > 0 && !rhyme {
		signals = append(signals, meaningSignals...)
		return InputClassification{
			Type:       "single_name",
			Confidence: 0.98,
			FirstName:  last,
			Signals:    uniqueSignals(signals),
		}, true
	}

	if firstInDB || (lastInDB && rhyme) {
		confidence := 0.96
		if rhyme {
			confidence = 1.0
		}
		return InputClassification{
			Type:       "full_name",
			Confidence: confidence,
			FirstName:  first,
			Surname:    last,
			Signals:    uniqueSignals(signals),
		}, true
	}
	if lastInDB {
		return InputClassification{
			Type:       "single_name",
			Confidence: 0.98,
			FirstName:  last,
			Signals:    uniqueSignals(signals),
		}, true
	}

	if !rhyme && len(meaningSignals) > 0 {
		signals = append(signals, meaningSignals...)
		return InputClassification{
			Type:       "meaning",
			Confidence: 0.94,
			Signals:    uniqueSignals(signals),
		}, true
	}

	return InputClassification{}, false
}

func containsMeaningKeywords(input string) bool {
	keywords := []string{
		"ความ", "การ", "ผู้", "แห่ง", "อย่าง", "เพื่อ", "ให้", "และ", "ของ",
		"เป็น", "หมายถึง", "แปลว่า", "สื่อถึง", "แทนความ", "นำพา",
		"งดงาม", "อ่อนโยน", "อ่อนหวาน", "เมตตา", "มั่งคั่ง", "ร่ำรวย",
		"รุ่งเรือง", "เจริญ", "โชคดี", "บารมี", "ปัญญา", "กล้าหาญ",
		"สว่าง", "ส่องประกาย", "ประกาย", "ดวงดาว", "แสงดาว", "ดั่ง",
		"ประดุจ", "เปรียบ", "อำนาจ", "สำเร็จ", "สุขุม", "บริสุทธิ์",
	}
	for _, kw := range keywords {
		if strings.Contains(input, kw) {
			return true
		}
	}
	return false
}

func meaningPhraseSignals(input string) []string {
	input = strings.TrimSpace(input)
	if input == "" {
		return nil
	}

	signals := []string{}
	if containsMeaningKeywords(input) {
		signals = append(signals, "meaning_keywords")
	}

	runeCount := utf8.RuneCountInString(strings.ReplaceAll(input, " ", ""))
	parts := strings.Fields(input)
	if runeCount > 12 {
		signals = append(signals, "long_phrase")
	}
	if len(parts) >= 3 {
		signals = append(signals, "descriptive_phrase")
	}
	if len(parts) == 2 && !hasThaiNameRhyme(parts[0], parts[1]) {
		if containsMeaningKeywords(input) || runeCount > 12 {
			signals = append(signals, "non_name_two_word_phrase")
		}
	}

	return uniqueSignals(signals)
}

func containsSignal(signals []string, target string) bool {
	for _, signal := range signals {
		if signal == target {
			return true
		}
	}
	return false
}

func uniqueSignals(signals []string) []string {
	if len(signals) <= 1 {
		return signals
	}
	seen := map[string]bool{}
	result := make([]string, 0, len(signals))
	for _, signal := range signals {
		if signal == "" || seen[signal] {
			continue
		}
		seen[signal] = true
		result = append(result, signal)
	}
	return result
}

func looksLikeTypedThaiNameLocal(input string) bool {
	input = strings.TrimSpace(input)
	if input == "" {
		return false
	}
	if strings.ContainsAny(input, " \t\n\r") {
		valid, _, _ := looksLikeThaiFullNameLocal(input)
		return valid
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
	if IsLikelyGibberishThai(input) && !isPlausibleImplicitThaiNameLocal(input) {
		return false
	}
	return true
}

func looksLikeThaiFullNameLocal(input string) (bool, string, string) {
	input = strings.TrimSpace(input)
	if input == "" || strings.ContainsAny(input, "\t\n\r") {
		return false, "", ""
	}
	if strings.Count(input, " ") < 1 || strings.Count(input, " ") > 2 {
		return false, "", ""
	}

	parts := strings.Fields(input)
	if len(parts) != 2 {
		return false, "", ""
	}

	allBasicChecksPass := true
	for _, part := range parts {
		runeCount := utf8.RuneCountInString(part)
		if runeCount < 2 || runeCount > 12 {
			allBasicChecksPass = false
			break
		}
		for _, r := range part {
			if r < 'ก' || r > '๙' {
				allBasicChecksPass = false
				break
			}
		}
		if IsLikelyGibberishThai(part) && !hasThaiVowelOrToneLocal(part) && !isPlausibleImplicitThaiNameLocal(part) {
			allBasicChecksPass = false
			break
		}
	}

	if allBasicChecksPass {
		return true, parts[0], parts[1]
	}

	// Borderline rescue check
	isBorderline := true
	for _, part := range parts {
		runeCount := utf8.RuneCountInString(part)
		if runeCount < 1 || runeCount > 12 {
			isBorderline = false
			break
		}
		for _, r := range part {
			if r < 'ก' || r > '๙' {
				isBorderline = false
				break
			}
		}
	}

	if isBorderline && hasThaiNameRhyme(parts[0], parts[1]) {
		return true, parts[0], parts[1]
	}

	return false, "", ""
}

func hasThaiVowelOrToneLocal(input string) bool {
	for _, r := range input {
		if r >= 'ะ' && r <= '๎' && r != 'ๆ' {
			return true
		}
	}
	return false
}

func isPlausibleImplicitThaiNameLocal(input string) bool {
	input = strings.TrimSpace(input)
	runeCount := utf8.RuneCountInString(input)
	if runeCount < 3 || runeCount > 5 {
		return false
	}
	for _, r := range input {
		if r < 'ก' || r > '๙' {
			return false
		}
	}

	leadingClusters := []string{"หม", "หน", "หง", "หญ", "หย", "หร", "หล", "หว"}
	for _, cluster := range leadingClusters {
		if strings.HasPrefix(input, cluster) {
			return true
		}
	}
	return false
}

// IsLikelyGibberishThai flags short Thai keyboard-like strings that lack any
// vowel/tone signal and repeat the same consonant shape too aggressively.
func IsLikelyGibberishThai(input string) bool {
	input = strings.TrimSpace(input)
	if input == "" {
		return false
	}

	hasThai := false
	hasVowelOrTone := false
	consonantRuns := 0
	longestConsonantRun := 0
	lastConsonant := rune(0)
	repeatedConsonants := 0

	for _, r := range input {
		if r < 'ก' || r > '๙' {
			return false
		}
		hasThai = true
		isVowelOrTone := r >= 'ะ' && r <= '๎' && r != 'ๆ'
		if isVowelOrTone {
			hasVowelOrTone = true
			consonantRuns = 0
			lastConsonant = 0
			continue
		}

		if r >= 'ก' && r <= 'ฮ' {
			consonantRuns++
			if consonantRuns > longestConsonantRun {
				longestConsonantRun = consonantRuns
			}
			if r == lastConsonant {
				repeatedConsonants++
			}
			lastConsonant = r
		}
	}

	runeCount := utf8.RuneCountInString(input)
	return hasThai &&
		!hasVowelOrTone &&
		runeCount >= 4 &&
		(longestConsonantRun >= 4 || repeatedConsonants >= 2)
}

// HasThaiNameRhyme returns true when two Thai name parts share a simple ending
// sound or when the last syllable of part1 rhymes with the first syllable of part2.
// It is intentionally permissive only for two non-empty Thai tokens.
func HasThaiNameRhyme(part1, part2 string) bool {
	part1 = strings.TrimSpace(part1)
	part2 = strings.TrimSpace(part2)
	if part1 == "" || part2 == "" {
		return false
	}

	r1 := []rune(part1)
	r2 := []rune(part2)
	if len(r1) < 2 || len(r2) < 2 {
		return false
	}
	for _, r := range append(r1, r2...) {
		if r < 'ก' || r > '๙' {
			return false
		}
	}

	// Extract Rhyme Key of the last syllable of part1
	v1, f1 := extractLastSyllableRhymeKey(r1)
	if v1 == "" {
		return false
	}

	// 1. Check Tail-to-Head Rhyme (Last syllable of part1 rhymes with first syllable of part2)
	if checkFirstSyllableRhyme(r2, v1, f1) {
		return true
	}

	// 2. Check Tail-to-Tail Rhyme (Last syllable of part1 rhymes with last syllable of part2)
	v2, f2 := extractLastSyllableRhymeKey(r2)
	if v2 != "" && normalizeVowel(v1) == normalizeVowel(v2) && f1 == f2 {
		return true
	}

	return false
}

func isThaiVowel(r rune, idx int, runes []rune) bool {
	// Leading vowels
	if r == 0x0E40 || r == 0x0E41 || r == 0x0E42 || r == 0x0E43 || r == 0x0E44 {
		return true
	}
	// Non-leading vowels
	if r == 0x0E30 || r == 0x0E31 || r == 0x0E32 || r == 0x0E33 || r == 0x0E34 || r == 0x0E35 || r == 0x0E36 || r == 0x0E37 || r == 0x0E38 || r == 0x0E39 || r == 0x0E47 {
		return true
	}
	// Semi-vowels (อ U+0E2D, ว U+0E27) if not the first character of the word/syllable
	if (r == 0x0E2D || r == 0x0E27) && idx > 0 {
		return true
	}
	return false
}

func normalizeVowel(v string) string {
	if v == string(rune(0x0E43)) { // Normalize ใ to ไ
		return string(rune(0x0E44))
	}
	return v
}

func isLeadingVowelRune(r rune) bool {
	return r == 0x0E40 || r == 0x0E41 || r == 0x0E42 || r == 0x0E43 || r == 0x0E44
}

func extractLastSyllableRhymeKey(runes []rune) (string, string) {
	vIdx := -1
	for i := len(runes) - 1; i >= 0; i-- {
		if isThaiVowel(runes[i], i, runes) {
			vIdx = i
			break
		}
	}
	if vIdx == -1 {
		return "", ""
	}

	vowel := string(runes[vIdx])
	var cList []rune
	for i := vIdx + 1; i < len(runes); i++ {
		r := runes[i]
		if r >= 0x0E01 && r <= 0x0E2E {
			cList = append(cList, r)
		}
	}

	finalConsonant := ""
	if isLeadingVowelRune(runes[vIdx]) {
		if len(cList) >= 2 {
			finalConsonant = string(cList[len(cList)-1])
		}
	} else {
		if len(cList) >= 1 {
			finalConsonant = string(cList[len(cList)-1])
		}
	}
	return vowel, finalConsonant
}

func checkFirstSyllableRhyme(runes []rune, targetVowel, targetFinal string) bool {
	if len(runes) == 0 {
		return false
	}

	targetVowel = normalizeVowel(targetVowel)

	if isLeadingVowelRune([]rune(targetVowel)[0]) {
		// Target vowel is leading, so runes[0] must be targetVowel
		if normalizeVowel(string(runes[0])) != targetVowel {
			return false
		}
		if targetFinal == "" {
			if len(runes) > 1 && runes[1] >= 0x0E01 && runes[1] <= 0x0E2E {
				return true
			}
			return false
		} else {
			if len(runes) > 2 && string(runes[2]) == targetFinal {
				return true
			}
			if len(runes) > 3 && runes[2] >= 0x0E01 && runes[2] <= 0x0E2E && string(runes[3]) == targetFinal {
				return true
			}
			return false
		}
	} else {
		// Target vowel is non-leading.
		vIdx := -1
		if len(runes) > 1 && normalizeVowel(string(runes[1])) == targetVowel {
			vIdx = 1
		} else if len(runes) > 2 && runes[1] >= 0x0E01 && runes[1] <= 0x0E2E && normalizeVowel(string(runes[2])) == targetVowel {
			vIdx = 2
		}

		if vIdx == -1 {
			return false
		}

		if targetFinal == "" {
			return true
		} else {
			if len(runes) > vIdx+1 && string(runes[vIdx+1]) == targetFinal {
				return true
			}
			return false
		}
	}
}

func thaiRhymeTail(runes []rune) string {
	start := len(runes) - 1
	for start > 0 {
		r := runes[start]
		if r >= 'ะ' && r <= '๎' && r != 'ๆ' {
			break
		}
		start--
	}
	if start == 0 && len(runes) > 2 {
		start = len(runes) - 2
	}
	return string(runes[start:])
}

// hasThaiNameRhyme checks if the two parts share a Thai rhyme.
func hasThaiNameRhyme(part1, part2 string) bool {
	return HasThaiNameRhyme(part1, part2)
}
