//go:build tools

package main

import (
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"os"

	"github.com/lib/pq"
)

func main() {
	os.Setenv("DATABASE_URL", "postgres://tayap:IntelliP24.X@127.0.0.1/tayap?sslmode=disable")
	database.Connect()

	goodSums, err := services.GetGoodSums()
	if err != nil {
		fmt.Printf("Error getting good sums: %v\n", err)
		return
	}
	fmt.Printf("Good Sums Count: %d\n", len(goodSums))
	fmt.Printf("Good Sums (First 20): %v\n", goodSums[:20])

	// Check DB stats
	// Use pq.Array to pass goodSums as array
	var count int
	err = database.DB.QueryRow("SELECT count(*) FROM names_miracle WHERE sat_sum = ANY($1)", pq.Array(goodSums)).Scan(&count)
	if err != nil {
		fmt.Printf("Error counting names: %v\n", err)
	} else {
		fmt.Printf("Names with Good Sat Sums: %d\n", count)
	}
}
