package handlers

import (
	"go-naming/services"
	"html/template"
	"log"
	"net/http"
	"strings"
)

func ArticleReaderHandler(w http.ResponseWriter, r *http.Request) {
	// Simple slug extraction from path: /article/some-slug
	path := r.URL.Path
	slug := strings.TrimPrefix(path, "/article/")
	if slug == "" {
		http.NotFound(w, r)
		return
	}

	article, err := services.GetArticleBySlug(slug)
	if err != nil {
		log.Printf("Error fetching article %s: %v", slug, err)
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	if article == nil {
		http.NotFound(w, r)
		return
	}

	highlights, _ := services.GetHighlightArticles()

	data := map[string]interface{}{
		"Article":    article,
		"Highlights": highlights, // For sidebar/footer recommendations
	}

	tmpl := template.New("article_reader.html").Funcs(template.FuncMap{
		"safeHTML": func(s string) template.HTML {
			return template.HTML(s)
		},
	})

	tmpl, err = tmpl.ParseFiles("templates/article_reader.html")
	if err != nil {
		log.Printf("Error parsing article template: %v", err)
		http.Error(w, "Template error", http.StatusInternalServerError)
		return
	}

	if err := tmpl.Execute(w, data); err != nil {
		log.Printf("Error executing article template: %v", err)
	}
}
