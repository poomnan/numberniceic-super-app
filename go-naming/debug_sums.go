//go:build tools

package main

import (
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"log"
)

func main() {
	// Connect to DB
	dsn := "postgres://tayap:IntelliP24.X@43.228.85.200:5432/tayap?sslmode=disable"
	err := database.Connect(dsn)
	if err != nil {
		log.Fatal(err)
	}

	sums, err := services.GetGoodSums()
	if err != nil {
		log.Fatal(err)
	}

	found60 := false
	for _, s := range sums {
		if s == 60 {
			found60 = true
		}
	}
	fmt.Printf("Is 60 in GoodSums? %v\n", found60)

	// Check specific pairing logic
	meanings, _ := services.GetPairMeaningsMap()
	fmt.Printf("Meaning of 60: %s\n", meanings["60"])
}
