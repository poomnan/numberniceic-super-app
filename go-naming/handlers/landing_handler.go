package handlers

import (
	"go-naming/services"
	"html/template"
	"log"
	"net/http"
)

func LandingPageHandler(w http.ResponseWriter, r *http.Request) {
	highlights, _ := services.GetHighlightArticles()
	popular, _ := services.GetPopularArticles()

	data := map[string]interface{}{
		"Highlights": highlights,
		"Popular":    popular,
	}

	tmpl, err := template.ParseFiles("templates/index.html")
	if err != nil {
		log.Printf("Error parsing index template: %v", err)
		http.Error(w, "Template error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Cache-Control", "no-store, no-cache, must-revalidate")
	if err := tmpl.Execute(w, data); err != nil {
		log.Printf("Error executing index template: %v", err)
	}
}
