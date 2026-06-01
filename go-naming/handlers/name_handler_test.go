package handlers

import (
	"reflect"
	"testing"
)

func TestExtractSuggestionTokens(t *testing.T) {
	tests := []struct {
		name     string
		input    []string
		expected []string
	}{
		{
			name:     "Thai continuous phrase with stop words",
			input:    []string{"ผู้มีชีวิตอันงดงามและเปี่ยมด้วยพลังแห่งความหวัง"},
			expected: []string{"ชีวิต", "งดงาม", "เปี่ยม", "พลัง", "หวัง"},
		},
		{
			name:     "Standard spaced phrase",
			input:    []string{"สมชาย", "ยิ่งใหญ่"},
			expected: []string{"สมชาย", "ยิ่งใหญ่"},
		},
		{
			name:     "Phrase with mixed spaces, stop words, and punctuation",
			input:    []string{"มีชีวิต (งดงาม) และ มีความหวัง"},
			expected: []string{"ชีวิต", "งดงาม", "หวัง"},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := extractSuggestionTokens(tt.input...)
			if !reflect.DeepEqual(got, tt.expected) {
				t.Errorf("extractSuggestionTokens(%v) = %v, want %v", tt.input, got, tt.expected)
			}
		})
	}
}

func TestDetectGenderFromText(t *testing.T) {
	tests := []struct {
		name     string
		text     string
		expected string
	}{
		{
			name:     "female phrase",
			text:     "หญิงสาวผู้อ่อนหวาน มีเสน่ห์ และเป็นที่รัก",
			expected: "female",
		},
		{
			name:     "male phrase",
			text:     "ชายหนุ่มผู้เข้มแข็ง กล้าหาญ และมีบารมี",
			expected: "male",
		},
		{
			name:     "mixed phrase is neutral",
			text:     "ลูกสาวที่เข้มแข็งและกล้าหาญ",
			expected: "neutral",
		},
		{
			name:     "no signal is neutral",
			text:     "ผู้มีปัญญาและความสุข",
			expected: "neutral",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := detectGenderFromText(tt.text); got != tt.expected {
				t.Fatalf("detectGenderFromText(%q) = %q, want %q", tt.text, got, tt.expected)
			}
		})
	}
}

func TestSuggestionGenderBonus(t *testing.T) {
	tests := []struct {
		name            string
		detectedGender  string
		rowGender       string
		hasGenderSignal bool
		expected        float64
	}{
		{
			name:            "female match",
			detectedGender:  "female",
			rowGender:       "female",
			hasGenderSignal: true,
			expected:        300,
		},
		{
			name:            "male match",
			detectedGender:  "male",
			rowGender:       "male",
			hasGenderSignal: true,
			expected:        300,
		},
		{
			name:            "female mismatch",
			detectedGender:  "female",
			rowGender:       "male",
			hasGenderSignal: true,
			expected:        0,
		},
		{
			name:            "neutral signal boosts gendered rows",
			detectedGender:  "neutral",
			rowGender:       "female",
			hasGenderSignal: true,
			expected:        100,
		},
		{
			name:            "neutral row receives no gender bonus",
			detectedGender:  "female",
			rowGender:       "neutral",
			hasGenderSignal: true,
			expected:        0,
		},
		{
			name:            "no signal receives no neutral bonus",
			detectedGender:  "neutral",
			rowGender:       "female",
			hasGenderSignal: false,
			expected:        0,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := suggestionGenderBonus(tt.detectedGender, tt.rowGender, tt.hasGenderSignal)
			if got != tt.expected {
				t.Fatalf("suggestionGenderBonus(%q, %q, %v) = %v, want %v", tt.detectedGender, tt.rowGender, tt.hasGenderSignal, got, tt.expected)
			}
		})
	}
}
