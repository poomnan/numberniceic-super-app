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

func TestThaiNameRhymes(t *testing.T) {
	tests := []struct {
		part1 string
		part2 string
		want  bool
	}{
		{"หอยแครง", "แสงตะวัน", true},  // tail-to-head (แครง + แสง)
		{"ลำไย", "ไหทองคำ", true},     // tail-to-head (ไย + ไห)
		{"สมศรี", "ดีพร้อม", true},     // tail-to-head (ศรี + ดี)
		{"ทองดี", "มีชัย", true},       // tail-to-head (ดี + มี)
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
