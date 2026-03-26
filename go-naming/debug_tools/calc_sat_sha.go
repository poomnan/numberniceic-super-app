package main

import (
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"os"
)

func main() {
	os.Setenv("DATABASE_URL", "postgres://tayap:IntelliP24.X@127.0.0.1/tayap?sslmode=disable")
	database.Connect()

	name := "พีระพงศ์"
	res, err := services.DecodeName(name, "")
	if err != nil {
		panic(err)
	}

	fmt.Printf("Name: %s\n", res.Name)
	fmt.Printf("Sat Sum: %d\n", res.TotalSat)
	fmt.Printf("Sha Sum: %d\n", res.TotalSha)

	fmt.Println("Details per char:")
	for _, c := range res.Characters {
		fmt.Printf("Char: %s, Sat: %d, Sha: %d\n", c.Char, c.SatValue, c.ShaValue)
	}

	fmt.Printf("Sat Pairs: %v\n", res.SatPairs)
	fmt.Printf("Sha Pairs: %v\n", res.ShaPairs)
}
