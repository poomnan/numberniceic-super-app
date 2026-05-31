package services

import (
	"context"
	"database/sql"
	"fmt"
	"sort"
	"strconv"
	"strings"
	"time"
	"unicode/utf8"
)

const (
	nameIntentModeName    = "NAME"
	nameIntentModeHybrid  = "HYBRID"
	nameIntentModeMeaning = "MEANING"

	nameIntentQueryTimeout = 150 * time.Millisecond
	nameIntentMaxResults   = 5
)

type Result struct {
	Mode       string   `json:"mode"`
	Confidence float64  `json:"confidence"`
	Candidates []string `json:"candidates"`
	BestScore  float64  `json:"best_score"`
}

type fuzzyCandidate struct {
	Name  string
	Score float64
}

type nameIntentSource struct {
	Table  string
	Column string
}

// DetectNameIntent classifies user input into NAME, HYBRID, or MEANING by
// combining an exact-match fast path with a bounded pg_trgm fuzzy lookup.
func DetectNameIntent(ctx context.Context, db *sql.DB, input string) (Result, error) {
	normalized := normalizeIntentInput(input)
	if normalized == "" {
		return Result{
			Mode:       nameIntentModeMeaning,
			Confidence: 0,
			Candidates: []string{},
			BestScore:  0,
		}, nil
	}

	queryCtx, cancel := context.WithTimeout(ctx, nameIntentQueryTimeout)
	defer cancel()

	source, err := detectNameIntentSource(queryCtx, db)
	if err != nil {
		return Result{}, err
	}

	parts := strings.Fields(normalized)
	if len(parts) == 2 {
		if isFullName, first, last := looksLikeThaiFullNameLocal(normalized); isFullName {
			rhyme := hasThaiNameRhyme(first, last)
			meaningSignals := meaningPhraseSignals(normalized)
			tokenMatches := exactNameTokenMatches(db, parts)
			if classification, ok := classifyTwoPartThaiInput(
				normalized,
				first,
				last,
				tokenMatches[first],
				tokenMatches[last],
				rhyme,
				meaningSignals,
				[]string{"intent_structural_full_name"},
			); ok {
				if classification.Type == "meaning" {
					return Result{
						Mode:       nameIntentModeMeaning,
						Confidence: classification.Confidence,
						Candidates: []string{},
						BestScore:  0,
					}, nil
				}
				classification = finalizeInputClassification(normalized, classification)
				candidate := classification.FirstName
				if candidate == "" {
					candidate = classification.SeedName
				}
				if candidate == "" {
					candidate = first
				}
				return Result{
					Mode:       nameIntentModeName,
					Confidence: classification.Confidence,
					Candidates: []string{candidate},
					BestScore:  classification.Confidence,
				}, nil
			}
		}
	}

	nameLike := looksLikePersonName(normalized)
	similarityThreshold := 0.3
	if shouldUseRelaxedThreshold(normalized, nameLike) {
		similarityThreshold = 0.2
	}

	var exactName string
	err = db.QueryRowContext(
		queryCtx,
		buildExactMatchQuery(source),
		normalized,
	).Scan(&exactName)
	if err == nil {
		return Result{
			Mode:       nameIntentModeName,
			Confidence: 1.0,
			Candidates: []string{exactName},
			BestScore:  1.0,
		}, nil
	}
	if err != nil && err != sql.ErrNoRows {
		return Result{}, err
	}

	if utf8.RuneCountInString(normalized) < 3 {
		return Result{
			Mode:       classifyShortInputMode(nameLike),
			Confidence: shortInputConfidence(nameLike),
			Candidates: []string{},
			BestScore:  shortInputConfidence(nameLike),
		}, nil
	}

	tx, err := db.BeginTx(queryCtx, &sql.TxOptions{ReadOnly: true})
	if err != nil {
		return Result{}, err
	}
	defer tx.Rollback()

	if _, err := tx.ExecContext(
		queryCtx,
		`SELECT set_config('pg_trgm.similarity_threshold', $1, true)`,
		strconv.FormatFloat(similarityThreshold, 'f', 2, 64),
	); err != nil {
		return Result{}, err
	}

	rows, err := tx.QueryContext(queryCtx, buildFuzzyMatchQuery(source), normalized)
	if err != nil {
		return Result{}, err
	}
	defer rows.Close()

	candidates := make([]fuzzyCandidate, 0, nameIntentMaxResults)
	for rows.Next() {
		var candidate fuzzyCandidate
		if err := rows.Scan(&candidate.Name, &candidate.Score); err != nil {
			return Result{}, err
		}
		if strings.HasPrefix(normalizeIntentInput(candidate.Name), normalized) {
			candidate.Score += 0.05
		}
		candidate.Score = clampUnit(candidate.Score)
		candidates = append(candidates, candidate)
	}
	if err := rows.Err(); err != nil {
		return Result{}, err
	}

	sort.SliceStable(candidates, func(i, j int) bool {
		if candidates[i].Score == candidates[j].Score {
			return candidates[i].Name < candidates[j].Name
		}
		return candidates[i].Score > candidates[j].Score
	})

	if err := tx.Commit(); err != nil {
		return Result{}, err
	}

	return classifyIntentCandidates(normalized, candidates, nameLike), nil
}

