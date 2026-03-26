package main

import (
"fmt"
)

func main() {
	lastnameSat := 41
	lastnameSha := 55
	FilterSat := true
	FilterSha := true
	
	goodSums := []int{10, 11, 14, 15, 18, 19, 20, 23, 24, 32, 36, 40, 41, 42, 44, 45, 46, 50, 51, 54, 55, 56, 59, 60, 63, 64, 65, 68, 69, 71, 74, 78, 79, 87, 89, 90, 92, 93, 95, 96, 97, 98, 99, 100, 104, 110, 114, 115, 141, 144, 145, 146, 149, 150, 151, 154, 155, 156, 159, 164}
	goodSumMap := make(map[int]bool)
	for _, sum := range goodSums {
		goodSumMap[sum] = true
	}

	targetSatSums := []int{}
	targetShaSums := []int{}

	for _, s := range goodSums {
		if s > lastnameSat {
			baseSat := s - lastnameSat
			if !FilterSat || goodSumMap[baseSat] {
				targetSatSums = append(targetSatSums, baseSat)
			}
		}
		if s > lastnameSha {
			baseSha := s - lastnameSha
			if !FilterSha || goodSumMap[baseSha] {
				targetShaSums = append(targetShaSums, baseSha)
			}
		}
	}

	fmt.Printf("targetSat: %v\n", targetSatSums)
	fmt.Printf("targetSha: %v\n", targetShaSums)
}
