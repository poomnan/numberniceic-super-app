package astro

import (
	"fmt"
	"time"
)

// NewMatrix creates a new 3x3 matrix
func NewMatrix(rows, cols int) Matrix {
	matrix := make(Matrix, rows)
	for i := range matrix {
		matrix[i] = make([]Cell, cols)
	}
	return matrix
}

// InitializeMatrix initializes the matrix with Thai astrological data
func InitializeMatrix() Matrix {
	matrix := NewMatrix(3, 3)
	
	matrix[0][0] = Cell{Position: 1, Day: "อาทิตย์"} // A1
	matrix[0][1] = Cell{Position: 2, Day: "จันทร์"}  // B1
	matrix[0][2] = Cell{Position: 3, Day: "อังคาร"}  // C1

	matrix[1][0] = Cell{Position: 6, Day: "ศุกร์"}     // A2
	matrix[1][1] = Cell{Position: 9, Day: "ไม่นับวัน"} // B2
	matrix[1][2] = Cell{Position: 4, Day: "พุธ"}       // C2

	matrix[2][0] = Cell{Position: 8, Day: "พุธ กลางคืน"} // A3
	matrix[2][1] = Cell{Position: 5, Day: "พฤหัสบดี"}    // B3
	matrix[2][2] = Cell{Position: 7, Day: "เสาร์"}       // C3

	return matrix
}

// GetThaiWeekdayName returns Thai name for weekday
func GetThaiWeekdayName(weekday time.Weekday) string {
	thaiDays := map[time.Weekday]string{
		time.Sunday:    "วันอาทิตย์",
		time.Monday:    "วันจันทร์",
		time.Tuesday:   "วันอังคาร",
		time.Wednesday: "วันพุธ",
		time.Thursday:  "วันพฤหัสบดี",
		time.Friday:    "วันศุกร์",
		time.Saturday:  "วันเสาร์",
	}
	return thaiDays[weekday]
}

// IsFooDay checks if a date is a Foo day according to Thai astrology
func IsFooDay(date time.Time) (bool, string) {
	// คำนวณเดือนจันทรคติ (แบบง่าย: ใช้เดือนสุริยคติ + 3)
	lunarMonth := (int(date.Month()) + 3) % 12
	if lunarMonth == 0 {
		lunarMonth = 12
	}

	weekday := date.Weekday()

	// ตารางวันฟูตามเดือนจันทรคติ
	fooDays := map[int]time.Weekday{
		5:  time.Sunday,    // เดือน 5, 8, 11: อาทิตย์
		8:  time.Sunday,
		11: time.Sunday,
		6:  time.Monday,    // เดือน 6, 9, 12: จันทร์
		9:  time.Monday,
		12: time.Monday,
		7:  time.Tuesday,   // เดือน 7, 10, 1: อังคาร
		10: time.Tuesday,
		1:  time.Tuesday,
		2:  time.Wednesday, // เดือน 2: พุธ
		3:  time.Thursday,  // เดือน 3: พฤหัสบดี
		4:  time.Friday,    // เดือน 4: ศุกร์/เสาร์
	}

	// ตรวจสอบว่าเป็นวันฟู
	if expectedWeekday, exists := fooDays[lunarMonth]; exists {
		if lunarMonth == 4 {
			// เดือน 4 เป็นได้ทั้งศุกร์และเสาร์
			if weekday == time.Friday || weekday == time.Saturday {
				return true, fmt.Sprintf("🌸 วันฟู - %s: วันมงคลสูงสุด เหมาะสำหรับเริ่มต้นใหม่", GetThaiWeekdayName(weekday))
			}
		} else if weekday == expectedWeekday {
			return true, fmt.Sprintf("🌸 วันฟู - %s: วันมงคลสูงสุด เหมาะสำหรับเริ่มต้นใหม่", GetThaiWeekdayName(weekday))
		}
	}

	return false, ""
}

// GetFooDaysForYear returns all Foo days for a specific year
func GetFooDaysForYear(year int) []FooDay {
	var fooDays []FooDay

	for month := 1; month <= 12; month++ {
		firstOfMonth := time.Date(year, time.Month(month), 1, 0, 0, 0, 0, time.Local)
		lastOfMonth := firstOfMonth.AddDate(0, 1, -1)
		daysInMonth := lastOfMonth.Day()

		for day := 1; day <= daysInMonth; day++ {
			currentDate := time.Date(year, time.Month(month), day, 0, 0, 0, 0, time.Local)
			isFoo, fooInfo := IsFooDay(currentDate)

			if isFoo {
				thaiDate := currentDate.Format("02/01/2006")
				fooDays = append(fooDays, FooDay{
					Date:        currentDate.Format("2006-01-02"),
					ThaiDate:    thaiDate,
					Description: fooInfo,
					IsFoo:       true,
				})
			}
		}
	}

	return fooDays
}

// GetKalaYokDays returns days for Ubath and Lokawinat for a specific year
func GetKalaYokDays(year int, category string) []string {
	results := []string{}
	
	// Chula Sakarat (CS) = Thai Year - 1181
	thaiYear := year + 543
	cs := thaiYear - 1181
	
	var targetWeekday time.Weekday
	if category == "ubath" {
		// สำหรับปี 2568 (CS 1387) วันอุบาทว์คือวันจันทร์
		// สำหรับปี 2569 (CS 1388) วันอุบาทว์คือวันเสาร์
		if cs == 1387 { targetWeekday = time.Monday } else if cs == 1388 { targetWeekday = time.Saturday } else { targetWeekday = time.Monday }
	} else if category == "lokawinat" {
		// สำหรับปี 2568 (CS 1387) วันโลกาวินาถคือวันเสาร์
		// สำหรับปี 2569 (CS 1388) วันโลกาวินาถคือวันศุกร์
		if cs == 1387 { targetWeekday = time.Saturday } else if cs == 1388 { targetWeekday = time.Friday } else { targetWeekday = time.Saturday }
	} else {
		return results
	}

	fmt.Printf("DEBUG: GetKalaYokDays category=%s year=%d thaiYear=%d cs=%d targetWeekday=%v\n", category, year, thaiYear, cs, targetWeekday)

	for month := 1; month <= 12; month++ {
		firstOfMonth := time.Date(year, time.Month(month), 1, 0, 0, 0, 0, time.Local)
		lastOfMonth := firstOfMonth.AddDate(0, 1, -1)
		daysInMonth := lastOfMonth.Day()

		for day := 1; day <= daysInMonth; day++ {
			currentDate := time.Date(year, time.Month(month), day, 0, 0, 0, 0, time.Local)
			if currentDate.Weekday() == targetWeekday {
				results = append(results, currentDate.Format("2006-01-02"))
			}
		}
	}
	return results
}