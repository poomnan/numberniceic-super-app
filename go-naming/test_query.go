package main

import (
	"fmt"
	"go-naming/database"
	"go-naming/services"
)

func main() {
	database.InitDB()
	goodSums, _ := services.GetGoodSums()

	goodSumMap := make(map[int]bool)
	for _, sum := range goodSums {
		goodSumMap[sum] = true
	}

	lastnameSat := 41 // อัศวโภคิน

	targetSatSums := []int{}
	for _, s := range goodSums {
		if s > lastnameSat {
			baseSat := s - lastnameSat
			if goodSumMap[baseSat] {
				targetSatSums = append(targetSatSums, baseSat)
			}
		}
	}

	is18InArray := false
	for _, v := range targetSatSums {
		if v == 18 {
			is18InArray = true
		}
	}
	fmt.Printf("Is 18 in targetSatSums? %v\n", is18InArray)
	fmt.Printf("Is 18 in goodSums? %v\n", goodSumMap[18])

	// What about 10?
	fmt.Printf("Is 10 in targetSatSums? %v\n", targetSatSums)
}
