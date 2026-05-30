package main

import (
"fmt"
"go-naming/database"
"go-naming/services"
)

func main() {
	database.InitDB()
	goodSums, _ := services.GetGoodSums()
	fmt.Printf("GetGoodSums() output: %v\n", goodSums)
}
