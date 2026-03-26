package services

import (
	"database/sql"
	"go-naming/database"
	"go-naming/models"
)

func GetArticleBySlug(slug string) (*models.Article, error) {
	query := `
		SELECT art_id, slug, title, excerpt, category, image_url, published_at, is_published, content, title_short, pin_order 
		FROM articles 
		WHERE slug = $1 AND is_published = true
	`
	var a models.Article
	err := database.DB.QueryRow(query, slug).Scan(
		&a.ArtID, &a.Slug, &a.Title, &a.Excerpt, &a.Category, &a.ImageURL,
		&a.PublishedAt, &a.IsPublished, &a.Content, &a.TitleShort, &a.PinOrder,
	)
	if err == sql.ErrNoRows {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &a, nil
}
