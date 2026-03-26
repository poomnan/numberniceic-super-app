package astro

import (
	"strings"
	"time"
)

var outfitTitleMiracles = []string{"บริวาร", "กาลี", "มนตรี", "อุตสาหะ", "มูละ", "ศรี", "เดช", "อายุ"}
var outfitReferenceProject = "/Users/tayap/project-naming/outfit-miracle"
var outfitReferenceSkillDoc = "/Users/tayap/project-naming/outfit-miracle/SKILL.md"
var outfitReferenceCodeFile = "/Users/tayap/project-naming/outfit-miracle/main.go"
var outfitForwardSequence = []int{1, 7, 2, 3, 4, 7, 5, 8, 6}
var outfitAgeCounterClockwiseSequence = []int{6, 8, 5, 7, 4, 3, 2, 1}
var outfitDayCounterClockwiseSequence = []int{1, 6, 8, 5, 7, 4, 3, 2}
var outfitNumberColors = map[int][]string{
	1: {"สีแดง", "สีแดงเลือดนก", "แดงอ่อน"},
	2: {"สีขาว", "สีเหลืองอ่อน", "สีเหลืองอ่อน"},
	3: {"สีชมพู", "สีชมพูอ่อน", "สีชมพูเข้ม"},
	4: {"สีเขียวเข้ม", "สีเขียวอ่อน", "สีเขียวอ่อน"},
	5: {"สีเหลืองเข้ม", "สีแสด", "สีส้มเข้ม"},
	6: {"สีฟ้า", "สีฟ้าอ่อน", "สีน้ำเงิน"},
	7: {"สีดำ", "สีม่วง", "สีน้ำตาลเข้ม"},
	8: {"สีเทา", "สีเทาอ่อน", "สีเทาอ่อนไล่ตามเฉด"},
}
var outfitDualInauspiciousByNumber = map[int][]int{
	1: {6, 3},
	2: {1, 5},
	3: {2, 1},
	4: {3, 8},
	5: {7, 2},
	6: {8, 7},
	7: {4, 6},
	8: {5, 4},
}
var outfitDayToNumber = map[string]int{
	"อาทิตย์":     1,
	"จันทร์":      2,
	"อังคาร":      3,
	"พุธ":         4,
	"พฤหัสบดี":    5,
	"ศุกร์":       6,
	"เสาร์":       7,
	"พุธ กลางคืน":   8,
	"พุธ (กลางคืน)": 8,
}
var outfitNumberToDay = map[int]string{
	1: "อาทิตย์",
	2: "จันทร์",
	3: "อังคาร",
	4: "พุธ",
	5: "พฤหัสบดี",
	6: "ศุกร์",
	7: "เสาร์",
	8: "พุธ (กลางคืน)",
}
var outfitDayNumberInputMap = map[string]string{
	"1": "อาทิตย์",
	"2": "จันทร์",
	"3": "อังคาร",
	"4": "พุธ",
	"5": "พฤหัสบดี",
	"6": "ศุกร์",
	"7": "เสาร์",
	"8": "พุธ (กลางคืน)",
}

type OutfitResult struct {
	Mode         string              `json:"mode"`
	BirthDay     string              `json:"birth_day,omitempty"`
	CurrentDay   string              `json:"current_day,omitempty"`
	Age          int                 `json:"age,omitempty"`
	StartPos     int                 `json:"start_pos"`
	Step1        int                 `json:"step1,omitempty"`
	Path         []int               `json:"path,omitempty"`
	FinalNumber  int                 `json:"final_number,omitempty"`
	SriPath      []int               `json:"sri_path"`
	SriNumber    int                 `json:"sri_number"`
	SriDay       string              `json:"sri_day"`
	KaliPath     []int               `json:"kali_path"`
	KaliNumber   int                 `json:"kali_number"`
	KaliDay      string              `json:"kali_day"`
	TitleNumbers map[string]int      `json:"title_numbers"`
	TitleDays    map[string]string   `json:"title_days"`
	TitleColors  map[string][]string `json:"title_colors"`
}

