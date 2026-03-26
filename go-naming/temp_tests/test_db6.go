//go:build tools

package main

import (
"fmt"
"go-naming/database"
    "github.com/lib/pq"
"go-naming/services"
)

func main() {
	database.Connect()
	
	keyword := "ร่ำรวย"
	lastname := "ณเดชน์"
	searchContext := fmt.Sprintf("%s %s", keyword, lastname)
	
	embedding, err := services.GetEmbedding(searchContext)
	if err != nil {
		fmt.Println("Error:", err)
		return
	}
	
	query := `
			SELECT thname, gender, (meaning_vector <=> $1) as distance
			FROM names_miracle 
			WHERE (sat_sum = ANY($2::int[])) AND (sha_sum = ANY($3::int[]))
            AND thname != $4
            ORDER BY distance ASC
            LIMIT 10
	`
	targetSatSums := []int{32, 40, 41, 45, 55, 65, 69}
	targetShaSums := []int{5, 14, 42}
	
	// Need to format vector as string for pgvector '<=>' operator
	vectorStr := "["
	for i, v := range embedding {
		if i > 0 {
			vectorStr += ","
		}
		vectorStr += fmt.Sprintf("%f", v)
	}
	vectorStr += "]"
	
	rows, err := database.DB.Query(query, vectorStr, pq.Array(targetSatSums), pq.Array(targetShaSums), lastname)
	if err != nil {
		fmt.Println("Error querying DB:", err)
		return
	}
    defer rows.Close()
    
    fmt.Println("Names found:")
    for rows.Next() {
        var name, gender string
		var distance float64
        rows.Scan(&name, &gender, &distance)
        fmt.Println("-", name, "Gender:", gender, "Distance:", distance)
    }
}
