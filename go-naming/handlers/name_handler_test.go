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
