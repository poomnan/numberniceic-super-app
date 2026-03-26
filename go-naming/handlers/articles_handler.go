package handlers

import (
	"go-naming/services"
	"html/template"
	"log"
	"net/http"
	"strconv"
)

func ArticlesListHandler(w http.ResponseWriter, r *http.Request) {
	pageStr := r.URL.Query().Get("page")
	page, _ := strconv.Atoi(pageStr)
	if page < 1 {
		page = 1
	}

	limit := 12
	offset := (page - 1) * limit

	articles, err := services.GetAllArticles(limit, offset)
	if err != nil {
		log.Printf("Error fetching articles: %v", err)
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	data := map[string]interface{}{
		"Articles": articles,
		"Page":     page,
	}

	tmpl := template.New("articles_list.html").Funcs(template.FuncMap{
		"add": func(a, b int) int {
			return a + b
		},
	})

	tmpl, err = tmpl.ParseFiles("templates/articles_list.html")
	if err != nil {
		log.Printf("Error parsing articles list template: %v", err)
		http.Error(w, "Template error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Cache-Control", "no-store, no-cache, must-revalidate")
	if err := tmpl.Execute(w, data); err != nil {
		log.Printf("Error executing articles list template: %v", err)
	}
}
