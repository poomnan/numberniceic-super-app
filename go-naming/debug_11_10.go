//go:build tools

package main

import (
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"os"
)

func main() {
	if os.Getenv("DATABASE_URL") == "" {
		fmt.Println("missing DATABASE_URL")
		return
	}
	database.Connect()

	meanings, _ := services.GetPairMeaningsMap()
	fmt.Printf("11: %s\n", meanings["11"])
	fmt.Printf("10: %s\n", meanings["10"])
}
