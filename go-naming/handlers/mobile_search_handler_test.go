package handlers

import (
	"context"
	"net/http"
	"testing"
	"time"
)

func TestPassesRequestedFiltersSingleToggleExcludesDoubleGood(t *testing.T) {
	doubleGood := MobileNameResult{
		Name:      "ทดสอบ",
		IsSatGood: true,
		IsShaGood: true,
	}

	if passesRequestedFilters(doubleGood, MobileSearchRequest{FilterSat: true}) {
		t.Fatal("sat-only filter should NOT include double-good names")
	}

	if passesRequestedFilters(doubleGood, MobileSearchRequest{FilterSha: true}) {
		t.Fatal("sha-only filter should NOT include double-good names")
	}
}

func TestPassesRequestedFiltersSingleToggleRejectsOnlyInactivePass(t *testing.T) {
	shaOnly := MobileNameResult{
		Name:      "ทดสอบ",
		IsSatGood: false,
		IsShaGood: true,
	}
	if passesRequestedFilters(shaOnly, MobileSearchRequest{FilterSat: true}) {
		t.Fatal("sat-only filter should reject names that pass only sha")
	}

	satOnly := MobileNameResult{
		Name:      "ทดสอบ",
		IsSatGood: true,
		IsShaGood: false,
	}
	if passesRequestedFilters(satOnly, MobileSearchRequest{FilterSha: true}) {
		t.Fatal("sha-only filter should reject names that pass only sat")
	}
}

func TestMobileSearchFlightSharesLeaderResponse(t *testing.T) {
	key := "flight-test-shared-response"
	call, isLeader := beginMobileSearchFlight(key)
	if !isLeader {
		t.Fatal("first caller should become flight leader")
	}

	type flightResult struct {
		resp   MobileSearchResponse
		status int
		ok     bool
	}
	done := make(chan flightResult, 1)
	go func() {
		resp, status, ok := waitMobileSearchFlight(context.Background(), call)
		done <- flightResult{resp: resp, status: status, ok: ok}
	}()

	expected := MobileSearchResponse{
		Success: true,
		Results: []MobileNameResult{{Name: "ทดสอบ"}},
		Total:   1,
	}
	finishMobileSearchFlight(key, call, http.StatusOK, expected, true)

	select {
	case got := <-done:
		if !got.ok {
			t.Fatal("shared flight waiter should receive leader response")
		}
		if got.status != http.StatusOK {
			t.Fatalf("status = %d, want %d", got.status, http.StatusOK)
		}
		if got.resp.Total != 1 || len(got.resp.Results) != 1 || got.resp.Results[0].Name != "ทดสอบ" {
			t.Fatalf("shared response = %+v, want one ทดสอบ result", got.resp)
		}
	case <-time.After(time.Second):
		t.Fatal("waiter did not receive shared flight response")
	}

	mobileSearchInflight.Lock()
	_, exists := mobileSearchInflight.calls[key]
	mobileSearchInflight.Unlock()
	if exists {
		t.Fatal("finished flight should be removed from in-flight map")
	}
}

func TestRankingPrefersUsableShorterNameWhenSignalsAreComparable(t *testing.T) {
	shorter := MobileNameResult{
		Name:          "มีสมวงษ์",
		IsSatGood:     true,
		IsShaGood:     true,
		SatPairType:   "D10",
		ShaPairType:   "D10",
		SatPairPoint:  70,
		ShaPairPoint:  70,
		SemanticScore: 0.82,
		RootScore:     0.62,
	}
	longPhrase := MobileNameResult{
		Name:          "โชติปัญญาชีพ",
		IsSatGood:     true,
		IsShaGood:     true,
		SatPairType:   "D10",
		ShaPairType:   "D10",
		SatPairPoint:  70,
		ShaPairPoint:  70,
		SemanticScore: 0.84,
		RootScore:     0.62,
	}

	shortScore, _ := calculateFinalRankScoreAndReasons(&shorter, "ปราชญ์ผู้มีสติปัญญาเฉลียวฉลาด และอายุยืน", false, true, true, false, false)
	longScore, _ := calculateFinalRankScoreAndReasons(&longPhrase, "ปราชญ์ผู้มีสติปัญญาเฉลียวฉลาด และอายุยืน", false, true, true, false, false)

	if shortScore <= longScore {
		t.Fatalf("shorter usable name score = %d, long phrase-like name score = %d; want shorter to rank higher", shortScore, longScore)
	}
}

func TestNameUsabilityPenaltyIncreasesForLongPhraseLikeNames(t *testing.T) {
	shortPenalty := nameUsabilityPenalty("มีสมวงษ์")
	longPenalty := nameUsabilityPenalty("โชติปัญญาชีพ")

	if longPenalty <= shortPenalty {
		t.Fatalf("long penalty = %.1f, short penalty = %.1f; want long phrase-like name penalized more", longPenalty, shortPenalty)
	}
}

