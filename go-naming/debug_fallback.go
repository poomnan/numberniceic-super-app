//go:build tools

package main

import (
	"database/sql"
	"fmt"
	"log"

	_ "github.com/lib/pq"
)

// Helper for minimal dependencies (reproduce logic manually)

func main() {
	// 1. Connect DB
	connStr := "postgres://tayap:IntelliP24.X@localhost/tayap?sslmode=disable"
	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	if err = db.Ping(); err != nil {
		log.Fatalf("Cannot connect to DB: %v", err)
	}

	lastname := "ทองแสน"

	// Mock Decode
	// ทองแสน (Sat=24, Sha=42) - Example values from common charts
	// ท=1, อ=6, ง=1, แ=8, ส=7, น=5 -> 1+6+1+8+7+5 = 28 (Close)
	// Let's query SatNums/ShaNums from DB to be exact

	// Get SatNums
	satMap := make(map[rune]int)
	rows, _ := db.Query("SELECT char_key, sat_value FROM sat_nums")
	for rows.Next() {
		var k string
		var v int
		rows.Scan(&k, &v)
		for _, r := range k {
			satMap[r] = v
		}
	}
	rows.Close()

	// Get ShaNums
	shaMap := make(map[rune]int)
	rows, _ = db.Query("SELECT char_key, sha_value FROM sha_nums")
	for rows.Next() {
		var k string
		var v int
		rows.Scan(&k, &v)
		for _, r := range k {
			shaMap[r] = v
		}
	}
	rows.Close()

	// Calc Sums
	lSat := 0
	lSha := 0
	for _, r := range lastname {
		lSat += satMap[r]
		lSha += shaMap[r]
	}
	fmt.Printf("Lastname: %s, Sat=%d, Sha=%d\n", lastname, lSat, lSha)

	// Get Good Sums (Simulated - assume D-series)
	// Query 'numbers' table or hardcode common good sums
	// 14, 15, 24, 36, 41, 42, 45, 50, 51, 54, 55, 56, 59, 63, 64, 65...
	// Let's query 'numbers' where pairtype starts with 'D'
	rows, _ = db.Query("SELECT pairnumber FROM numbers WHERE pairtype LIKE 'D%'")
	goodPairs := make(map[string]bool)
	for rows.Next() {
		var p string
		rows.Scan(&p)
		goodPairs[p] = true
	}
	rows.Close()

	var goodSums []int
	for i := 1; i <= 200; i++ {
		s := fmt.Sprintf("%d", i)
		isGood := true
		// Pairs logic
		if i >= 100 {
			p1 := s[:2]
			p2 := s[1:]
			if !goodPairs[p1] || !goodPairs[p2] {
				isGood = false
			}
		} else if i >= 10 {
			if !goodPairs[s] {
				isGood = false
			}
		} else {
			if !goodPairs["0"+s] {
				isGood = false
			}
		}
		if isGood {
			goodSums = append(goodSums, i)
		}
	}
	fmt.Printf("Good Sums Count: %d\n", len(goodSums))

	// Calc Allowed
	var allowedSats []int
	for _, gs := range goodSums {
		if val := gs - lSat; val > 0 {
			allowedSats = append(allowedSats, val)
		}
	}
	var allowedShas []int
	for _, gs := range goodSums {
		if val := gs - lSha; val > 0 {
			allowedShas = append(allowedShas, val)
		}
	}

	fmt.Printf("Allowed Sats: %d, Allowed Shas: %d\n", len(allowedSats), len(allowedShas))

	// STRICT Query count
	var strictCount int
	// We need array string for query: "{1,2,3}"
	asArr := arrayToString(allowedSats)
	ahArr := arrayToString(allowedShas)

	err = db.QueryRow("SELECT count(*) FROM names_miracle WHERE sat_sum = ANY($1::int[]) AND sha_sum = ANY($2::int[])", asArr, ahArr).Scan(&strictCount)
	if err != nil {
		fmt.Printf("Strict Error: %v\n", err)
	} else {
		fmt.Printf("Strict Count (AND): %d\n", strictCount)
	}

	// RELAXED Query count
	var relaxCount int
	err = db.QueryRow("SELECT count(*) FROM names_miracle WHERE (sat_sum = ANY($1::int[]) OR sha_sum = ANY($2::int[]))", asArr, ahArr).Scan(&relaxCount)
	if err != nil {
		fmt.Printf("Relax Error: %v\n", err)
	} else {
		fmt.Printf("Relax Count (OR): %d\n", relaxCount)
	}
}

func arrayToString(a []int) string {
	if len(a) == 0 {
		return "{}"
	}
	s := "{"
	for i, v := range a {
		if i > 0 {
			s += ","
		}
		s += fmt.Sprintf("%d", v)
	}
	s += "}"
	return s
}
