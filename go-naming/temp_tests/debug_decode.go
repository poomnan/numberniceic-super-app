//go:build tools

package main

import (
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"log"
	"os"
)

func main() {
	if os.Getenv("DATABASE_URL") == "" {
		log.Fatal("missing DATABASE_URL")
	}
	database.Connect()

	res, err := services.DecodeName("ลำไย", "")
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("Name: %s\n", res.Name)
	fmt.Printf("TotalSat: %d\n", res.TotalSat)
	fmt.Printf("TotalSha: %d\n", res.TotalSha)
	for _, cv := range res.Characters {
		fmt.Printf("  '%s': Sat=%d, Sha=%d\n", cv.Char, cv.SatValue, cv.ShaValue)
	}
}
