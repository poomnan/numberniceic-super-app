package services

import (
	"database/sql"
	"go-naming/database"
	"log"
	"strings"
	"sync"
	"time"
)

// AstroTermDictionaryEntry is the public shape returned to clients.
type AstroTermDictionaryEntry struct {
	TermKey     string `json:"term_key"`
	DisplayName string `json:"display_name"`
	Description string `json:"description"`
}

var (
	astroTermsCacheMu   sync.RWMutex
	astroTermsCacheAt   time.Time
	astroTermsCacheData map[string]AstroTermDictionaryEntry
)

func getAstroTermsFromMySQL(db *sql.DB) map[string]AstroTermDictionaryEntry {
	if db == nil {
		return map[string]AstroTermDictionaryEntry{}
	}

	rows, err := db.Query(`
		SELECT term_key, display_name, COALESCE(description, '')
		FROM astro_terms_dictionary
		WHERE is_active = 1
	`)
	if err != nil {
		log.Printf("getAstroTermsFromMySQL query error: %v", err)
		return map[string]AstroTermDictionaryEntry{}
	}
	defer rows.Close()

	out := map[string]AstroTermDictionaryEntry{}
	for rows.Next() {
		var key, name, desc string
		if err := rows.Scan(&key, &name, &desc); err != nil {
			continue
		}
		key = strings.TrimSpace(key)
		if key == "" {
			continue
		}
		out[key] = AstroTermDictionaryEntry{
			TermKey:     key,
			DisplayName: strings.TrimSpace(name),
			Description: strings.TrimSpace(desc),
		}
	}
	return out
}

// GetAstroTermsDictionary returns a cached (60s) dictionary for term_key -> entry.
func GetAstroTermsDictionary() map[string]AstroTermDictionaryEntry {
	astroTermsCacheMu.RLock()
	if astroTermsCacheData != nil && time.Since(astroTermsCacheAt) < 60*time.Second {
		defer astroTermsCacheMu.RUnlock()
		// Return a shallow copy to prevent callers from mutating the cache.
		out := make(map[string]AstroTermDictionaryEntry, len(astroTermsCacheData))
		for k, v := range astroTermsCacheData {
			out[k] = v
		}
		return out
	}
	astroTermsCacheMu.RUnlock()

	astroTermsCacheMu.Lock()
	defer astroTermsCacheMu.Unlock()
	if astroTermsCacheData != nil && time.Since(astroTermsCacheAt) < 60*time.Second {
		out := make(map[string]AstroTermDictionaryEntry, len(astroTermsCacheData))
		for k, v := range astroTermsCacheData {
			out[k] = v
		}
		return out
	}

	db := database.GetMySQLDB()
	astroTermsCacheData = getAstroTermsFromMySQL(db)
	astroTermsCacheAt = time.Now()

	out := make(map[string]AstroTermDictionaryEntry, len(astroTermsCacheData))
	for k, v := range astroTermsCacheData {
		out[k] = v
	}
	return out
}

// ResolveAstroTermName returns display_name for a term_key, or fallback if not found.
func ResolveAstroTermName(termKey string, fallback string) string {
	termKey = strings.TrimSpace(termKey)
	if termKey == "" {
		return fallback
	}
	dict := GetAstroTermsDictionary()
	if v, ok := dict[termKey]; ok && strings.TrimSpace(v.DisplayName) != "" {
		return v.DisplayName
	}
	return fallback
}

// ResolveAstroTermDescription returns description for a term_key, or fallback if not found.
func ResolveAstroTermDescription(termKey string, fallback string) string {
	termKey = strings.TrimSpace(termKey)
	if termKey == "" {
		return fallback
	}
	dict := GetAstroTermsDictionary()
	if v, ok := dict[termKey]; ok && strings.TrimSpace(v.Description) != "" {
		return v.Description
	}
	return fallback
}

