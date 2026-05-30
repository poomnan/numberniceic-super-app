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
			SELECT thname, gender
			FROM names_miracle 
			WHERE (sat_sum = ANY($1::int[])) AND (sha_sum = ANY($2::int[]))
            AND thname != $3
            LIMIT 10
	`
	targetSatSums := []int{32, 40, 41, 45, 55, 65, 69}
	targetShaSums := []int{5, 14, 42}
	lastname := "ณเดชน์"
	
	rows, err := database.DB.Query(query, pq.Array(targetSatSums), pq.Array(targetShaSums), lastname)
	if err != nil {
		fmt.Println("Error:", err)
		return
	}
    defer rows.Close()
    
    fmt.Println("Names found:")
    for rows.Next() {
        var name, gender string
        rows.Scan(&name, &gender)
        fmt.Println("-", name, "Gender:", gender)
    }
}
