package astro

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestCalculateOutfitByAgeKnownCases(t *testing.T) {
	resultA, okA := CalculateOutfitByAge("ศุกร์", 44)
	if !okA {
		t.Fatalf("expected valid result for case A")
	}
	if resultA.SriNumber != 1 || resultA.KaliNumber != 7 {
		t.Fatalf("case A mismatch sri=%d kali=%d", resultA.SriNumber, resultA.KaliNumber)
	}

	resultB, okB := CalculateOutfitByAge("อาทิตย์", 45)
	if !okB {
		t.Fatalf("expected valid result for case B")
	}
	if resultB.SriNumber != 3 || resultB.KaliNumber != 8 {
		t.Fatalf("case B mismatch sri=%d kali=%d", resultB.SriNumber, resultB.KaliNumber)
	}
}

func TestCalculateOutfitByBirthDayKnownCase(t *testing.T) {
	result, ok := CalculateOutfitByBirthDay("อาทิตย์")
	if !ok {
		t.Fatalf("expected valid result for birth day")
	}
	if result.SriNumber != 4 || result.KaliNumber != 6 {
		t.Fatalf("birth day mismatch sri=%d kali=%d", result.SriNumber, result.KaliNumber)
	}
}

func TestNormalizeOutfitDayInput(t *testing.T) {
	if NormalizeOutfitDayInput("6") != "ศุกร์" {
		t.Fatalf("expected 6 -> ศุกร์")
	}
	if NormalizeOutfitDayInput("วันอาทิตย์") != "อาทิตย์" {
		t.Fatalf("expected วันอาทิตย์ -> อาทิตย์")
	}
	if NormalizeOutfitDayInput("พุธกลางคืน") != "พุธ กลางคืน" {
		t.Fatalf("expected พุธกลางคืน normalization")
	}
}

func TestCalculateOutfitColorSetsOrderAndValues(t *testing.T) {
	response, ok := CalculateOutfitColorSets("ศุกร์", "อาทิตย์", 45)
	if !ok {
		t.Fatalf("expected valid color set response")
	}

	if len(response.AuspiciousSets) != 3 || len(response.InauspiciousSets) != 3 {
		t.Fatalf("expected 3 ordered sets each")
	}

	if response.AuspiciousSets[0].Label != "ชุดสีตามวันปัจจุบัน" || response.AuspiciousSets[0].Number != 3 {
		t.Fatalf("unexpected auspicious current-day set")
	}
	if response.AuspiciousSets[1].Label != "ชุดสีตามวันเกิด" || response.AuspiciousSets[1].Number != 4 {
		t.Fatalf("unexpected auspicious birth-day set")
	}
	if response.AuspiciousSets[2].Label != "ชุดสีตามอายุย่าง" || response.AuspiciousSets[2].Number != 3 {
		t.Fatalf("unexpected auspicious age set")
	}

	if response.InauspiciousSets[0].Label != "ชุดสีตามวันปัจจุบัน" || response.InauspiciousSets[0].Number != 8 {
		t.Fatalf("unexpected inauspicious current-day set")
	}
	if response.InauspiciousSets[1].Label != "ชุดสีตามวันเกิด" || response.InauspiciousSets[1].Number != 6 {
		t.Fatalf("unexpected inauspicious birth-day set")
	}
	if response.InauspiciousSets[2].Label != "ชุดสีตามอายุย่าง" || response.InauspiciousSets[2].Number != 8 {
		t.Fatalf("unexpected inauspicious age set")
	}
	if len(response.InauspiciousSets[0].DualNumbers) != 2 || response.InauspiciousSets[0].DualNumbers[0] != 5 || response.InauspiciousSets[0].DualNumbers[1] != 4 {
		t.Fatalf("unexpected dual inauspicious mapping for number 8")
	}
	if len(response.InauspiciousSets[1].DualDays) != 2 {
		t.Fatalf("expected dual inauspicious days for birth set")
	}
}

func TestOutfitByAgeHandler(t *testing.T) {
	req := httptest.NewRequest(http.MethodGet, "/api/v1/outfit-miracle/by-age?birth_day=ศุกร์&age=44", nil)
	rr := httptest.NewRecorder()
	OutfitByAgeHandler(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("unexpected status: %d", rr.Code)
	}

	var result OutfitResult
	if err := json.Unmarshal(rr.Body.Bytes(), &result); err != nil {
		t.Fatalf("failed to decode response: %v", err)
	}
	if result.SriNumber != 1 || result.KaliNumber != 7 {
		t.Fatalf("unexpected result sri=%d kali=%d", result.SriNumber, result.KaliNumber)
	}
}

func TestOutfitColorSetsHandler(t *testing.T) {
	body := []byte(`{"current_day":"6","birth_day":"1","age":45}`)
	req := httptest.NewRequest(http.MethodPost, "/api/v1/outfit-miracle/color-sets", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	rr := httptest.NewRecorder()
	OutfitColorSetsHandler(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("unexpected status: %d", rr.Code)
	}

	var response OutfitColorSetResponse
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Fatalf("failed to decode response: %v", err)
	}
	if len(response.AuspiciousSets) != 3 || len(response.InauspiciousSets) != 3 {
		t.Fatalf("unexpected set lengths")
	}
	if response.ReferenceSkillDoc != "/Users/tayap/project-naming/outfit-miracle/SKILL.md" {
		t.Fatalf("unexpected reference skill doc: %s", response.ReferenceSkillDoc)
	}
}

func TestOutfitColorSetsHandlerSemanticBody(t *testing.T) {
	body := []byte(`{"current_day_name":"ศุกร์","birth_day_name":"อาทิตย์","age_years":44}`)
	req := httptest.NewRequest(http.MethodPost, "/api/v1/outfit-miracle/color-sets", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	rr := httptest.NewRecorder()
	OutfitColorSetsHandler(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("unexpected status: %d", rr.Code)
	}

	var response OutfitColorSetResponse
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Fatalf("failed to decode response: %v", err)
	}
	if response.CurrentDay != "ศุกร์" || response.BirthDay != "อาทิตย์" || response.Age != 44 {
		t.Fatalf("unexpected semantic body mapping: current=%s birth=%s age=%d", response.CurrentDay, response.BirthDay, response.Age)
	}
	if response.ReferenceCodeFile != "/Users/tayap/project-naming/outfit-miracle/main.go" {
		t.Fatalf("unexpected reference code file: %s", response.ReferenceCodeFile)
	}
}