type OutfitColorSetItem struct {
	Label       string   `json:"label"`
	SourceDay   string   `json:"source_day,omitempty"`
	SourceAge   int      `json:"source_age,omitempty"`
	Number      int      `json:"number"`
	Day         string   `json:"day"`
	DualNumbers []int    `json:"dual_numbers,omitempty"`
	DualDays    []string `json:"dual_days,omitempty"`
	ColorGroup  []string `json:"color_group"`
}

type OutfitColorSetResponse struct {
	CurrentDay        string               `json:"current_day"`
	BirthDay          string               `json:"birth_day"`
	Age               int                  `json:"age"`
	ReferenceProject  string               `json:"reference_project"`
	ReferenceSkillDoc string               `json:"reference_skill_doc"`
	ReferenceCodeFile string               `json:"reference_code_file"`
	AuspiciousSets    []OutfitColorSetItem `json:"auspicious_sets"`
	InauspiciousSets  []OutfitColorSetItem `json:"inauspicious_sets"`
	CurrentDayResult  OutfitResult         `json:"current_day_result"`
	BirthDayResult    OutfitResult         `json:"birth_day_result"`
	AgeResult         OutfitResult         `json:"age_result"`
}

func NormalizeOutfitDayInput(input string) string {
	v := strings.TrimSpace(input)
	vl := strings.ToLower(v)
	englishMap := map[string]string{
		"sunday":     "อาทิตย์",
		"monday":     "จันทร์",
		"tuesday":    "อังคาร",
		"wednesday":  "พุธ",
		"thursday":   "พฤหัสบดี",
		"friday":     "ศุกร์",
		"saturday":   "เสาร์",
		"wednesday2": "พุธ กลางคืน",
	}
	if dayName, ok := englishMap[vl]; ok {
		return dayName
	}
	v = strings.TrimPrefix(v, "วัน")
	v = strings.TrimSpace(v)
	if dayName, ok := outfitDayNumberInputMap[v]; ok {
		return dayName
	}
	if v == "พุธกลางคืน" || v == "พุธ กลางคืน" {
		return "พุธ กลางคืน"
	}
	return v
}

func CurrentThaiDayName() string {
	thaiDays := []string{"อาทิตย์", "จันทร์", "อังคาร", "พุธ", "พฤหัสบดี", "ศุกร์", "เสาร์"}
	return thaiDays[int(time.Now().Weekday())]
}

func CalculateOutfitByAge(birthDay string, age int) (OutfitResult, bool) {
	normalizedDay := NormalizeOutfitDayInput(birthDay)
	startPos, ok := outfitDayToNumber[normalizedDay]
	if !ok || age <= 0 {
		return OutfitResult{}, false
	}

	step1 := outfitSingleDigit(age)
	path := outfitCountBySequence(startPos, step1, outfitForwardSequence, true, true)
	finalNumber := path[len(path)-1]
	sriPath := outfitCountBySequence(finalNumber, 6, outfitAgeCounterClockwiseSequence, true, true)
	sriNumber := sriPath[len(sriPath)-1]
	kaliPath := outfitCountBySequence(finalNumber, 1, outfitAgeCounterClockwiseSequence, false, true)
	kaliNumber := kaliPath[len(kaliPath)-1]
	titleNumbers := outfitTitleNumbersFromSri(sriNumber, outfitAgeCounterClockwiseSequence)

	return OutfitResult{
		Mode:         "age",
		BirthDay:     normalizedDay,
		Age:          age,
		StartPos:     startPos,
		Step1:        step1,
		Path:         path,
		FinalNumber:  finalNumber,
		SriPath:      sriPath,
		SriNumber:    sriNumber,
		SriDay:       outfitFindDayFromNumber(sriNumber),
		KaliPath:     kaliPath,
		KaliNumber:   kaliNumber,
		KaliDay:      outfitFindDayFromNumber(kaliNumber),
		TitleNumbers: titleNumbers,
		TitleDays:    outfitTitleDays(titleNumbers),
		TitleColors:  outfitTitleColors(titleNumbers),
	}, true
}

