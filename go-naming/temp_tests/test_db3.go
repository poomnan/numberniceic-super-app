//go:build tools

package main

import (
"fmt"
"go-naming/database"
    "github.com/lib/pq"
)

func main() {
	database.Connect()
	
	query := `
			SELECT count(*)
			FROM names_miracle 
			WHERE (sat_sum = ANY($1::int[])) AND (sha_sum = ANY($2::int[]))
            AND thname != $3
	`
	targetSatSums := []int{32, 40, 41, 45, 55, 65, 69}
	targetShaSums := []int{5, 14, 42}
	lastname := "ณเดชน์"
	
	fmt.Println("Searching for matching names...")
	
	var count int
	err := database.DB.QueryRow(query, pq.Array(targetSatSums), pq.Array(targetShaSums), lastname).Scan(&count)
	if err != nil {
		fmt.Println("Error:", err)
		return
	}
	fmt.Println("Found strict matching names (excluding lastname):", count)
}
