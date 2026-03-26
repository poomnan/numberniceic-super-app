package services

import (
	"strings"
)

// PhoneticService handles sound-based algorithms (Soundex) for Thai Language.
// This allows finding names that sound similar but are spelled differently.

// GetThaiPhoneticCode generates a phonetic code for a given Thai string.
// Algorithm Logic:
// 1. Map each consonant to a specific phonetic group code (1-9).
// 2. Handle Karan (์) by removing the immediately preceding consonant code.
// 3. Collapse adjacent duplicate codes (e.g., 22 -> 2).
// 4. Return the resulting numerical string.
func GetThaiPhoneticCode(text string) string {
	if text == "" {
		return ""
	}

	// 2. Map to Phonetic Groups with Karan Handling
	// Group 1 (K-sound): ก, ข, ฃ, ค, ฅ, ฆ
	// Group 2 (J/Ch/S/D/T): จ, ฉ, ช, ซ, ฌ, ฎ, ฏ, ฐ, ฑ, ฒ, ด, ต, ถ, ท, ธ, ศ, ษ, ส
	// Group 3 (N/L/R): ญ, ณ, น, ร, ล, ฬ
	// Group 4 (B/P/F): บ, ป, ผ, ฝ, พ, ฟ, ภ
	// Group 5 (M): ม
	// Group 6 (Y): ย
	// Group 7 (W): ว
	// Group 8 (H/Silent): ห, ฮ, อ
	// Group 9 (Ng): ง

	// We use a temporary slice to handle "Karan" (Look-back removal) before collapsing.
	var rawCodes []rune

	for _, char := range text { // Iterate over ORIGINAL text to catch Karan

		// Handle Karan (์ - \u0E4C)
		// If we encounter a Karan, it silences the preceding consonant.
		// We remove the last added code from rawCodes.
		if char == '\u0e4c' {
			if len(rawCodes) > 0 {
				rawCodes = rawCodes[:len(rawCodes)-1]
			}
			continue
		}

		// Check if it is a consonant (Thai Consonants \u0E01 - \u0E2E)
		if char >= 0x0E01 && char <= 0x0E2E {
			code := getConsonantCode(char)
			if code != '0' {
				rawCodes = append(rawCodes, code)
			}
		}
	}

	// 3. Collapse adjacent duplicates
	var codeBuilder strings.Builder
	lastCode := ' '

	for _, code := range rawCodes {
		if code != lastCode {
			codeBuilder.WriteRune(code)
			lastCode = code
		}
	}

	return codeBuilder.String()
}

func getConsonantCode(r rune) rune {
	switch r {
	case 'ก', 'ข', 'ฃ', 'ค', 'ฅ', 'ฆ':
		return '1'
	case 'จ', 'ฉ', 'ช', 'ซ', 'ฌ', 'ฎ', 'ฏ', 'ฐ', 'ฑ', 'ฒ', 'ด', 'ต', 'ถ', 'ท', 'ธ', 'ศ', 'ษ', 'ส':
		return '2'
	case 'ญ', 'ณ', 'น', 'ร', 'ล', 'ฬ':
		return '3'
	case 'บ', 'ป', 'ผ', 'ฝ', 'พ', 'ฟ', 'ภ':
		return '4'
	case 'ม':
		return '5'
	case 'ย':
		return '6'
	case 'ว':
		return '7'
	case 'ห', 'ฮ', 'อ':
		return '8'
	case 'ง':
		return '9'
	default:
		return '0' // Should not happen if filtered correctly
	}
}

// CheckPhoneticMatch returns true if two strings have the same phonetic code.
func CheckPhoneticMatch(name1, name2 string) bool {
	return GetThaiPhoneticCode(name1) == GetThaiPhoneticCode(name2)
}
