package handlers

import (
	"go-naming/services"
	"html/template"
	"log"
	"net/http"
)

func LandingPageHandler(w http.ResponseWriter, r *http.Request) {
	highlights, err := services.GetHighlightArticles()
	if err != nil {
		log.Printf("Error fetching highlights: %v", err)
	}

	popular, err := services.GetPopularArticles()
	if err != nil {
		log.Printf("Error fetching popular: %v", err)
	}

	data := map[string]interface{}{
		"Highlights": highlights,
		"Popular":    popular,
	}

	tmpl := template.New("index.html").Funcs(template.FuncMap{
		"add": func(a, b int) int {
			return a + b
		},
	})

	tmpl, err = tmpl.ParseFiles("templates/index.html")
	if err != nil {
		log.Printf("Error parsing template: %v", err)
		http.Error(w, "Template error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Cache-Control", "no-store, no-cache, must-revalidate")
	if err := tmpl.Execute(w, data); err != nil {
		log.Printf("Error executing template: %v", err)
	}
}
