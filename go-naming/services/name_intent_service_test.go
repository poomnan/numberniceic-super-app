package services

import "testing"

func TestNormalizeIntentInput(t *testing.T) {
	tests := []struct {
		name     string
		input    string
		expected string
	}{
		{
			name:     "trim and collapse spaces",
			input:    "  ณัฐ   พล  ",
			expected: "ณัฐ พล",
		},
		{
			name:     "lowercase english only",
			input:    "  NATTaPon  ",
			expected: "nattapon",
		},
		{
			name:     "keep thai intact while lowering english",
			input:    "  ณัฐ Aon   Dee ",
			expected: "ณัฐ aon dee",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := normalizeIntentInput(tt.input)
			if got != tt.expected {
				t.Fatalf("normalizeIntentInput(%q) = %q, want %q", tt.input, got, tt.expected)
			}
		})
	}
}

func TestClassifyIntentCandidates(t *testing.T) {
	tests := []struct {
		name       string
		input      string
		nameLike   bool
		candidates []fuzzyCandidate
		wantMode   string
		wantScore  float64
	}{
		{
			name:      "no results but name-like stays name",
			input:     "ทญา",
			nameLike:  true,
			wantMode:  nameIntentModeName,
			wantScore: 0.55,
		},
		{
			name:      "no results means meaning",
			input:     "ร่ำรวย บารมี",
			nameLike:  false,
			wantMode:  nameIntentModeMeaning,
			wantScore: 0,
		},
		{
			name:     "high score means name",
			input:    "ณัฐพล",
			nameLike: true,
			candidates: []fuzzyCandidate{
				{Name: "ณัฐพล", Score: 0.91},
				{Name: "ณัฐพน", Score: 0.88},
			},
			wantMode:  nameIntentModeName,
			wantScore: 0.91,
		},
		{
			name:     "mid score means hybrid",
			input:    "ณัฐพล",
			nameLike: true,
			candidates: []fuzzyCandidate{
				{Name: "ณัฐพล", Score: 0.72},
				{Name: "ณัฐพน", Score: 0.68},
			},
			wantMode:  nameIntentModeHybrid,
			wantScore: 0.72,
		},
		{
			name:     "low score means meaning",
			input:    "พลังบารมี",
			nameLike: false,
			candidates: []fuzzyCandidate{
				{Name: "พลัง", Score: 0.44},
			},
			wantMode:  nameIntentModeMeaning,
			wantScore: 0.44,
		},
		{
			name:     "low score but name-like becomes hybrid",
			input:    "ทญา",
			nameLike: true,
			candidates: []fuzzyCandidate{
				{Name: "ณัทญา", Score: 0.25},
			},
			wantMode:  nameIntentModeHybrid,
			wantScore: 0.25,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := classifyIntentCandidates(tt.input, tt.candidates, tt.nameLike)
			if got.Mode != tt.wantMode {
				t.Fatalf("classifyIntentCandidates mode = %q, want %q", got.Mode, tt.wantMode)
			}
			if got.BestScore != tt.wantScore {
				t.Fatalf("classifyIntentCandidates best score = %v, want %v", got.BestScore, tt.wantScore)
			}
		})
	}
}

func TestClampUnit(t *testing.T) {
	tests := []struct {
		input    float64
		expected float64
	}{
		{-0.2, 0},
		{0.7, 0.7},
		{1.1, 1},
	}

	for _, tt := range tests {
		got := clampUnit(tt.input)
		if got != tt.expected {
			t.Fatalf("clampUnit(%v) = %v, want %v", tt.input, got, tt.expected)
		}
	}
}

func TestLooksLikePersonName(t *testing.T) {
	tests := []struct {
		input    string
		expected bool
	}{
		{"ทญา", true},
		{"ณัฐพล", true},
		{"ร่ำรวย บารมี", false},
		{"wealthy leader", false},
		{"นา", true},
		{"name-01", false},
	}

	for _, tt := range tests {
		got := looksLikePersonName(tt.input)
		if got != tt.expected {
			t.Fatalf("looksLikePersonName(%q) = %v, want %v", tt.input, got, tt.expected)
		}
	}
}