func CalculateOutfitByBirthDay(birthDay string) (OutfitResult, bool) {
	normalizedDay := NormalizeOutfitDayInput(birthDay)
	startPos, ok := outfitDayToNumber[normalizedDay]
	if !ok {
		return OutfitResult{}, false
	}

	sriPath := outfitCountBySequence(startPos, 6, outfitDayCounterClockwiseSequence, true, true)
	sriNumber := sriPath[len(sriPath)-1]
	kaliPath := outfitCountBySequence(startPos, 1, outfitDayCounterClockwiseSequence, false, true)
	kaliNumber := kaliPath[len(kaliPath)-1]
	titleNumbers := outfitTitleNumbersFromSri(sriNumber, outfitDayCounterClockwiseSequence)

	return OutfitResult{
		Mode:         "birth_day",
		BirthDay:     normalizedDay,
		StartPos:     startPos,
		SriPath:      sriPath,
		SriNumber:    sriNumber,
		SriDay:       outfitFindDayFromNumber(sriNumber),
		KaliPath:     kaliPath,
		KaliNumber:   kaliNumber,
		KaliDay:      outfitFindDayFromNumber(kaliNumber),
		TitleNumbers: titleNumbers,
		TitleDays:    outfitTitleDays(titleNumbers),
		TitleColors:  outfitTitleColors(titleNumbers),
	}, true
}

func CalculateOutfitByCurrentDay(day string) (OutfitResult, bool) {
	result, ok := CalculateOutfitByBirthDay(day)
	if !ok {
		return OutfitResult{}, false
	}
	result.Mode = "current_day"
	result.CurrentDay = NormalizeOutfitDayInput(day)
	return result, true
}

func CalculateOutfitColorSets(currentDay, birthDay string, age int) (OutfitColorSetResponse, bool) {
	currentResult, ok := CalculateOutfitByCurrentDay(currentDay)
	if !ok {
		return OutfitColorSetResponse{}, false
	}
	birthResult, ok := CalculateOutfitByBirthDay(birthDay)
	if !ok {
		return OutfitColorSetResponse{}, false
	}
	ageResult, ok := CalculateOutfitByAge(birthDay, age)
	if !ok {
		return OutfitColorSetResponse{}, false
	}

	auspicious := []OutfitColorSetItem{
		{
			Label:      "ชุดสีตามวันปัจจุบัน",
			SourceDay:  currentResult.CurrentDay,
			Number:     currentResult.SriNumber,
			Day:        currentResult.SriDay,
			ColorGroup: outfitColorsForNumber(currentResult.SriNumber),
		},
		{
			Label:      "ชุดสีตามวันเกิด",
			SourceDay:  birthResult.BirthDay,
			Number:     birthResult.SriNumber,
			Day:        birthResult.SriDay,
			ColorGroup: outfitColorsForNumber(birthResult.SriNumber),
		},
		{
			Label:      "ชุดสีตามอายุย่าง",
			SourceAge:  ageResult.Age,
			Number:     ageResult.SriNumber,
			Day:        ageResult.SriDay,
			ColorGroup: outfitColorsForNumber(ageResult.SriNumber),
		},
	}

	// dualInauspicious lookup ใช้ StartPos (เลขวันต้นทาง) เป็น key ตาม design ของตาราง outfitDualInauspiciousByNumber
	// ไม่ใช่ KaliNumber เพราะ key ในตารางคือเลขวันเกิด/วันต้นทาง (startPos)
	inauspicious := []OutfitColorSetItem{
		{
			Label:       "ชุดสีตามวันปัจจุบัน",
			SourceDay:   currentResult.CurrentDay,
			Number:      currentResult.KaliNumber,
			Day:         currentResult.KaliDay,
			DualNumbers: outfitDualInauspiciousNumbers(currentResult.StartPos),
			DualDays:    outfitDualInauspiciousDays(currentResult.StartPos),
			ColorGroup:  outfitColorsForNumber(currentResult.KaliNumber),
		},
		{
			Label:       "ชุดสีตามวันเกิด",
			SourceDay:   birthResult.BirthDay,
			Number:      birthResult.KaliNumber,
			Day:         birthResult.KaliDay,
			DualNumbers: outfitDualInauspiciousNumbers(birthResult.StartPos),
			DualDays:    outfitDualInauspiciousDays(birthResult.StartPos),
			ColorGroup:  outfitColorsForNumber(birthResult.KaliNumber),
		},
		{
			Label:       "ชุดสีตามอายุย่าง",
			SourceAge:   ageResult.Age,
			Number:      ageResult.KaliNumber,
			Day:         ageResult.KaliDay,
			DualNumbers: outfitDualInauspiciousNumbers(ageResult.StartPos),
			DualDays:    outfitDualInauspiciousDays(ageResult.StartPos),
			ColorGroup:  outfitColorsForNumber(ageResult.KaliNumber),
		},
	}

	return OutfitColorSetResponse{
		CurrentDay:        currentResult.CurrentDay,
		BirthDay:          birthResult.BirthDay,
		Age:               ageResult.Age,
		ReferenceProject:  outfitReferenceProject,
		ReferenceSkillDoc: outfitReferenceSkillDoc,
		ReferenceCodeFile: outfitReferenceCodeFile,
		AuspiciousSets:    auspicious,
		InauspiciousSets:  inauspicious,
		CurrentDayResult:  currentResult,
		BirthDayResult:    birthResult,
		AgeResult:         ageResult,
	}, true
}

