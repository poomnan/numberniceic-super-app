//go:build tools

package main

import (
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"os"
)

func main() {
	os.Setenv("DATABASE_URL", "postgres://tayap:IntelliP24.X@43.228.85.200:5432/tayap?sslmode=disable")
	database.Connect()

	meanings, _ := services.GetPairMeaningsMap()
	fmt.Printf("69: %s\n", meanings["69"])
	fmt.Printf("79: %s\n", meanings["79"])
}