func detectNameIntentSource(ctx context.Context, db *sql.DB) (nameIntentSource, error) {
	var namesTable sql.NullString
	var miracleTable sql.NullString

	err := db.QueryRowContext(
		ctx,
		`SELECT to_regclass('public.names')::text, to_regclass('public.names_miracle')::text`,
	).Scan(&namesTable, &miracleTable)
	if err != nil {
		return nameIntentSource{}, err
	}

	switch {
	case namesTable.Valid:
		return nameIntentSource{Table: "names", Column: "name"}, nil
	case miracleTable.Valid:
		return nameIntentSource{Table: "names_miracle", Column: "thname"}, nil
	default:
		return nameIntentSource{Table: "names", Column: "name"}, nil
	}
}

func buildExactMatchQuery(source nameIntentSource) string {
	return "SELECT " + source.Column + " FROM " + source.Table + " WHERE " + source.Column + " = $1 LIMIT 1"
}

func buildFuzzyMatchQuery(source nameIntentSource) string {
	return "SELECT " + source.Column + ", similarity(" + source.Column + ", $1) AS score " +
		"FROM " + source.Table + " " +
		"WHERE " + source.Column + " % $1 " +
		"ORDER BY score DESC " +
		"LIMIT 5"
}

func normalizeIntentInput(input string) string {
	input = strings.TrimSpace(input)
	if input == "" {
		return ""
	}

	fields := strings.Fields(input)
	if len(fields) == 0 {
		return ""
	}

	normalized := strings.Join(fields, " ")
	return lowerASCII(normalized)
}

func lowerASCII(input string) string {
	var builder strings.Builder
	builder.Grow(len(input))

	for _, r := range input {
		if r >= 'A' && r <= 'Z' {
			builder.WriteRune(r + ('a' - 'A'))
			continue
		}
		builder.WriteRune(r)
	}

	return builder.String()
}

func classifyIntentCandidates(input string, candidates []fuzzyCandidate, nameLike bool) Result {
	if len(candidates) == 0 {
		if nameLike {
			return Result{
				Mode:       nameIntentModeName,
				Confidence: 0.55,
				Candidates: []string{input},
				BestScore:  0.55,
			}
		}

		return Result{
			Mode:       nameIntentModeMeaning,
			Confidence: 0,
			Candidates: []string{},
			BestScore:  0,
		}
	}

	bestScore := candidates[0].Score
	names := make([]string, 0, len(candidates))
	for _, candidate := range candidates {
		if candidate.Score > bestScore {
			bestScore = candidate.Score
		}
		names = append(names, candidate.Name)
	}

	mode := nameIntentModeMeaning
	switch {
	case bestScore >= 0.85:
		mode = nameIntentModeName
	case bestScore >= 0.65:
		mode = nameIntentModeHybrid
	case nameLike:
		mode = nameIntentModeHybrid
	}

	return Result{
		Mode:       mode,
		Confidence: bestScore,
		Candidates: names,
		BestScore:  bestScore,
	}
}

func looksLikePersonName(input string) bool {
	if strings.TrimSpace(input) == "" {
		return false
	}

	if strings.Contains(input, " ") {
		return false
	}

	runeCount := utf8.RuneCountInString(input)
	if runeCount < 2 || runeCount > 8 {
		return false
	}

	for _, r := range input {
		switch {
		case r >= '0' && r <= '9':
			return false
		case r == '-' || r == '_' || r == '/' || r == ',' || r == '.':
			return false
		}
	}

	return true
}

func shouldUseRelaxedThreshold(input string, nameLike bool) bool {
	if !nameLike {
		return false
	}
	return utf8.RuneCountInString(input) <= 4
}

func classifyShortInputMode(nameLike bool) string {
	if nameLike {
		return nameIntentModeName
	}
	return nameIntentModeMeaning
}

func shortInputConfidence(nameLike bool) float64 {
	if nameLike {
		return 0.55
	}
	return 0
}

func clampUnit(value float64) float64 {
	switch {
	case value < 0:
		return 0
	case value > 1:
		return 1
	default:
		return value
	}
}

func (s nameIntentSource) String() string {
	return fmt.Sprintf("%s.%s", s.Table, s.Column)
}