func outfitSingleDigit(age int) int {
	sum := (age / 10) + (age % 10)
	for sum >= 10 {
		sum = (sum / 10) + (sum % 10)
	}
	return sum
}

func outfitFindDayFromNumber(number int) string {
	if day, ok := outfitNumberToDay[number]; ok {
		return day
	}
	return "ไม่พบ"
}

func outfitColorsForNumber(number int) []string {
	if colors, ok := outfitNumberColors[number]; ok {
		return colors
	}
	return []string{}
}

func outfitDualInauspiciousNumbers(number int) []int {
	if values, ok := outfitDualInauspiciousByNumber[number]; ok {
		return values
	}
	return []int{number}
}

func outfitDualInauspiciousDays(number int) []string {
	values := outfitDualInauspiciousNumbers(number)
	days := make([]string, 0, len(values))
	for _, n := range values {
		days = append(days, outfitFindDayFromNumber(n))
	}
	return days
}

func outfitCountBySequence(startPos, steps int, sequence []int, includeStart, wrap bool) []int {
	path := []int{startPos}
	if len(sequence) == 0 {
		return path
	}

	startIndex := -1
	for i, num := range sequence {
		if num == startPos {
			startIndex = i
			break
		}
	}
	if startIndex == -1 {
		return path
	}

	moves := steps
	if includeStart {
		moves = steps - 1
	}
	if moves <= 0 {
		return path
	}

	currentIndex := startIndex
	for i := 0; i < moves; i++ {
		nextIndex := currentIndex + 1
		if nextIndex >= len(sequence) {
			if !wrap {
				break
			}
			nextIndex = 0
		}
		currentIndex = nextIndex
		path = append(path, sequence[currentIndex])
	}

	return path
}

func outfitTitleNumbersFromSri(sriNumber int, sequence []int) map[string]int {
	results := make(map[string]int)
	sriTitleIndex := -1
	for i, title := range outfitTitleMiracles {
		if title == "ศรี" {
			sriTitleIndex = i
			break
		}
	}
	if sriTitleIndex == -1 {
		return results
	}

	sriSequenceIndex := -1
	for i, num := range sequence {
		if num == sriNumber {
			sriSequenceIndex = i
			break
		}
	}
	if sriSequenceIndex == -1 {
		return results
	}

	total := len(sequence)
	for i, title := range outfitTitleMiracles {
		offset := i - sriTitleIndex
		seqIndex := (sriSequenceIndex + offset + total) % total
		results[title] = sequence[seqIndex]
	}
	return results
}

func outfitTitleDays(titleNumbers map[string]int) map[string]string {
	results := make(map[string]string)
	for title, number := range titleNumbers {
		results[title] = outfitFindDayFromNumber(number)
	}
	return results
}

func outfitTitleColors(titleNumbers map[string]int) map[string][]string {
	results := make(map[string][]string)
	for title, number := range titleNumbers {
		results[title] = outfitColorsForNumber(number)
	}
	return results
}