func TestMicroTiebreakerDeterministicAndUnique(t *testing.T) {
	name1 := MobileNameResult{
		Name:          "ใจตา",
		IsSatGood:     true,
		IsShaGood:     true,
		SatPairType:   "D10",
		ShaPairType:   "D10",
		SatPairPoint:  70,
		ShaPairPoint:  70,
		SemanticScore: 0.80,
		RootScore:     0.50,
	}
	name2 := MobileNameResult{
		Name:          "ใหคำ",
		IsSatGood:     true,
		IsShaGood:     true,
		SatPairType:   "D10",
		ShaPairType:   "D10",
		SatPairPoint:  70,
		ShaPairPoint:  70,
		SemanticScore: 0.80,
		RootScore:     0.50,
	}

	score1, _ := calculateFinalRankScoreAndReasons(&name1, "คำค้นหา", false, true, true, false, false)
	score2, _ := calculateFinalRankScoreAndReasons(&name2, "คำค้นหา", false, true, true, false, false)

	// They should have the same integer score
	if score1 != score2 {
		t.Errorf("expected same integer score for both, got %d and %d", score1, score2)
	}

	// Their exact scores must differ (micro-tiebreaker)
	if name1.FinalRankScoreExact == name2.FinalRankScoreExact {
		t.Errorf("expected different FinalRankScoreExact, but both got %f", name1.FinalRankScoreExact)
	}

	// Test determinism
	name1Copy := name1
	_, _ = calculateFinalRankScoreAndReasons(&name1Copy, "คำค้นหา", false, true, true, false, false)
	if name1Copy.FinalRankScoreExact != name1.FinalRankScoreExact {
		t.Errorf("micro-tiebreaker is not deterministic: %f vs %f", name1Copy.FinalRankScoreExact, name1.FinalRankScoreExact)
	}
}

func TestEnforceStrictlyDecreasingScores(t *testing.T) {
	results := []MobileNameResult{
		{Name: "Name1", FinalRankScoreExact: 100.0},
		{Name: "Name2", FinalRankScoreExact: 100.0},
		{Name: "Name3", FinalRankScoreExact: 100.0},
		{Name: "Name4", FinalRankScoreExact: 99.99},
		{Name: "Name5", FinalRankScoreExact: 95.0},
		{Name: "Name6", FinalRankScoreExact: 95.0},
		{Name: "Name7", FinalRankScoreExact: 50.0},
		{Name: "Name8", FinalRankScoreExact: 0.0},
		{Name: "Name9", FinalRankScoreExact: 0.0},
	}

	enforceStrictlyDecreasingScores(results)

	expected := []float64{
		100.0,
		99.99,
		99.98,
		99.97,
		95.0,
		94.99,
		50.0,
		0.0,
		0.0,
	}

	for i, r := range results {
		if r.FinalRankScoreExact != expected[i] {
			t.Errorf("at index %d: expected %f, got %f", i, expected[i], r.FinalRankScoreExact)
		}
	}
}

func TestCompareVisibleFinalRankScoreDescUsesExactScore(t *testing.T) {
	hundred := MobileNameResult{Name: "ปราช", FinalRankScore: 100, FinalRankScoreExact: 100.00}
	ninetyNine := MobileNameResult{Name: "ปราชญา", FinalRankScore: 100, FinalRankScoreExact: 99.99}

	if got := compareVisibleFinalRankScoreDesc(hundred, ninetyNine); got >= 0 {
		t.Fatalf("expected 100.00 exact score to sort before 99.99, got compare=%d", got)
	}
	if got := compareVisibleFinalRankScoreDesc(ninetyNine, hundred); got <= 0 {
		t.Fatalf("expected 99.99 exact score to sort after 100.00, got compare=%d", got)
	}
}

func TestRankingRespectsActiveToggles(t *testing.T) {
	name := MobileNameResult{
		Name:         "สมชาย",
		Meaning:      "ผู้ชายที่เป็นสุข",
		IsSatGood:    true,
		IsShaGood:    true,
		SatPairType:  "D10",
		ShaPairType:  "D10",
		SatPairPoint: 80,
		ShaPairPoint: 80,
	}

	// Case 1: Both filters active
	nameBoth := name
	_, _ = calculateFinalRankScoreAndReasons(&nameBoth, "สมชาย", false, true, true, false, false)

	// Case 2: Only Sat filter active
	nameSatOnly := name
	_, _ = calculateFinalRankScoreAndReasons(&nameSatOnly, "สมชาย", false, true, false, false, false)

	if nameBoth.ShaBonus != 20 {
		t.Errorf("expected ShaBonus to be 20 when filterSha is active, got %d", nameBoth.ShaBonus)
	}
	if nameBoth.DoubleBonus != 50 {
		t.Errorf("expected DoubleBonus to be 50 when both filters are active, got %d", nameBoth.DoubleBonus)
	}

	if nameSatOnly.ShaBonus != 0 {
		t.Errorf("expected ShaBonus to be 0 when filterSha is inactive, got %d", nameSatOnly.ShaBonus)
	}
	if nameSatOnly.DoubleBonus != 0 {
		t.Errorf("expected DoubleBonus to be 0 when only filterSat is active, got %d", nameSatOnly.DoubleBonus)
	}

	if nameSatOnly.NumerologyScore >= nameBoth.NumerologyScore {
		t.Errorf("expected NumerologyScore with single filter (%d) to be strictly lower than both filters (%d)", nameSatOnly.NumerologyScore, nameBoth.NumerologyScore)
	}
	if nameSatOnly.NumerologyScore != 79 {
		t.Errorf("expected NumerologyScore for single filter to be 79, got %d", nameSatOnly.NumerologyScore)
	}
	if nameBoth.NumerologyScore != 100 {
		t.Errorf("expected NumerologyScore for both filters to be 100, got %d", nameBoth.NumerologyScore)
	}
}
