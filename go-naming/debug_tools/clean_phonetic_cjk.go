// Clean Chinese/Korean/Japanese characters from phonetic columns
// Run directly on the production server
package main

import (
	"database/sql"
	"fmt"
	"log"
	"regexp"
	"strings"

	_ "github.com/lib/pq"
)

var cjkRegex = regexp.MustCompile(`[\x{3400}-\x{4DBF}\x{4E00}-\x{9FFF}\x{F900}-\x{FAFF}\x{FF00}-\x{FFEF}]`)

func clean(s string) string {
	if s == "" {
		return s
	}
	return strings.TrimSpace(cjkRegex.ReplaceAllString(s, ""))
}

func main() {
	dsn := "postgres://tayap:IntelliP24.X@localhost:5432/tayap?sslmode=disable"
	db, err := sql.Open("postgres", dsn)
	if err != nil {
		log.Fatalf("connect: %v", err)
	}
	defer db.Close()

	var total int
	db.QueryRow("SELECT COUNT(*) FROM names_miracle WHERE phonetic_summary IS NOT NULL").Scan(&total)
	fmt.Printf("Total rows with phonetic_summary: %d\n", total)

	rows, err := db.Query("SELECT name_id, COALESCE(phonetic_summary,''), COALESCE(phonetic_issues::text,'[]'), COALESCE(phonetic_style_tone::text,'[]') FROM names_miracle WHERE phonetic_summary IS NOT NULL")
	if err != nil {
		log.Fatalf("query: %v", err)
	}
	defer rows.Close()

	updated := 0
	for rows.Next() {
		var id int
		var summary, issues, styleTone string
		if err := rows.Scan(&id, &summary, &issues, &styleTone); err != nil {
			continue
		}
		cs := clean(summary)
		ci := clean(issues)
		cst := clean(styleTone)
		if cs != summary || ci != issues || cst != styleTone {
			_, _ = db.Exec(
				"UPDATE names_miracle SET phonetic_summary=$1, phonetic_issues=$2::jsonb, phonetic_style_tone=$3::jsonb WHERE name_id=$4",
				cs, ciOrEmpty(ci), ciOrEmpty(cst), id,
			)
			updated++
			if updated%1000 == 0 {
				fmt.Printf("  cleaned %d rows...\n", updated)
			}
		}
	}
	fmt.Printf("Done. Cleaned %d rows.\n", updated)
}

func ciOrEmpty(s string) string {
	if s == "[]" || s == "" {
		return "[]"
	}
	// Try to keep it as a JSON array
	s = strings.TrimPrefix(s, "[")
	s = strings.TrimSuffix(s, "]")
	parts := strings.Split(s, ",")
	var out []string
	for _, p := range parts {
		p = strings.TrimSpace(p)
		if p != "" {
			out = append(out, p)
		}
	}
	if len(out) == 0 {
		return "[]"
	}
	return `["` + strings.Join(out, `","`) + `"]`
}
