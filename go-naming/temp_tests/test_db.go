package main

import (
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"log"
)

func main() {
	database.InitDB()
	goodSums, err := services.GetGoodSums()
	if err != nil {
		log.Fatal(err)
	}
	fmt.Println("Real GoodSums:", goodSums)

	goodSumMap := make(map[int]bool)
	for _, sum := range goodSums {
		goodSumMap[sum] = true
	}

	lastnameSat := 41

	targetSatSums := []int{}

	for _, s := range goodSums {
		if s > lastnameSat {
			baseSat := s - lastnameSat
			if goodSumMap[baseSat] {
				targetSatSums = append(targetSatSums, baseSat)
			}
		}
	}
	fmt.Println("targetSatSums with FilterSat=true:", targetSatSums)
}
