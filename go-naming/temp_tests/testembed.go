//go:build tools

package main

import (
	"fmt"
	"go-naming/services"
)

func main() {
	vec, err := services.GetEmbedding("ณเดชน์")
	if err != nil {
		fmt.Printf("Error: %v\n", err)
	} else {
		fmt.Printf("Vec length: %d\n", len(vec))
	}
}
