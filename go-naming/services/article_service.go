package services

import (
	"go-naming/database"
	"go-naming/models"
	"log"
)

func GetHighlightArticles() ([]models.Article, error) {
	query := `
		SELECT art_id, slug, title, excerpt, category, image_url, published_at, is_published, content, title_short, pin_order 
		FROM articles 
		WHERE is_published = true 
		ORDER BY pin_order DESC, published_at DESC 
		LIMIT 4
	`
	rows, err := database.DB.Query(query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var articles []models.Article
	for rows.Next() {
		var a models.Article
		err := rows.Scan(
			&a.ArtID, &a.Slug, &a.Title, &a.Excerpt, &a.Category, &a.ImageURL,
			&a.PublishedAt, &a.IsPublished, &a.Content, &a.TitleShort, &a.PinOrder,
		)
		if err != nil {
			log.Printf("Error scanning article: %v", err)
			continue
		}
		articles = append(articles, a)
	}
	if articles == nil {
		articles = []models.Article{}
	}
	return articles, nil
}

func GetPopularArticles() ([]models.Article, error) {
	query := `
		SELECT art_id, slug, title, title_short 
		FROM articles 
		WHERE is_published = true 
		ORDER BY published_at DESC 
		LIMIT 3
	`
	rows, err := database.DB.Query(query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var articles []models.Article
	for rows.Next() {
		var a models.Article
		err := rows.Scan(&a.ArtID, &a.Slug, &a.Title, &a.TitleShort)
		if err != nil {
			log.Printf("Error scanning popular article: %v", err)
			continue
		}
		articles = append(articles, a)
	}
	if articles == nil {
		articles = []models.Article{}
	}
	return articles, nil
}

func GetAllArticles(limit, offset int) ([]models.Article, error) {
	query := `
		SELECT art_id, slug, title, excerpt, category, image_url, published_at, is_published, content, title_short, pin_order 
		FROM articles 
		WHERE is_published = true 
		ORDER BY pin_order DESC, published_at DESC 
		LIMIT $1 OFFSET $2
	`
	rows, err := database.DB.Query(query, limit, offset)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var articles []models.Article
	for rows.Next() {
		var a models.Article
		err := rows.Scan(
			&a.ArtID, &a.Slug, &a.Title, &a.Excerpt, &a.Category, &a.ImageURL,
			&a.PublishedAt, &a.IsPublished, &a.Content, &a.TitleShort, &a.PinOrder,
		)
		if err != nil {
			log.Printf("Error scanning article: %v", err)
			continue
		}
		articles = append(articles, a)
	}
	if articles == nil {
		articles = []models.Article{}
	}
	return articles, nil
}

func GetAllArticlesAdmin() ([]models.Article, error) {
	query := `
		SELECT art_id, slug, title, excerpt, category, image_url, published_at, is_published, content, title_short, pin_order 
		FROM articles 
		ORDER BY pin_order DESC, published_at DESC
	`
	rows, err := database.DB.Query(query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var articles []models.Article
	for rows.Next() {
		var a models.Article
		err := rows.Scan(
			&a.ArtID, &a.Slug, &a.Title, &a.Excerpt, &a.Category, &a.ImageURL,
			&a.PublishedAt, &a.IsPublished, &a.Content, &a.TitleShort, &a.PinOrder,
		)
		if err != nil {
			log.Printf("Error scanning article: %v", err)
			continue
		}
		articles = append(articles, a)
	}
	if articles == nil {
		articles = []models.Article{}
	}
	return articles, nil
}

func GetArticleByID(id int) (*models.Article, error) {
	var a models.Article
	query := `
		SELECT art_id, slug, title, excerpt, category, image_url, published_at, is_published, content, title_short, pin_order 
		FROM articles 
		WHERE art_id = $1
	`
	err := database.DB.QueryRow(query, id).Scan(
		&a.ArtID, &a.Slug, &a.Title, &a.Excerpt, &a.Category, &a.ImageURL,
		&a.PublishedAt, &a.IsPublished, &a.Content, &a.TitleShort, &a.PinOrder,
	)
	if err != nil {
		return nil, err
	}
	return &a, nil
}

func AddArticle(a models.Article) (int, error) {
	var id int
	query := `
		INSERT INTO articles (slug, title, excerpt, category, image_url, is_published, content, title_short, pin_order)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
		RETURNING art_id
	`
	err := database.DB.QueryRow(query, a.Slug, a.Title, a.Excerpt, a.Category, a.ImageURL, a.IsPublished, a.Content, a.TitleShort, a.PinOrder).Scan(&id)
	return id, err
}

func UpdateArticle(a models.Article) error {
	query := `
		UPDATE articles 
		SET slug = $1, title = $2, excerpt = $3, category = $4, image_url = $5, is_published = $6, content = $7, title_short = $8, pin_order = $9
		WHERE art_id = $10
	`
	_, err := database.DB.Exec(query, a.Slug, a.Title, a.Excerpt, a.Category, a.ImageURL, a.IsPublished, a.Content, a.TitleShort, a.PinOrder, a.ArtID)
	return err
}

func DeleteArticle(id int) error {
	_, err := database.DB.Exec("DELETE FROM articles WHERE art_id = $1", id)
	return err
}
