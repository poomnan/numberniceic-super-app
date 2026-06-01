package main

import (
	"context"
	"fmt"
	"go-naming/database"
	"go-naming/handlers"
	"go-naming/services"
	"log"
)

func main() {
	database.InitDB()

	// Replicate key setup from mobile_search_handler
	goodSums, err := services.GetGoodSums()
	if err != nil {
		log.Fatal(err)
	}

	goodSumMap := make(map[int]bool)
	for _, sum := range goodSums {
		goodSumMap[sum] = true
	}

	// Print sum and check if 24 and 56 are in goodSums
	fmt.Printf("Is 24 in goodSums? %v\n", goodSumMap[24])
	fmt.Printf("Is 56 in goodSums? %v\n", goodSumMap[56])

	// Mock MobileNameResult for "ใบพลู"
	res := handlers.MobileNameResult{
		Name:      "ใบพลู",
		SatSum:    24,
		ShaSum:    56,
		IsSatGood: goodSumMap[24],
		IsShaGood: goodSumMap[56],
	}

	req := handlers.MobileSearchRequest{
		Keyword:     "ใบพลู",
		FilterSat:   false,
		FilterSha:   true,
		SimilarMode: false,
	}

	// Call the passesRequestedFilters via handlers reflection/test helper or just test the logic directly:
	// Wait, we can call a Go test or use the exported function if it's in the handlers package.
	// Since debug_baiphlu.go is package main, and passesRequestedFilters is unexported in handlers,
	// let's check its visibility or use a test.
	// Let's run it by calling passesRequestedFilters indirectly or by replicating its logic.
}
