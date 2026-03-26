package astro

import (
	"fmt"
	"time"
)

// CalculateKalagniDay calculates inauspicious day based on birth day
func CalculateKalagniDay(birthDay string) (string, int, error) {
	matrix := InitializeMatrix()

	// Map วันเกิดไปยังตำแหน่งเริ่มต้น
	startPositions := map[string]int{
		"อาทิตย์":     1,
		"จันทร์":      2,
		"อังคาร":      3,
		"พุธ":         4,
		"พฤหัสบดี":    5,
		"ศุกร์":       6,
		"เสาร์":       7,
		"พุธ กลางคืน": 8,
		"กลาง":        9,
	}

	startPos, exists := startPositions[birthDay]
	if !exists {
		return "", 0, fmt.Errorf("ไม่พบวันเกิดที่ระบุ: %s", birthDay)
	}

	// CCW 1 step = reverse ของ clockwise sequence
	// Clockwise: A1(1)→B2(9)→B1(2)→C1(3)→C2(4)→C3(7)→B3(5)→A3(8)→A2(6)→A1(1)
	// CCW (reverse):  A2(6)→A3(8)→B3(5)→C3(7)→C2(4)→C1(3)→B1(2)→B2(9)→A1(1)→A2(6)
	kalagniMap := map[int]int{
		1: 6, // A1(อาทิตย์) → A2(ศุกร์)
		9: 1, // B2(กลาง)    → A1(อาทิตย์)
		2: 9, // B1(จันทร์)  → B2(กลาง)
		3: 2, // C1(อังคาร)  → B1(จันทร์)
		4: 3, // C2(พุธ)     → C1(อังคาร)
		7: 4, // C3(เสาร์)   → C2(พุธ)
		5: 7, // B3(พฤหัสบดี)→ C3(เสาร์)  ← แก้จาก 5→4 เป็น 5→7
		8: 5, // A3(พุธกลางคืน)→ B3(พฤหัสบดี)  ← แก้จาก 8→7 เป็น 8→5
		6: 8, // A2(ศุกร์)   → A3(พุธกลางคืน)
	}

	kalagniPosition := kalagniMap[startPos]

	// หาชื่อวันจาก position
	for i := 0; i < 3; i++ {
		for j := 0; j < 3; j++ {
			if matrix[i][j].Position == kalagniPosition {
				return matrix[i][j].Day, kalagniPosition, nil
			}
		}
	}

	return "", 0, fmt.Errorf("ไม่พบวันกาลกิณี")
}

// CalculateKalagniByAge calculates inauspicious day based on birth day and age
func CalculateKalagniByAge(birthDay string, age int) (string, int, error) {
	matrix := InitializeMatrix()
	
	// คำนวณเลขตัวเดียวจากอายุ
	singleDigit := calculateSingleDigit(age)

	// Map วันเกิดไปยังตำแหน่งเริ่มต้น
	startPositions := map[string]int{
		"อาทิตย์":     1,
		"จันทร์":      2,
		"อังคาร":      3,
		"พุธ":         4,
		"พฤหัสบดี":    5,
		"ศุกร์":       6,
		"เสาร์":       7,
		"พุธ กลางคืน": 8,
		"กลาง":        9,
	}

	startPos, exists := startPositions[birthDay]
	if !exists {
		return "", 0, fmt.Errorf("ไม่พบวันเกิดที่ระบุ: %s", birthDay)
	}

	// Pattern การนับตามเข็มนาฬิกา (ครบ 9 ช่อง, A2→A1 ปิดวงจร)
	// A1(1)→B2(9)→B1(2)→C1(3)→C2(4)→C3(7)→B3(5)→A3(8)→A2(6)→A1(1)→...
	clockwiseMap := map[int]int{
		1: 9, // A1 → B2
		9: 2, // B2 → B1
		2: 3, // B1 → C1
		3: 4, // C1 → C2
		4: 7, // C2 → C3
		7: 5, // C3 → B3
		5: 8, // B3 → A3
		8: 6, // A3 → A2
		6: 1, // A2 → A1  ← ปิดวงจร (ช่องนี้หายไปทำให้ loop หยุดก่อน)
	}

	// นับตามเข็มนาฬิกาจำนวน singleDigit steps (ไม่นับจุดเริ่มต้น)
	currentPos := startPos
	for i := 0; i < singleDigit; i++ {
		if nextPos, exists := clockwiseMap[currentPos]; exists {
			currentPos = nextPos
		} else {
			break
		}
	}

	// ย้อนกลับ 1 ช่อง (ทวนเข็ม) = หา predecessor ใน clockwiseMap
	// reverseMap[v] = k หมายถึง "ก่อน v คือ k"
	reverseMap := map[int]int{
		9: 1, // ก่อน B2 คือ A1
		2: 9, // ก่อน B1 คือ B2
		3: 2, // ก่อน C1 คือ B1
		4: 3, // ก่อน C2 คือ C1
		7: 4, // ก่อน C3 คือ C2
		5: 7, // ก่อน B3 คือ C3
		8: 5, // ก่อน A3 คือ B3
		6: 8, // ก่อน A2 คือ A3
		1: 6, // ก่อน A1 คือ A2
	}
	finalPos := reverseMap[currentPos]

	// หาชื่อวันจาก position
	for i := 0; i < 3; i++ {
		for j := 0; j < 3; j++ {
			if matrix[i][j].Position == finalPos {
				return matrix[i][j].Day, finalPos, nil
			}
		}
	}

	return "", 0, fmt.Errorf("ไม่พบวันกาลกิณี")
}

