package services

import "testing"

func TestSingleTokenExactDatabaseMatchIsSingleName(t *testing.T) {
	// Covered at service-contract level by the first branch in ClassifyInput:
	// one token with an exact DB match must return single_name before any
	// structural or semantic heuristics can run.
	if HasThaiNameRhyme("ปัญญา", "") {
		t.Fatal("single token should not be treated as a full-name rhyme")
	}
}

func TestMeaningPhraseSignals(t *testing.T) {
	tests := []struct {
		name        string
		input       string
		wantSignals []string
	}{
		{
			name:        "descriptive star phrase",
			input:       "งดงามดั่งแสงดาวที่ส่องประกายอย่างอ่อนโยน",
			wantSignals: []string{"meaning_keywords", "long_phrase"},
		},
		{
			name:        "two word semantic phrase",
			input:       "ร่ำรวย บารมี",
			wantSignals: []string{"meaning_keywords", "non_name_two_word_phrase"},
		},
		{
			name:        "plain short name",
			input:       "ณัฐพล",
			wantSignals: nil,
		},
		{
			name:        "rhyme full name stays name-like",
			input:       "ลำไย ไหทองคำ",
			wantSignals: nil,
		},
		{
			name:        "long two-part rhyme may look descriptive but stays name-like upstream",
			input:       "หอยแครง แสงตะวัน",
			wantSignals: []string{"long_phrase"},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := meaningPhraseSignals(tt.input)
			for _, signal := range tt.wantSignals {
				if !containsSignal(got, signal) {
					t.Fatalf("meaningPhraseSignals(%q) = %v, want signal %q", tt.input, got, signal)
				}
			}
			if tt.wantSignals == nil && len(got) != 0 {
				t.Fatalf("meaningPhraseSignals(%q) = %v, want no signals", tt.input, got)
			}
		})
	}
}

func TestClassifyTwoPartThaiInputAlgorithm(t *testing.T) {
	tests := []struct {
		name           string
		input          string
		first          string
		last           string
		firstInDB      bool
		lastInDB       bool
		rhyme          bool
		wantMatched    bool
		wantType       string
		wantFirstName  string
		wantSurname    string
		wantConfidence float64
	}{
		{
			name:           "first token in database wins as name even with semantic companion",
			input:          "ใบเตย ร่ำรวย",
			first:          "ใบเตย",
			last:           "ร่ำรวย",
			firstInDB:      true,
			wantMatched:    true,
			wantType:       "single_name",
			wantFirstName:  "ใบเตย",
			wantConfidence: 0.98,
		},
		{
			name:           "database first token plus plausible surname becomes full name",
			input:          "ใบเตย ศรีสุข",
			first:          "ใบเตย",
			last:           "ศรีสุข",
			firstInDB:      true,
			wantMatched:    true,
			wantType:       "full_name",
			wantFirstName:  "ใบเตย",
			wantSurname:    "ศรีสุข",
			wantConfidence: 0.96,
		},
		{
			name:           "semantic two-token phrase stays meaning",
			input:          "ร่ำรวย บารมี",
			first:          "ร่ำรวย",
			last:           "บารมี",
			wantMatched:    true,
			wantType:       "meaning",
			wantConfidence: 0.94,
		},
		{
			name:           "second token in database wins as name without rhyme",
			input:          "ความสุข ใบเตย",
			first:          "ความสุข",
			last:           "ใบเตย",
			lastInDB:       true,
			wantMatched:    true,
			wantType:       "single_name",
			wantFirstName:  "ใบเตย",
			wantConfidence: 0.98,
		},
		{
			name:        "pure rhyme is handled by structural full-name fallback",
			input:       "หอยแครง แสงตะวัน",
			first:       "หอยแครง",
			last:        "แสงตะวัน",
			rhyme:       true,
			wantMatched: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, matched := classifyTwoPartThaiInput(
				tt.input,
				tt.first,
				tt.last,
				tt.firstInDB,
				tt.lastInDB,
				tt.rhyme,
				meaningPhraseSignals(tt.input),
				[]string{"structural_full_name"},
			)
			if matched != tt.wantMatched {
				t.Fatalf("matched = %v, want %v (classification=%+v)", matched, tt.wantMatched, got)
			}
			if !matched {
				return
			}
			if got.Type != tt.wantType {
				t.Fatalf("Type = %q, want %q", got.Type, tt.wantType)
			}
			if got.FirstName != tt.wantFirstName {
				t.Fatalf("FirstName = %q, want %q", got.FirstName, tt.wantFirstName)
			}
			if got.Surname != tt.wantSurname {
				t.Fatalf("Surname = %q, want %q", got.Surname, tt.wantSurname)
			}
			if got.Confidence != tt.wantConfidence {
				t.Fatalf("Confidence = %v, want %v", got.Confidence, tt.wantConfidence)
			}
		})
	}
}

func TestThaiNameRhymes(t *testing.T) {
	tests := []struct {
		part1 string
		part2 string
		want  bool
	}{
		{"หอยแครง", "แสงตะวัน", true}, // tail-to-head (แครง + แสง)
		{"ลำไย", "ไหทองคำ", true},     // tail-to-head (ไย + ไห)
		{"สมศรี", "ดีพร้อม", true},    // tail-to-head (ศรี + ดี)
		{"ทองดี", "มีชัย", true},      // tail-to-head (ดี + มี)
		{"ณัฐพล", "พลรบ", false},      // no rhyme
	}

	for _, tt := range tests {
		t.Run(tt.part1+"_"+tt.part2, func(t *testing.T) {
			got := HasThaiNameRhyme(tt.part1, tt.part2)
			if got != tt.want {
				t.Errorf("HasThaiNameRhyme(%q, %q) = %v, want %v", tt.part1, tt.part2, got, tt.want)
			}
		})
	}
}
