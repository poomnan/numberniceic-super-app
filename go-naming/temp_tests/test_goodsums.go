package main

import (
"fmt"
"go-naming/database"
"go-naming/services"
)

func main() {
	database.InitDB()
	goodSums, _ := services.GetGoodSums()
	fmt.Printf("Real GoodSums: %v\n", goodSums)
	for _, sum := range goodSums {
		if sum == 10 || sum == 18 {
			fmt.Printf("Alert! %d is in GoodSums!!!\n", sum)
		}
	}
}