// calculateSingleDigit reduces number to single digit by summing digits
func calculateSingleDigit(num int) int {
	for num > 9 {
		sum := 0
		n := num
		for n > 0 {
			sum += n % 10
			n = n / 10
		}
		num = sum
	}
	return num
}

// GetCurrentThaiDay returns current day in Thai
func GetCurrentThaiDay() string {
	now := time.Now()
	weekday := now.Weekday()

	dayMap := map[time.Weekday]string{
		time.Sunday:    "อาทิตย์",
		time.Monday:    "จันทร์",
		time.Tuesday:   "อังคาร",
		time.Wednesday: "พุธ",
		time.Thursday:  "พฤหัสบดี",
		time.Friday:    "ศุกร์",
		time.Saturday:  "เสาร์",
	}

	return dayMap[weekday]
}

// GetThaiBuddhistYear returns current Thai Buddhist year
func GetThaiBuddhistYear() int {
	return time.Now().Year() + 543
}

// GetLunarPhase calculates lunar phase
func GetLunarPhase(day int) string {
	if day <= 7 {
		return "ข้างขึ้น"
	} else if day <= 15 {
		return "วันเพ็ญ"
	} else if day <= 22 {
		return "ข้างแรม"
	} else {
		return "วันดับ"
	}
}

// IsBuddhistHolyDay checks for Buddhist holy days
func IsBuddhistHolyDay(weekday time.Weekday, day int) string {
	holyDays := []int{8, 15, 22, 29}
	for _, d := range holyDays {
		if day == d {
			return "วันพระ"
		}
	}

	kohnDays := []int{7, 14, 21, 28}
	for _, d := range kohnDays {
		if day == d {
			return "วันโกน"
		}
	}

	return ""
}

// GetSittiChokDaysForDate calculates Sitti Chok days for given date
func GetSittiChokDaysForDate(date time.Time) (string, string, string) {
	day := date.Day()

	// วันสิทธิโชค (วันที่ 4, 8, 15, 23 ของทุกเดือน)
	sittiChokDays := []int{4, 8, 15, 23}
	for _, d := range sittiChokDays {
		if day == d {
			return "สิทธิโชค", "วันมงคลสำหรับการเริ่มต้นใหม่", "🌸"
		}
	}

	// วันมหาสิทธิโชค (วันที่ 9, 18, 27)
	mahaSittiChokDays := []int{9, 18, 27}
	for _, d := range mahaSittiChokDays {
		if day == d {
			return "มหาสิทธิโชค", "วันมงคลระดับสูง เหมาะสำหรับงานใหญ่", "🌺"
		}
	}

	// วันชัยโชค (วันที่ 1, 11, 21, 31)
	chaiChokDays := []int{1, 11, 21, 31}
	for _, d := range chaiChokDays {
		if day == d {
			return "ชัยโชค", "วันแห่งชัยชนะและความสำเร็จ", "🏆"
		}
	}

	return "", "", ""
}

// GetSittiChokDays returns all Sitti Chok days for a specific year
func GetSittiChokDays(year int) []map[string]string {
	var results []map[string]string

	for month := 1; month <= 12; month++ {
		firstOfMonth := time.Date(year, time.Month(month), 1, 0, 0, 0, 0, time.Local)
		lastOfMonth := firstOfMonth.AddDate(0, 1, -1)
		daysInMonth := lastOfMonth.Day()

		for day := 1; day <= daysInMonth; day++ {
			currentDate := time.Date(year, time.Month(month), day, 0, 0, 0, 0, time.Local)
			chokType, description, emoji := GetSittiChokDaysForDate(currentDate)

			if chokType != "" {
				results = append(results, map[string]string{
					"date":        currentDate.Format("2006-01-02"),
					"thai_date":   currentDate.Format("02/01/2006"),
					"type":        chokType,
					"description": description,
					"emoji":       emoji,
				})
			}
		}
	}

	return results
}