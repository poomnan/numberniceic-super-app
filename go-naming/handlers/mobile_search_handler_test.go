package handlers

import (
	"context"
	"net/http"
	"testing"
	"time"
)

func TestPassesRequestedFiltersSingleToggleIncludesDoubleGood(t *testing.T) {
	doubleGood := MobileNameResult{
		Name:      "ทดสอบ",
		IsSatGood: true,
		IsShaGood: true,
	}

	if !passesRequestedFilters(doubleGood, MobileSearchRequest{FilterSat: true}) {
		t.Fatal("sat-only filter should include names that also pass sha")
	}

	if !passesRequestedFilters(doubleGood, MobileSearchRequest{FilterSha: true}) {
		t.Fatal("sha-only filter should include names that also pass sat")
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
