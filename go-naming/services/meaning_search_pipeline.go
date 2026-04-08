package services

import (
	"fmt"
	"strings"
)

type SearchEntryMode string

const (
	SearchEntryModeDefault           SearchEntryMode = "default"
	SearchEntryModeMeaningFirst      SearchEntryMode = "meaning_first"
	SearchEntryModeCelebrityInspired SearchEntryMode = "celebrity_inspired"
)

type MeaningSearchPipelineInput struct {
	Keyword         string
	Lastname        string
	SemanticMeaning string
	MeaningIntent   string
	EntryMode       string
	SimilarMode     bool
	Limit           int
}

type MeaningSearchContext struct {
	EntryMode         SearchEntryMode
	WordContext       string
	MeaningContext    string
	RetrievalStrategy string
	SemanticWeight    float64
	WordWeight        float64
	CandidateLimit    int
	IsBroadSearch     bool
}

func BuildMeaningSearchContext(input MeaningSearchPipelineInput, fallbackMeaning string) MeaningSearchContext {
	keyword := strings.TrimSpace(input.Keyword)
	lastname := strings.TrimSpace(input.Lastname)
	semanticMeaning := strings.TrimSpace(input.SemanticMeaning)
	meaningIntent := strings.TrimSpace(input.MeaningIntent)
	fallbackMeaning = strings.TrimSpace(fallbackMeaning)

	entryMode := normalizeSearchEntryMode(input.EntryMode, meaningIntent)
	meaningContext := keyword
	wordContext := keyword

	switch {
	case meaningIntent != "":
		meaningContext = meaningIntent
	case semanticMeaning != "":
		meaningContext = semanticMeaning
	case fallbackMeaning != "":
		meaningContext = fallbackMeaning
	case lastname != "" && keyword == "":
		meaningContext = lastname
	}

	if entryMode == SearchEntryModeMeaningFirst {
		if meaningIntent == "" && semanticMeaning == "" && keyword != "" {
			meaningContext = keyword
		}
		// Meaning-first search should let semantics drive candidate generation.
		// We keep wordContext for exact-name boosts only; retrieval weighting stays
		// heavily semantic so numerology remains the final validation layer.
		return MeaningSearchContext{
			EntryMode:         entryMode,
			WordContext:       wordContext,
			MeaningContext:    meaningContext,
			RetrievalStrategy: "semantic_primary_then_numerology",
			SemanticWeight:    2.8,
			WordWeight:        0.2,
			CandidateLimit:    resolvedCandidateLimit(input.Limit, input.SimilarMode, true),
			IsBroadSearch:     keyword == "" && input.SimilarMode && lastname != "",
		}
	}

	if entryMode == SearchEntryModeCelebrityInspired {
		if semanticMeaning == "" && fallbackMeaning != "" {
			meaningContext = fallbackMeaning
		}
		return MeaningSearchContext{
			EntryMode:         entryMode,
			WordContext:       wordContext,
			MeaningContext:    meaningContext,
			RetrievalStrategy: "hybrid_name_plus_semantic",
			SemanticWeight:    2.5,
			WordWeight:        0.5,
			CandidateLimit:    resolvedCandidateLimit(input.Limit, input.SimilarMode, false),
			IsBroadSearch:     keyword == "" && input.SimilarMode && lastname != "",
		}
	}

	if semanticMeaning == "" && fallbackMeaning != "" {
		meaningContext = fallbackMeaning
	}

	return MeaningSearchContext{
		EntryMode:         entryMode,
		WordContext:       wordContext,
		MeaningContext:    meaningContext,
		RetrievalStrategy: "default_hybrid",
		SemanticWeight:    2.5,
		WordWeight:        0.5,
		CandidateLimit:    resolvedCandidateLimit(input.Limit, input.SimilarMode, false),
		IsBroadSearch:     keyword == "" && input.SimilarMode && lastname != "",
	}
}

func (c MeaningSearchContext) SelectProjection() string {
	if c.IsBroadSearch {
		return `0.0 as distance, 0.0 as root_score, 1.0 as semantic_score, 1.0 as hybrid_score, `
	}

	return fmt.Sprintf(
		`COALESCE((meaning_vector <=> $1), 0) as distance,
	       COALESCE(word_similarity(thname, $2), 0) as root_score,
	       GREATEST(0, COALESCE(1 - (meaning_vector <=> $1), 0)) as semantic_score,
	       (COALESCE(word_similarity(thname, $2), 0) * %.2f + GREATEST(0, COALESCE(1 - (meaning_vector <=> $1), 0)) * %.2f) as hybrid_score, `,
		c.WordWeight,
		c.SemanticWeight,
	)
}

func (c MeaningSearchContext) OrderByExpression() string {
	if c.IsBroadSearch {
		return "name_id DESC"
	}
	return fmt.Sprintf(
		"(COALESCE(word_similarity(thname, $2), 0) * %.2f + (1 - COALESCE(meaning_vector <=> $1, 1)) * %.2f) DESC",
		c.WordWeight,
		c.SemanticWeight,
	)
}

func normalizeSearchEntryMode(raw string, meaningIntent string) SearchEntryMode {
	switch strings.TrimSpace(strings.ToLower(raw)) {
	case "meaning_first":
		return SearchEntryModeMeaningFirst
	case "celebrity_inspired":
		return SearchEntryModeCelebrityInspired
	default:
		if strings.TrimSpace(meaningIntent) != "" {
			return SearchEntryModeMeaningFirst
		}
		return SearchEntryModeDefault
	}
}

func resolvedCandidateLimit(requestLimit int, similarMode bool, semanticPrimary bool) int {
	limit := 100
	if requestLimit > 100 {
		limit = requestLimit
	}
	if similarMode && limit < 150 {
		limit = 150
	}
	if semanticPrimary && limit < 120 {
		limit = 120
	}
	return limit
}
