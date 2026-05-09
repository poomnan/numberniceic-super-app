package handlers

import (
	"context"
	"encoding/json"
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"log"
	"math"
	"net/http"
	"sort"
	"strings"
	"sync"
	"time"
	"unicode/utf8"

	"github.com/lib/pq"
)

// --- Mobile Name Search API ---

// MobileSearchRequest defines the input for mobile app name search
type MobileSearchRequest struct {
	Keyword         string `json:"keyword"`
	Lastname        string `json:"lastname"`
	Day             string `json:"day"`
	Gender          string `json:"gender"`
	SemanticMeaning string `json:"semantic_meaning"`
	MeaningIntent   string `json:"meaning_intent"`
	EntryMode       string `json:"entry_mode"`
	FilterSat       bool   `json:"filter_sat"`
	FilterSha       bool   `json:"filter_sha"`
	FilterKaki      bool   `json:"filter_kaki"`
	SimilarMode     bool   `json:"similar_mode"`
	Limit           int    `json:"limit"`
}

// CharHighlight represents a single character with its kaki status
type CharHighlight struct {
	Char   string `json:"char"`
	IsKaki bool   `json:"is_kaki"`
}

// MobileNameResult represents a single name result for mobile
type MobileNameResult struct {
	Name                string          `json:"name"`
	Meaning             string          `json:"meaning"`
	Gender              string          `json:"gender"`
	PhoneticSummary     string          `json:"phonetic_summary,omitempty"`
	SatSum              int             `json:"sat_sum"`
	ShaSum              int             `json:"sha_sum"`
	TotalSat            int             `json:"total_sat"`
	TotalSha            int             `json:"total_sha"`
	Distance            float64         `json:"distance"`
	RootScore           float64         `json:"root_score"`
	SemanticScore       float64         `json:"semantic_score"`
	HybridScore         float64         `json:"hybrid_score"`
	BonusCalculated     float64         `json:"bonus_calculated"`
	PhoneticScore       *int            `json:"phonetic_score,omitempty"`
	PronunciationEase   *int            `json:"pronunciation_ease,omitempty"`
	EuphonyScore        *int            `json:"euphony_score,omitempty"`
	RhythmScore         *int            `json:"rhythm_score,omitempty"`
	IsSatGood           bool            `json:"is_sat_good"`
	IsShaGood           bool            `json:"is_sha_good"`
	IsTotalSatGood      bool            `json:"is_total_sat_good"`
	IsTotalShaGood      bool            `json:"is_total_sha_good"`
	SatPairType         string          `json:"sat_pair_type"`
	ShaPairType         string          `json:"sha_pair_type"`
	TotalSatPairType    string          `json:"total_sat_pair_type"`
	TotalShaPairType    string          `json:"total_sha_pair_type"`
	SatPairPoint        int             `json:"sat_pair_point"`
	ShaPairPoint        int             `json:"sha_pair_point"`
	TotalSatPairPoint   int             `json:"total_sat_pair_point"`
	TotalShaPairPoint   int             `json:"total_sha_pair_point"`
	KakiHighlight       []CharHighlight `json:"kaki_highlight"`
	FinalRankScore      int             `json:"final_rank_score"`
	SemanticRankScore   int             `json:"semantic_rank_score"`
	NumerologyScore     int             `json:"numerology_rank_score"`
	PairTypeBonus       int             `json:"pair_type_bonus"`
	PairPointBonus      int             `json:"pair_point_bonus"`
	LengthBonus         int             `json:"length_bonus"`
	KakiBonus           int             `json:"kaki_bonus"`
	SatBonus            int             `json:"sat_bonus"`
	ShaBonus            int             `json:"sha_bonus"`
	DoubleBonus         int             `json:"double_bonus"`
	PhoneticBonus       int             `json:"phonetic_bonus"`
	PhoneticPenalty     int             `json:"phonetic_penalty,omitempty"`
	IsRelaxedNumerology bool            `json:"is_relaxed_numerology,omitempty"`
	RankReasons         []string        `json:"rank_reasons"`
}

// ... unchanged ...
// KakiInfo provides metadata about kaki characters for the selected day
type KakiInfo struct {
	Day         string   `json:"day"`
	DayThai     string   `json:"day_th"`
	Description string   `json:"description"`
	KakiChars   []string `json:"kaki_chars"`
}

// MobileSearchResponse is the full response for mobile
type MobileSearchResponse struct {
	Success       bool               `json:"success"`
	Error         any                `json:"error,omitempty"`
	Results       []MobileNameResult `json:"results"`
	KakiInfo      *KakiInfo          `json:"kaki_info,omitempty"`
	RetrievalMeta *RetrievalMeta     `json:"retrieval_meta,omitempty"`
	Total         int                `json:"total"`
}

type mobileSearchCacheEntry struct {
	response  MobileSearchResponse
	expiresAt time.Time
}

var mobileSearchCache = struct {
	sync.RWMutex
	items map[string]mobileSearchCacheEntry
}{
	items: map[string]mobileSearchCacheEntry{},
}

const mobileSearchCacheTTL = 45 * time.Second
const mobileSearchCacheMaxEntries = 500

type seedAnalysisCacheEntry struct {
	targetSatSums []int
	targetShaSums []int
	expiresAt     time.Time
}

var seedAnalysisCache = struct {
	sync.RWMutex
	items map[string]seedAnalysisCacheEntry
}{
	items: map[string]seedAnalysisCacheEntry{},
}

const seedAnalysisCacheTTL = 2 * time.Minute
const seedAnalysisCacheMaxEntries = 300

type RetrievalStageMeta struct {
	Stage            string `json:"stage"`
	Pool             string `json:"pool"`
	RawRows          int    `json:"raw_rows"`
	Accepted         int    `json:"accepted"`
	PhoneticFiltered int    `json:"phonetic_filtered,omitempty"`
	SatShaCount      int    `json:"sat_sha_count,omitempty"`
	UniqueTotal      int    `json:"unique_total"`
}

type doubleGoodStageConfig struct {
	Name               string
	MinTrigram         float64
	PhoneticRelaxLevel int
	StrongSoftPenalty  bool
	AllowEitherGood    bool
	RelaxedNumerology  bool
	DisableNumerology  bool
}

type poolResult struct {
	Pool    string
	Results []MobileNameResult
	Stats   retrievalScanStats
	Err     error
}

type RetrievalMeta struct {
	EntryMode         string               `json:"entry_mode"`
	RetrievalStrategy string               `json:"retrieval_strategy"`
	MeaningContext    string               `json:"meaning_context"`
	SemanticWeight    float64              `json:"semantic_weight"`
	WordWeight        float64              `json:"word_weight"`
	CandidateLimit    int                  `json:"candidate_limit"`
	StageCounters     []RetrievalStageMeta `json:"stage_counters,omitempty"`
}

func setMobileCORSHeaders(w http.ResponseWriter) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
}

func makeMobileSearchCacheKey(req MobileSearchRequest) string {
	parts := []string{
		"k=" + strings.TrimSpace(req.Keyword),
		"l=" + strings.TrimSpace(req.Lastname),
		"d=" + strings.TrimSpace(req.Day),
		"g=" + strings.TrimSpace(req.Gender),
		"sm=" + strings.TrimSpace(req.SemanticMeaning),
		"mi=" + strings.TrimSpace(req.MeaningIntent),
		"em=" + strings.TrimSpace(req.EntryMode),
		fmt.Sprintf("sat=%t", req.FilterSat),
		fmt.Sprintf("sha=%t", req.FilterSha),
		fmt.Sprintf("kaki=%t", req.FilterKaki),
		fmt.Sprintf("sim=%t", req.SimilarMode),
		fmt.Sprintf("lim=%d", req.Limit),
	}
	return strings.Join(parts, "|")
}

func cloneMobileSearchResponse(src MobileSearchResponse) MobileSearchResponse {
	dst := src
	if src.Results != nil {
		dst.Results = make([]MobileNameResult, len(src.Results))
		for i := range src.Results {
			dst.Results[i] = src.Results[i]
			if src.Results[i].KakiHighlight != nil {
				dst.Results[i].KakiHighlight = append([]CharHighlight(nil), src.Results[i].KakiHighlight...)
			}
			if src.Results[i].RankReasons != nil {
				dst.Results[i].RankReasons = append([]string(nil), src.Results[i].RankReasons...)
			}
		}
	}
	if src.KakiInfo != nil {
		info := *src.KakiInfo
		if src.KakiInfo.KakiChars != nil {
			info.KakiChars = append([]string(nil), src.KakiInfo.KakiChars...)
		}
		dst.KakiInfo = &info
	}
	if src.RetrievalMeta != nil {
		meta := *src.RetrievalMeta
		if src.RetrievalMeta.StageCounters != nil {
			meta.StageCounters = append([]RetrievalStageMeta(nil), src.RetrievalMeta.StageCounters...)
		}
		dst.RetrievalMeta = &meta
	}
	return dst
}

func getCachedMobileSearchResponse(key string) (MobileSearchResponse, bool) {
	now := time.Now()
	mobileSearchCache.RLock()
	entry, ok := mobileSearchCache.items[key]
	mobileSearchCache.RUnlock()
	if !ok || now.After(entry.expiresAt) {
		return MobileSearchResponse{}, false
	}
	return cloneMobileSearchResponse(entry.response), true
}

func setCachedMobileSearchResponse(key string, resp MobileSearchResponse) {
	now := time.Now()
	mobileSearchCache.Lock()
	if len(mobileSearchCache.items) >= mobileSearchCacheMaxEntries {
		for k, v := range mobileSearchCache.items {
			if now.After(v.expiresAt) {
				delete(mobileSearchCache.items, k)
			}
		}
		if len(mobileSearchCache.items) >= mobileSearchCacheMaxEntries {
			for k := range mobileSearchCache.items {
				delete(mobileSearchCache.items, k)
				break
			}
		}
	}
	mobileSearchCache.items[key] = mobileSearchCacheEntry{
		response:  cloneMobileSearchResponse(resp),
		expiresAt: now.Add(mobileSearchCacheTTL),
	}
	mobileSearchCache.Unlock()
}

func makeSeedAnalysisCacheKey(req MobileSearchRequest, lastnameSat int, lastnameSha int) string {
	return strings.Join([]string{
		strings.TrimSpace(req.Keyword),
		strings.TrimSpace(req.SemanticMeaning),
		strings.TrimSpace(req.Lastname),
		fmt.Sprintf("ls=%d", lastnameSat),
		fmt.Sprintf("lh=%d", lastnameSha),
		fmt.Sprintf("sat=%t", req.FilterSat),
		fmt.Sprintf("sha=%t", req.FilterSha),
		fmt.Sprintf("sim=%t", req.SimilarMode),
	}, "|")
}

func getCachedSeedAnalysis(key string) (seedAnalysisCacheEntry, bool) {
	now := time.Now()
	seedAnalysisCache.RLock()
	entry, ok := seedAnalysisCache.items[key]
	seedAnalysisCache.RUnlock()
	if !ok || now.After(entry.expiresAt) {
		return seedAnalysisCacheEntry{}, false
	}
	return seedAnalysisCacheEntry{
		targetSatSums: append([]int(nil), entry.targetSatSums...),
		targetShaSums: append([]int(nil), entry.targetShaSums...),
		expiresAt:     entry.expiresAt,
	}, true
}

func setCachedSeedAnalysis(key string, satSums []int, shaSums []int) {
	now := time.Now()
	seedAnalysisCache.Lock()
	if len(seedAnalysisCache.items) >= seedAnalysisCacheMaxEntries {
		for k, v := range seedAnalysisCache.items {
			if now.After(v.expiresAt) {
				delete(seedAnalysisCache.items, k)
			}
		}
		if len(seedAnalysisCache.items) >= seedAnalysisCacheMaxEntries {
			for k := range seedAnalysisCache.items {
				delete(seedAnalysisCache.items, k)
				break
			}
		}
	}
	seedAnalysisCache.items[key] = seedAnalysisCacheEntry{
		targetSatSums: append([]int(nil), satSums...),
		targetShaSums: append([]int(nil), shaSums...),
		expiresAt:     now.Add(seedAnalysisCacheTTL),
	}
	seedAnalysisCache.Unlock()
}

// calculateBonus calculates the bonus score based on frontend ranking logic
func calculateBonus(name string, isSatGood, isShaGood, isTotalSatGood, isTotalShaGood bool, kakiHighlight []CharHighlight) float64 {
	bonus := 0.0

	// SAT good bonus
	if isSatGood {
		bonus += 20.0
	}

	// SHA good bonus
	if isShaGood {
		bonus += 20.0
	}

	// Double Lucky bonus (both SAT and SHA good)
	if isSatGood && isShaGood {
		bonus += 80.0
	}

	// Kaki free bonus (no kaki characters)
	hasKaki := false
	for _, h := range kakiHighlight {
		if h.IsKaki {
			hasKaki = true
			break
		}
	}
	if !hasKaki {
		bonus += 10.0
	}

	// Length bonus
	nameLen := len([]rune(name))
	if nameLen == 2 {
		bonus += 40.0
	} else if nameLen == 3 {
		bonus += 30.0
	} else if nameLen == 4 {
		bonus += 20.0
	} else if nameLen == 5 {
		bonus += 10.0
	} else if nameLen >= 6 {
		bonus -= 10.0
	}

	// Short Name Quality Bonus (for short names with good quality)
	if nameLen <= 3 && (isSatGood || isShaGood) {
		bonus += 30.0
	}

	return bonus
}

func calculateFinalRankScoreAndReasons(
	r *MobileNameResult,
	keyword string,
	showMatching bool,
	filterSat bool,
	filterSha bool,
	filterKaki bool,
	hasKeywordSignal bool,
) (int, []string) {
	satPass, shaPass, satPairType, shaPairType, satPairPoint, shaPairPoint := activeNumerologySignals(*r, showMatching)
	doubleGoodMode := filterSat && filterSha

	satBonus := 0
	if satPass {
		satBonus = 20
	}
	shaBonus := 0
	if shaPass {
		shaBonus = 20
	}
	doubleBonus := 0
	if satPass && shaPass {
		doubleBonus = 50
	}
	pairTypeBonus := pairTypeTierBonus(satPairType) + pairTypeTierBonus(shaPairType)
	pairPointBonus := pairPointRankBonus(satPairPoint) + pairPointRankBonus(shaPairPoint)

	kakiBonus := 0
	if len(r.KakiHighlight) > 0 {
		hasKaki := false
		for _, h := range r.KakiHighlight {
			if h.IsKaki {
				hasKaki = true
				break
			}
		}
		if !hasKaki {
			kakiBonus = 10
		}
	}

	lengthBonus := 0
	nameLen := len([]rune(r.Name))
	if nameLen == 2 {
		lengthBonus = 40
	} else if nameLen == 3 {
		lengthBonus = 30
	} else if nameLen == 4 {
		lengthBonus = 20
	} else if nameLen == 5 {
		lengthBonus = 10
	} else if nameLen >= 6 {
		lengthBonus = -10
	}

	phoneticBonus := phoneticRankingBonus(r.PhoneticScore, r.PronunciationEase, r.EuphonyScore, r.RhythmScore)
	nameNearBonus := nameNearnessBonus(r.RootScore)
	shapeBonus := 0
	if hasKeywordSignal {
		shapeBonus = thaiShapeBonus(keyword, r.Name)
	}
	muPriorityBonus := activeMuPriorityBonus(
		satPass,
		shaPass,
		len(r.KakiHighlight) == 0 || !containsKaki(r.KakiHighlight),
		filterSat,
		filterSha,
		filterKaki,
	)
	numerologyComponent := normalizedNumerologyScore(
		satPass,
		shaPass,
		satPairType,
		shaPairType,
		satPairPoint,
		shaPairPoint,
		kakiBonus,
		lengthBonus,
		muPriorityBonus,
	)
	semanticComponent := normalizedSemanticScore(r.SemanticScore, r.RootScore, hasKeywordSignal, shapeBonus, nameNearBonus)
	phoneticComponent := normalizedPhoneticScore(r.PhoneticScore, r.PronunciationEase, r.EuphonyScore, r.RhythmScore, r.PhoneticSummary)
	adjustedPhonetic := clampScore(phoneticComponent + float64(r.PhoneticPenalty))
	trigramComponent := clampScore(r.RootScore * 100)
	humanNameScore := computeHumanNameScore(r.Name)
	humanComponent := humanNameScore * 100
	baseScore := numerologyComponent*0.30 +
		trigramComponent*0.10 +
		semanticComponent*0.22 +
		adjustedPhonetic*0.12 +
		humanComponent*0.12 +
		lengthScoreMultiplier(nameLen)*14
	if doubleGoodMode {
		baseScore = numerologyComponent*0.25 +
			trigramComponent*0.35 +
			semanticComponent*0.15 +
			adjustedPhonetic*0.05 +
			humanComponent*0.08 +
			lengthScoreMultiplier(nameLen)*12
	}
	baseScore = clampScore(baseScore)
	eliteBoost := 0
	if doubleGoodMode {
		if numerologyComponent >= 85 {
			eliteBoost += 8
		}
		if satPairPoint >= 0 && shaPairPoint >= 0 {
			eliteBoost += 6
		}
		if math.Abs(float64(satPairPoint-shaPairPoint)) <= 12 {
			eliteBoost += 4
		}
		if adjustedPhonetic >= 70 {
			eliteBoost += 3
		}
		if numerologyComponent < 70 {
			eliteBoost -= 10
		}
		if r.IsRelaxedNumerology {
			eliteBoost -= 6
		}
	}
	if nameLen <= 5 && satPass && shaPass && r.SemanticScore > 0.75 {
		eliteBoost += 5
	}
	if nameLen <= 6 && satPass && shaPass && numerologyComponent >= 85 {
		eliteBoost += 3
	}
	originalScore := clampScore(baseScore + float64(eliteBoost))
	if doubleGoodMode {
		originalScore -= calculatePhoneticPenalty(
			safeInt(r.PhoneticScore),
			safeInt(r.PronunciationEase),
			safeInt(r.EuphonyScore),
			safeInt(r.RhythmScore),
		)
	}
	finalScore := int(clampScore(originalScore) + 0.5)

	r.SemanticRankScore = int(semanticComponent + 0.5)
	r.NumerologyScore = int(numerologyComponent + 0.5)
	r.PairTypeBonus = pairTypeBonus
	r.PairPointBonus = pairPointBonus
	r.LengthBonus = lengthBonus
	r.KakiBonus = kakiBonus
	r.SatBonus = satBonus
	r.ShaBonus = shaBonus
	r.DoubleBonus = doubleBonus
	r.PhoneticBonus = phoneticBonus

	reasons := []string{}
	if doubleGoodMode {
		reasons = append(reasons, "คำนวณคะแนนรวมโดยให้ความใกล้เสียง/รูปคำ 35% เลขศาสตร์ 25% ความเป็นชื่อไทย 8% ความหมาย 15% เสียงอ่าน 5% และความยาว 12%")
	} else {
		reasons = append(reasons, "คำนวณคะแนนรวมโดยให้เลขศาสตร์ 30% ความหมาย 22% เสียงอ่าน 12% ความใกล้เสียง 10% ความเป็นชื่อจริง 12% และความยาว 14%")
	}
	if muPriorityBonus != 0 {
		if muPriorityBonus > 0 {
			reasons = append(reasons, fmt.Sprintf("ตรงตามตัวเลือกสายมู %.0f/100", clampScore(float64(muPriorityBonus)/2.4+50)))
		} else {
			reasons = append(reasons, fmt.Sprintf("ยังไม่ตรงตัวเลือกสายมู จึงถูกกดคะแนน"))
		}
	}
	if hasKeywordSignal {
		reasons = append(reasons, fmt.Sprintf("รูปคำ/การสะกดใกล้ต้นแบบ %.0f/100", clampScore(r.RootScore*100)))
	}
	reasons = append(reasons, fmt.Sprintf("ความใกล้เคียงความหมาย %.0f/100", clampScore(semanticComponent)))
	reasons = append(reasons, fmt.Sprintf("คะแนนเลขศาสตร์รวม %.0f/100", clampScore(numerologyComponent)))
	if adjustedPhonetic > 0 {
		reasons = append(reasons, fmt.Sprintf("คะแนนเสียงอ่านและความเป็นธรรมชาติ %.0f/100", clampScore(adjustedPhonetic)))
	}
	if satBonus > 0 {
		reasons = append(reasons, fmt.Sprintf("เลขศาสตร์ผ่าน +%d", satBonus))
	}
	if shaBonus > 0 {
		reasons = append(reasons, fmt.Sprintf("พลังเงาผ่าน +%d", shaBonus))
	}
	if doubleBonus > 0 {
		reasons = append(reasons, fmt.Sprintf("ผ่านทั้งเลขศาสตร์และพลังเงา +%d", doubleBonus))
	}
	if pairTypeBonus > 0 {
		reasons = append(reasons, fmt.Sprintf("ระดับคู่เลข/คู่เงา %+d", pairTypeBonus))
	}
	if pairPointBonus != 0 {
		reasons = append(reasons, fmt.Sprintf("คะแนนละเอียด pairpoint %+d", pairPointBonus))
	}
	if kakiBonus > 0 {
		reasons = append(reasons, fmt.Sprintf("ปลอดกาลกิณี +%d", kakiBonus))
	}
	if lengthBonus != 0 {
		reasons = append(reasons, fmt.Sprintf("คะแนนความยาวชื่อ %+d", lengthBonus))
	}
	if phoneticBonus > 0 {
		reasons = append(reasons, fmt.Sprintf("เสียงอ่านลื่นและเป็นธรรมชาติ +%d", phoneticBonus))
	} else if phoneticBonus < 0 {
		reasons = append(reasons, fmt.Sprintf("ลดคะแนนเพราะเสียงอ่านสะดุด %d", phoneticBonus))
	}
	if r.PhoneticPenalty < 0 {
		reasons = append(reasons, fmt.Sprintf("ลดคะแนนเพิ่มจากโหมดผ่อนเสียงอ่าน %d", r.PhoneticPenalty))
	}
	if nameNearBonus > 0 {
		reasons = append(reasons, fmt.Sprintf("รูปคำใกล้ชื่อแม่ +%d", nameNearBonus))
	} else if nameNearBonus < 0 {
		reasons = append(reasons, fmt.Sprintf("ลดคะแนนเพราะรูปคำห่างจากชื่อแม่ %d", nameNearBonus))
	}
	if shapeBonus > 0 {
		reasons = append(reasons, fmt.Sprintf("รูปทรงชื่อคล้ายต้นแบบ +%d", shapeBonus))
	}
	if eliteBoost > 0 {
		reasons = append(reasons, fmt.Sprintf("บูสต์ความแข็งแรงและความน่าเชื่อถือ +%d", eliteBoost))
	} else if eliteBoost < 0 {
		reasons = append(reasons, fmt.Sprintf("กดคะแนนเพราะความแข็งแรงรวมยังไม่ถึงเกณฑ์ %d", eliteBoost))
	}
	if r.IsRelaxedNumerology {
		reasons = append(reasons, "ชื่อชุดนี้เป็นผลลัพธ์สำรอง: ผ่านเลขศาสตร์หรือพลังเงาอย่างน้อย 1 ด้าน")
	}

	return finalScore, reasons
}

func activeNumerologySignals(
	r MobileNameResult,
	showMatching bool,
) (bool, bool, string, string, int, int) {
	if showMatching {
		return r.IsTotalSatGood, r.IsTotalShaGood, r.TotalSatPairType, r.TotalShaPairType, r.TotalSatPairPoint, r.TotalShaPairPoint
	}
	return r.IsSatGood, r.IsShaGood, r.SatPairType, r.ShaPairType, r.SatPairPoint, r.ShaPairPoint
}

func containsKaki(items []CharHighlight) bool {
	for _, h := range items {
		if h.IsKaki {
			return true
		}
	}
	return false
}

func activeMuPriorityBonus(
	satPass bool,
	shaPass bool,
	noKaki bool,
	filterSat bool,
	filterSha bool,
	filterKaki bool,
) int {
	score := 0

	if filterSat {
		if satPass {
			score += 140
		} else {
			score -= 140
		}
	}
	if filterSha {
		if shaPass {
			score += 120
		} else {
			score -= 120
		}
	}
	if filterKaki {
		if noKaki {
			score += 80
		} else {
			score -= 80
		}
	}
	if filterSat && filterSha && satPass && shaPass {
		score += 45
	}

	return score
}

func thaiShapeBonus(keyword, candidate string) int {
	keyword = strings.TrimSpace(keyword)
	candidate = strings.TrimSpace(candidate)
	if keyword == "" || candidate == "" {
		return 0
	}

	prefix := sharedPrefixRunes(keyword, candidate)
	suffix := sharedSuffixRunes(keyword, candidate)
	bonus := 0

	switch {
	case suffix >= 3:
		bonus += 40
	case suffix == 2:
		bonus += 22
	case suffix == 1:
		bonus += 8
	}

	switch {
	case prefix >= 3:
		bonus += 24
	case prefix == 2:
		bonus += 12
	case prefix == 1:
		bonus += 4
	}

	if utf8.RuneCountInString(candidate) <= 2 {
		bonus -= 8
	}

	return bonus
}

func sharedPrefixRunes(a, b string) int {
	ar := []rune(a)
	br := []rune(b)
	n := minInt(len(ar), len(br))
	count := 0
	for i := 0; i < n; i++ {
		if ar[i] != br[i] {
			break
		}
		count++
	}
	return count
}

func sharedSuffixRunes(a, b string) int {
	ar := []rune(a)
	br := []rune(b)
	i := len(ar) - 1
	j := len(br) - 1
	count := 0
	for i >= 0 && j >= 0 {
		if ar[i] != br[j] {
			break
		}
		count++
		i--
		j--
	}
	return count
}

func minInt(a, b int) int {
	if a < b {
		return a
	}
	return b
}

func nameNearnessBonus(rootScore float64) int {
	if rootScore <= 0 {
		return 0
	}

	switch {
	case rootScore >= 0.75:
		return 26
	case rootScore >= 0.58:
		return 18
	case rootScore >= 0.42:
		return 10
	case rootScore >= 0.30:
		return 4
	case rootScore < 0.10:
		return -95
	case rootScore < 0.15:
		return -70
	case rootScore < 0.22:
		return -40
	case rootScore < 0.28:
		return -18
	default:
		return 0
	}
}

func phoneticRankingBonus(
	phoneticScore, pronunciationEase, euphonyScore, rhythmScore *int,
) int {
	if phoneticScore == nil && pronunciationEase == nil && euphonyScore == nil && rhythmScore == nil {
		return 0
	}

	score := safeInt(phoneticScore)
	ease := safeInt(pronunciationEase)
	euphony := safeInt(euphonyScore)
	rhythm := safeInt(rhythmScore)

	switch {
	case score >= 94 && ease >= 94 && euphony >= 92 && rhythm >= 90:
		return 22
	case score >= 88 && ease >= 88:
		return 14
	case score >= 80 && ease >= 82:
		return 8
	case score < 58 || ease < 60:
		return -22
	case score < 68 || ease < 70:
		return -12
	default:
		return 0
	}
}

func shouldExcludeBrokenPronunciation(
	relaxLevel int,
	doubleGoodMode bool,
	phoneticScore, pronunciationEase, euphonyScore, rhythmScore *int,
) bool {
	if doubleGoodMode && relaxLevel >= 2 {
		return false
	}
	if phoneticScore == nil && pronunciationEase == nil && euphonyScore == nil && rhythmScore == nil {
		return false
	}

	score := safeInt(phoneticScore)
	ease := safeInt(pronunciationEase)
	euphony := safeInt(euphonyScore)
	rhythm := safeInt(rhythmScore)

	switch relaxLevel {
	case 0:
		if score < 42 || ease < 42 {
			return true
		}
		if score < 48 && ease < 48 {
			return true
		}
		if score < 52 && euphony < 45 && rhythm < 45 {
			return true
		}
	case 1:
		if score < 35 || ease < 35 {
			return true
		}
		if score < 40 && ease < 40 && euphony < 38 {
			return true
		}
	case 2:
		return false
	}
	return false
}

func calculatePhoneticPenalty(score, ease, euphony, rhythm int) float64 {
	penalty := 0.0

	if score < 42 || ease < 42 {
		penalty += 25
	} else if score < 48 && ease < 48 {
		penalty += 10
	}

	return penalty
}

func phoneticSoftPenalty(
	phoneticScore, pronunciationEase, euphonyScore, rhythmScore *int,
	summary string,
	strong bool,
) int {
	score := safeInt(phoneticScore)
	ease := safeInt(pronunciationEase)
	euphony := safeInt(euphonyScore)
	rhythm := safeInt(rhythmScore)

	penalty := 0.0
	if score >= 35 && score < 42 {
		penalty += (42.0 - float64(score)) * 1.4
	}
	if ease >= 35 && ease < 42 {
		penalty += (42.0 - float64(ease)) * 1.4
	}
	if score < 35 {
		penalty += 12 + (35.0-float64(score))*1.5
	}
	if ease < 35 {
		penalty += 12 + (35.0-float64(ease))*1.5
	}
	if euphony < 30 && rhythm < 30 {
		penalty += 10
	}

	s := strings.TrimSpace(strings.ToLower(summary))
	if strings.Contains(s, "สะดุดมาก") || strings.Contains(s, "อ่านยาก") || strings.Contains(s, "ติดขัด") {
		penalty += 10
	}
	if strong {
		penalty *= 1.5
	}
	if penalty <= 0 {
		return 0
	}
	return -int(math.Round(math.Min(36, penalty)))
}

func shouldExcludeByPhoneticSummary(summary string) bool {
	s := strings.TrimSpace(strings.ToLower(summary))
	if s == "" {
		return false
	}

	badHints := []string{"สะดุดมาก", "ไม่ธรรมชาติ", "ติดขัด", "อ่านยาก", "สับสน"}

	for _, hint := range badHints {
		if strings.Contains(s, hint) {
			return true
		}
	}

	return false
}

func safeInt(value *int) int {
	if value == nil {
		return 0
	}
	return *value
}

func normalizedMeaningKey(meaning string) string {
	meaning = strings.TrimSpace(strings.ToLower(meaning))
	replacer := strings.NewReplacer(
		"\n", " ",
		"\r", " ",
		"\t", " ",
		"  ", " ",
	)
	for {
		next := replacer.Replace(meaning)
		if next == meaning {
			break
		}
		meaning = next
	}
	return meaning
}

func pairTypeTierBonus(pairType string) int {
	switch strings.ToUpper(strings.TrimSpace(pairType)) {
	case "D10":
		return 14
	case "D8":
		return 8
	case "D5":
		return 3
	default:
		return 0
	}
}

func pairPointRankBonus(pairPoint int) int {
	switch {
	case pairPoint >= 80:
		return 20
	case pairPoint >= 65:
		return 14
	case pairPoint >= 50:
		return 9
	case pairPoint >= 30:
		return 5
	case pairPoint >= 10:
		return 2
	case pairPoint <= -20:
		return -8
	case pairPoint < 0:
		return -4
	default:
		return 0
	}
}

func pairPointNormalized(pairPoint int) float64 {
	switch {
	case pairPoint >= 80:
		return 1.0
	case pairPoint >= 65:
		return 0.88
	case pairPoint >= 50:
		return 0.76
	case pairPoint >= 30:
		return 0.62
	case pairPoint >= 10:
		return 0.52
	case pairPoint >= 0:
		return 0.42
	case pairPoint >= -20:
		return 0.24
	default:
		return 0.08
	}
}

func pairTypeRank(pairType string) int {
	switch strings.ToUpper(strings.TrimSpace(pairType)) {
	case "D10":
		return 3
	case "D8":
		return 2
	case "D5":
		return 1
	default:
		return 0
	}
}

func isHardRejectedPairPoints(satPairPoint, shaPairPoint int) bool {
	return satPairPoint <= -30 && shaPairPoint <= -30
}

func passesRequestedFilters(result MobileNameResult, req MobileSearchRequest) bool {
	return passesRequestedFiltersWithStage(result, req, nil)
}

func passesRequestedFiltersWithStage(result MobileNameResult, req MobileSearchRequest, stage *doubleGoodStageConfig) bool {
	satPass, shaPass, _, _, satPairPoint, shaPairPoint := activeNumerologySignals(result, req.SimilarMode && req.Lastname != "")

	if stage != nil {
		if stage.DisableNumerology {
			return !isHardRejectedPairPoints(satPairPoint, shaPairPoint)
		}
		if stage.AllowEitherGood {
			if isHardRejectedPairPoints(satPairPoint, shaPairPoint) {
				return false
			}
			return satPass || shaPass
		}
	}

	switch {
	case req.FilterSat && req.FilterSha:
		if !(satPass && shaPass) {
			log.Printf("[VALIDATION] mode=double name=%s satPass=%v shaPass=%v valid=false", result.Name, satPass, shaPass)
		}
		return satPass && shaPass
	case req.FilterSat && !req.FilterSha:
		if !(satPass && !shaPass) {
			log.Printf("[VALIDATION] mode=sat_only name=%s satPass=%v shaPass=%v valid=false", result.Name, satPass, shaPass)
		}
		return satPass && !shaPass
	case !req.FilterSat && req.FilterSha:
		if !(!satPass && shaPass) {
			log.Printf("[VALIDATION] mode=sha_only name=%s satPass=%v shaPass=%v valid=false", result.Name, satPass, shaPass)
		}
		return !satPass && shaPass
	default:
		return true
	}
}

func shouldExcludeInvalidCandidate(result MobileNameResult) bool {
	name := strings.TrimSpace(result.Name)
	meaning := strings.TrimSpace(result.Meaning)
	return name == "" || meaning == "" || utf8.RuneCountInString(name) < 2
}

func shouldExcludeExtremeNumerology(result MobileNameResult, showMatching bool) bool {
	satPass, shaPass, satPairType, shaPairType, satPairPoint, shaPairPoint := activeNumerologySignals(result, showMatching)
	if isHardRejectedPairPoints(satPairPoint, shaPairPoint) {
		return true
	}
	if satPass || shaPass {
		return false
	}
	if pairTypeRank(satPairType) > 0 || pairTypeRank(shaPairType) > 0 {
		return false
	}
	return isHardRejectedPairPoints(satPairPoint, shaPairPoint)
}

func clampScore(value float64) float64 {
	if math.IsNaN(value) || math.IsInf(value, 0) {
		return 0
	}
	switch {
	case value < 0:
		return 0
	case value > 100:
		return 100
	default:
		return value
	}
}

func normalizedNumerologyScore(
	satPass bool,
	shaPass bool,
	satPairType string,
	shaPairType string,
	satPairPoint int,
	shaPairPoint int,
	kakiBonus int,
	lengthBonus int,
	muPriorityBonus int,
) float64 {
	score := 18.0
	if satPass {
		score += 18
	}
	if shaPass {
		score += 18
	}
	if satPass && shaPass {
		score += 12
	}
	score += float64(pairTypeRank(satPairType)+pairTypeRank(shaPairType)) * 6
	score += (pairPointNormalized(satPairPoint) + pairPointNormalized(shaPairPoint)) * 12
	score += float64(kakiBonus) * 0.6
	score += float64(lengthBonus) * 0.5
	if muPriorityBonus > 0 {
		score += math.Min(16, float64(muPriorityBonus)*0.06)
	} else if muPriorityBonus < 0 {
		score += math.Max(-18, float64(muPriorityBonus)*0.05)
	}
	return clampScore(score)
}

func normalizedSemanticScore(semanticScore float64, rootScore float64, hasKeywordSignal bool, shapeBonus int, nameNearBonus int) float64 {
	score := clampScore(semanticScore * 100)
	if hasKeywordSignal {
		score = score*0.58 + clampScore(rootScore*100)*0.32 + clampScore(float64(shapeBonus)+50)*0.10
	}
	if nameNearBonus != 0 {
		score += float64(nameNearBonus) * 0.45
	}
	return clampScore(score)
}

func lengthScoreMultiplier(nameLen int) float64 {
	mult := 1.80 - float64(nameLen)*0.13
	if mult > 1.60 {
		mult = 1.60
	}
	if mult < 0.10 {
		mult = 0.10
	}
	return mult
}

func computeHumanNameScore(name string) float64 {
	nameLen := len([]rune(strings.TrimSpace(name)))
	lengthScore := 0.0
	switch {
	case nameLen <= 3:
		lengthScore = 1.0
	case nameLen == 4:
		lengthScore = 0.9
	case nameLen == 5:
		lengthScore = 0.75
	case nameLen == 6:
		lengthScore = 0.55
	case nameLen == 7:
		lengthScore = 0.35
	case nameLen == 8:
		lengthScore = 0.15
	default:
		lengthScore = 0.0
	}

	naturalnessScore := 1.0
	clusterCount := thaiConsonantClusterCount(name)
	switch {
	case clusterCount >= 4:
		naturalnessScore -= 0.25
	case clusterCount >= 3:
		naturalnessScore -= 0.15
	case clusterCount >= 2:
		naturalnessScore -= 0.08
	}
	if hasRepeatedComplexPattern(name) {
		naturalnessScore -= 0.18
	}
	if looksPhraseLikeName(name) {
		naturalnessScore -= 0.15
	}

	score := lengthScore*0.7 + clamp01(naturalnessScore)*0.3
	if nameLen >= 9 {
		score *= 0.5
	} else if nameLen >= 8 {
		score *= 0.7
	}
	return clamp01(score)
}

func clamp01(value float64) float64 {
	switch {
	case value < 0:
		return 0
	case value > 1:
		return 1
	default:
		return value
	}
}

func thaiConsonantClusterCount(name string) int {
	clusters := 0
	runLength := 0
	for _, r := range name {
		if isThaiConsonant(r) {
			runLength++
			if runLength == 2 {
				clusters++
			}
			continue
		}
		runLength = 0
	}
	return clusters
}

func isThaiConsonant(r rune) bool {
	return r >= 'ก' && r <= 'ฮ' && r != 'ฤ' && r != 'ฦ'
}

func hasRepeatedComplexPattern(name string) bool {
	runes := []rune(strings.TrimSpace(name))
	if len(runes) < 6 {
		return false
	}
	seen := map[string]int{}
	for size := 2; size <= 3; size++ {
		for i := 0; i+size <= len(runes); i++ {
			pattern := string(runes[i : i+size])
			seen[pattern]++
			if seen[pattern] >= 2 && thaiConsonantClusterCount(pattern) > 0 {
				return true
			}
		}
	}
	return false
}

func looksPhraseLikeName(name string) bool {
	trimmed := strings.TrimSpace(name)
	if strings.ContainsAny(trimmed, " -_/") {
		return true
	}
	nameLen := len([]rune(trimmed))
	return nameLen >= 8 && thaiConsonantClusterCount(trimmed) >= 3
}

func normalizedPhoneticScore(
	phoneticScore *int,
	pronunciationEase *int,
	euphonyScore *int,
	rhythmScore *int,
	summary string,
) float64 {
	if phoneticScore == nil && pronunciationEase == nil && euphonyScore == nil && rhythmScore == nil {
		return 68
	}
	score := float64(safeInt(phoneticScore))*0.40 +
		float64(safeInt(pronunciationEase))*0.30 +
		float64(safeInt(euphonyScore))*0.18 +
		float64(safeInt(rhythmScore))*0.12

	summary = strings.TrimSpace(strings.ToLower(summary))
	switch {
	case strings.Contains(summary, "ลื่น") || strings.Contains(summary, "ธรรมชาติ"):
		score += 4
	case strings.Contains(summary, "สะดุด") || strings.Contains(summary, "แข็ง"):
		score -= 8
	case strings.Contains(summary, "แปลก"):
		score -= 10
	}

	return clampScore(score)
}

func sanitizeFloat(value float64) float64 {
	if math.IsNaN(value) || math.IsInf(value, 0) {
		return 0
	}
	return value
}

func sanitizeMobileResult(result *MobileNameResult) {
	result.Distance = sanitizeFloat(result.Distance)
	result.RootScore = sanitizeFloat(result.RootScore)
	result.SemanticScore = sanitizeFloat(result.SemanticScore)
	result.HybridScore = sanitizeFloat(result.HybridScore)
	result.BonusCalculated = sanitizeFloat(result.BonusCalculated)
}

func isEliteCandidate(result MobileNameResult, showMatching bool) bool {
	_, _, _, _, satPairPoint, shaPairPoint := activeNumerologySignals(result, showMatching)
	phonetic := clampScore(
		normalizedPhoneticScore(
			result.PhoneticScore,
			result.PronunciationEase,
			result.EuphonyScore,
			result.RhythmScore,
			result.PhoneticSummary,
		) + float64(result.PhoneticPenalty),
	)
	return result.NumerologyScore > 80 && satPairPoint > 0 && shaPairPoint > 0 && phonetic > 65
}

func prioritizeEliteTopResult(results []MobileNameResult, showMatching bool) {
	if len(results) < 2 || isEliteCandidate(results[0], showMatching) {
		return
	}
	bestIdx := -1
	bestScore := -1
	for i := 1; i < len(results); i++ {
		if !isEliteCandidate(results[i], showMatching) {
			continue
		}
		score := results[i].FinalRankScore + results[i].NumerologyScore
		if score > bestScore {
			bestScore = score
			bestIdx = i
		}
	}
	if bestIdx > 0 {
		results[0], results[bestIdx] = results[bestIdx], results[0]
	}
}

func applyTopLengthValidationBoost(results []MobileNameResult) bool {
	topTenLimit := minInt(len(results), 10)
	shortTopTen := 0
	for i := 0; i < topTenLimit; i++ {
		if len([]rune(results[i].Name)) <= 6 {
			shortTopTen++
		}
	}

	topThreeLimit := minInt(len(results), 3)
	longInTopThree := false
	for i := 0; i < topThreeLimit; i++ {
		if len([]rune(results[i].Name)) >= 9 {
			longInTopThree = true
			break
		}
	}

	if shortTopTen >= 5 && !longInTopThree {
		return false
	}

	boosted := false
	for i := range results {
		if len([]rune(results[i].Name)) <= 6 && results[i].FinalRankScore < 100 {
			results[i].FinalRankScore = int(clampScore(float64(results[i].FinalRankScore) + 2))
			boosted = true
		}
	}
	return boosted
}

// MobileSearchHandler handles POST /api/v1/name-search
func MobileSearchHandler(w http.ResponseWriter, r *http.Request) {
	setMobileCORSHeaders(w)
	handlerStart := time.Now()
	defer func() {
		if rec := recover(); rec != nil {
			log.Printf("MobileSearchHandler panic recovered: %v", rec)
			jsonResponse(w, http.StatusInternalServerError, MobileSearchResponse{
				Success: false,
				Error:   map[string]any{"message": "เกิดข้อผิดพลาดชั่วคราว กรุณาลองใหม่อีกครั้ง"},
				Results: []MobileNameResult{},
				Total:   0,
			})
		}
	}()
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusNoContent)
		return
	}
	if r.Method != http.MethodPost {
		jsonResponse(w, http.StatusMethodNotAllowed, MobileSearchResponse{Success: false, Error: map[string]any{"message": "Method not allowed"}, Results: []MobileNameResult{}, Total: 0})
		return
	}
	log.Printf("MobileSearchHandler: request received")

	var req MobileSearchRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		jsonResponse(w, http.StatusBadRequest, MobileSearchResponse{Success: false, Error: map[string]any{"message": "Invalid JSON body"}, Results: []MobileNameResult{}, Total: 0})
		return
	}

	searchTimeout := 55 * time.Second
	if req.FilterSat || req.FilterSha {
		searchTimeout = 55 * time.Second
	}
	if req.FilterSat && req.FilterSha {
		searchTimeout = 110 * time.Second
	}
	ctx, cancel := context.WithTimeout(r.Context(), searchTimeout)
	defer cancel()

	_, err := database.DB.ExecContext(
		ctx,
		fmt.Sprintf(
			"SET LOCAL statement_timeout = '%ds'",
			int(searchTimeout.Seconds()),
		),
	)
	if err != nil {
		log.Printf("Failed to set statement_timeout: %v", err)
	}
	if _, err := database.DB.ExecContext(ctx, "SET LOCAL work_mem = '256MB'"); err != nil {
		log.Printf("Failed to set work_mem: %v", err)
	}

	log.Printf("Search param: Keyword='%s', Meaning='%s'", req.Keyword, req.SemanticMeaning)
	cacheKey := makeMobileSearchCacheKey(req)
	if cachedResp, ok := getCachedMobileSearchResponse(cacheKey); ok {
		log.Printf("name-search cache hit key=%s", cacheKey)
		jsonResponse(w, http.StatusOK, cachedResp)
		log.Printf("MobileSearchHandler total elapsed=%s (cache hit)", time.Since(handlerStart))
		return
	}

	var meaningDB string
	if req.SemanticMeaning == "" && req.Keyword != "" {
		// Fallback: If frontend didn't send semantic meaning, fetch it from DB using the keyword
		err := database.DB.QueryRowContext(ctx, "SELECT meaning FROM names_miracle WHERE thname = $1 LIMIT 1", req.Keyword).Scan(&meaningDB)
		if err == nil && meaningDB != "" {
			req.SemanticMeaning = meaningDB
			log.Printf("Auto-fetched Semantic Meaning: '%s'", req.SemanticMeaning)
		}
	}

	searchContext := services.BuildMeaningSearchContext(
		services.MeaningSearchPipelineInput{
			Keyword:         req.Keyword,
			Lastname:        req.Lastname,
			SemanticMeaning: req.SemanticMeaning,
			MeaningIntent:   req.MeaningIntent,
			EntryMode:       req.EntryMode,
			SimilarMode:     req.SimilarMode,
			Limit:           req.Limit,
		},
		meaningDB,
	)

	if searchContext.WordContext == "" &&
		searchContext.MeaningContext == "" &&
		!searchContext.IsBroadSearch {
		jsonResponse(w, http.StatusOK, MobileSearchResponse{Success: true, Results: []MobileNameResult{}, Total: 0})
		return
	}

	limit := req.Limit
	if limit <= 0 || limit > 100 {
		limit = 50
	}

	// 1. Get Numerology for Lastname
	lastnameSat := 0
	lastnameSha := 0
	if req.Lastname != "" {
		res, err := services.DecodeName(req.Lastname, "")
		if err == nil {
			lastnameSat = res.TotalSat
			lastnameSha = res.TotalSha
		}
	}

	// 2. Get Good Sums
	goodSums, err := services.GetGoodSums()
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, MobileSearchResponse{Success: false, Error: map[string]any{"message": "Failed to calculate good sums"}, Results: []MobileNameResult{}, Total: 0})
		return
	}
	goodSumMap := make(map[int]bool)
	for _, sum := range goodSums {
		goodSumMap[sum] = true
	}

	// 2.5 Get pair metadata for granular coloring and ranking
	pairTypeMap, _ := services.GetPairTypesMap()
	pairPointMap, _ := services.GetPairPointsMap()

	targetSatSums := []int{}
	targetShaSums := []int{}
	seedCacheKey := ""
	useSeedCache := strings.TrimSpace(req.Keyword) != "" && strings.TrimSpace(req.SemanticMeaning) != ""
	if useSeedCache {
		seedCacheKey = makeSeedAnalysisCacheKey(req, lastnameSat, lastnameSha)
		if cachedSeed, ok := getCachedSeedAnalysis(seedCacheKey); ok {
			targetSatSums = cachedSeed.targetSatSums
			targetShaSums = cachedSeed.targetShaSums
			log.Printf("[PERF] seed analysis cache hit sat=%d sha=%d", len(targetSatSums), len(targetShaSums))
		}
	}

	if len(targetSatSums) == 0 || len(targetShaSums) == 0 {
		if req.SimilarMode && req.Lastname != "" {
			for _, s := range goodSums {
				if s > lastnameSat {
					baseSat := s - lastnameSat
					if !req.FilterSat || goodSumMap[baseSat] {
						targetSatSums = append(targetSatSums, baseSat)
					}
				}
				if s > lastnameSha {
					baseSha := s - lastnameSha
					if !req.FilterSha || goodSumMap[baseSha] {
						targetShaSums = append(targetShaSums, baseSha)
					}
				}
			}
		} else {
			targetSatSums = goodSums
			targetShaSums = goodSums
		}
		if useSeedCache {
			setCachedSeedAnalysis(seedCacheKey, targetSatSums, targetShaSums)
			log.Printf("[PERF] seed analysis cache store sat=%d sha=%d", len(targetSatSums), len(targetShaSums))
		}
	}

	if len(targetSatSums) == 0 {
		targetSatSums = []int{-1}
	}
	if len(targetShaSums) == 0 {
		targetShaSums = []int{-1}
	}

	// 3. Prepare Kaki filter & Highlight map
	dayMap := map[string]string{
		"Sunday": "k_sunday", "Monday": "k_monday", "Tuesday": "k_tuesday",
		"Wednesday1": "k_wednesday1", "Wednesday2": "k_wednesday2",
		"Thursday": "k_thursday", "Friday": "k_friday", "Saturday": "k_saturday",
	}
	kakiColumn := dayMap[req.Day]

	kakiCharMap := make(map[string]bool)
	var kakiCharList []string
	if req.Day != "" {
		dayThaiMap := map[string]string{
			"Sunday": "อาทิตย์", "Monday": "จันทร์", "Tuesday": "อังคาร",
			"Wednesday1": "พุธกลางวัน", "Wednesday2": "พุธกลางคืน",
			"Thursday": "พฤหัสบดี", "Friday": "ศุกร์", "Saturday": "เสาร์",
		}
		rows, err := database.DB.QueryContext(
			ctx,
			"SELECT kakis FROM kakis_day WHERE LOWER(day) = LOWER($1) OR day_th = $2",
			req.Day, dayThaiMap[req.Day],
		)
		if err == nil {
			defer rows.Close()
			for rows.Next() {
				var k string
				if rows.Scan(&k) == nil {
					k = strings.TrimSpace(k)
					for _, r := range k {
						ch := string(r)
						if !kakiCharMap[ch] {
							kakiCharMap[ch] = true
							kakiCharList = append(kakiCharList, ch)
						}
					}
				}
			}
		}
	}

	// 4. Get Embedding
	// Fast path for keyword-led searches (no explicit meaning intent):
	// skip external embedding call to reduce latency.
	doubleGoodMode := req.FilterSat && req.FilterSha
	if doubleGoodMode && searchContext.CandidateLimit < 200 {
		searchContext.CandidateLimit = 200
	}
	fastKeywordMode := searchContext.HasKeywordSignal &&
		strings.TrimSpace(req.MeaningIntent) == "" &&
		strings.TrimSpace(req.SemanticMeaning) == ""
	if doubleGoodMode {
		fastKeywordMode = true
	}
	embedding := make([]float64, 1536)
	if !fastKeywordMode {
		embedStart := time.Now()
		embedding, err = services.GetEmbedding(searchContext.MeaningContext)
		if err != nil {
			log.Printf("OpenAI embedding failed: %v", err)
			embedding = make([]float64, 1536)
		}
		log.Printf("MobileSearchHandler embedding elapsed=%s", time.Since(embedStart))
	}
	log.Printf(
		"MeaningContext: '%s', Strategy='%s', FastKeywordMode=%v, EmbeddingPreview: %v",
		searchContext.MeaningContext,
		searchContext.RetrievalStrategy,
		fastKeywordMode,
		previewEmbedding(embedding, 5),
	)

	buildQuery := func(relaxFilters bool, pool string, dgStage *doubleGoodStageConfig) (string, []interface{}) {
		isBroadSearch := searchContext.IsBroadSearch
		keywordLen := utf8.RuneCountInString(req.Keyword)

		if pool == "spiritual" && doubleGoodMode && searchContext.HasKeywordSignal {
			selectCols := "name_id, thname, meaning, gender, sat_sum, sha_sum, " +
				searchContext.SelectProjection() +
				"0.0 as bonus_calculated, phonetic_summary, phonetic_score, pronunciation_ease, euphony_score, rhythm_score"
			commonWhere := "char_length(thname) BETWEEN 2 AND 8"
			args := []interface{}{
				formatVector(embedding),
				searchContext.WordContext,
				pq.Array(targetSatSums),
				pq.Array(targetShaSums),
			}
			argCounter := 5
			if req.FilterKaki && kakiColumn != "" {
				commonWhere += fmt.Sprintf(" AND %s = false", kakiColumn)
			}
			if req.Lastname != "" {
				commonWhere += fmt.Sprintf(" AND thname != $%d", argCounter)
				args = append(args, req.Lastname)
				argCounter++
			}
			exactBoost := ""
			if req.Keyword != "" && !req.SimilarMode {
				exactBoost = fmt.Sprintf(" OR thname = $%d", argCounter)
				args = append(args, req.Keyword)
				argCounter++
			}
			minTrigram := 0.002
			if keywordLen <= 3 {
				minTrigram = 0.0005
			}
			if dgStage != nil && dgStage.MinTrigram > 0 {
				minTrigram = dgStage.MinTrigram
			}
			limitVal := searchContext.CandidateLimit
			if limitVal < 500 {
				limitVal = 500
			}
			if keywordLen <= 3 && limitVal < 600 {
				limitVal = 600
			}
			branchLimit := 300
			if keywordLen <= 3 {
				branchLimit = 360
			}
			query := fmt.Sprintf(`
				SELECT name_id, thname, meaning, gender, sat_sum, sha_sum,
				       distance, root_score, semantic_score, hybrid_score,
				       bonus_calculated, phonetic_summary, phonetic_score,
				       pronunciation_ease, euphony_score, rhythm_score
				FROM (
					(SELECT %s, 2 AS _prio
					 FROM names_miracle
					 WHERE %s
					   AND ((sat_sum = ANY($3::int[]) AND sha_sum = ANY($4::int[]))%s)
					 LIMIT %d)
					UNION ALL
					(SELECT %s, 1 AS _prio
					 FROM names_miracle
					 WHERE %s
					   AND GREATEST(COALESCE(similarity(thname, $2), 0), COALESCE(word_similarity(thname, $2), 0)) >= %g
					 LIMIT %d)
				) spiritual_union
				ORDER BY _prio DESC, char_length(thname) ASC
				LIMIT %d`,
				selectCols,
				commonWhere,
				exactBoost,
				branchLimit,
				selectCols,
				commonWhere,
				minTrigram,
				branchLimit,
				limitVal,
			)
			log.Printf("DB Query pool=%s relax=%v union=true: %s, Args: %+v", pool, relaxFilters, query, args)
			return query, args
		}

		query := `
			SELECT name_id, thname, meaning, gender,
			       sat_sum, sha_sum, `
		query += searchContext.SelectProjection()
		query += `0.0 as bonus_calculated,
		       phonetic_summary, phonetic_score, pronunciation_ease, euphony_score, rhythm_score
			FROM names_miracle 
			WHERE 1=1
		`
		if doubleGoodMode {
			query += " AND char_length(thname) BETWEEN 2 AND 8 "
		}
		args := []interface{}{formatVector(embedding)}
		argCounter := 2
		if searchContext.HasKeywordSignal {
			args = append(args, searchContext.WordContext)
			argCounter = 3
		}

		applyHardMu := req.SimilarMode || (!doubleGoodMode && (req.FilterSat || req.FilterSha))
		var satCond, shaCond string
		if (req.FilterSat || req.SimilarMode) && applyHardMu {
			satCond = fmt.Sprintf("sat_sum = ANY($%d::int[])", argCounter)
			args = append(args, pq.Array(targetSatSums))
			argCounter++
		}
		if (req.FilterSha || req.SimilarMode) && applyHardMu {
			shaCond = fmt.Sprintf("sha_sum = ANY($%d::int[])", argCounter)
			args = append(args, pq.Array(targetShaSums))
			argCounter++
		}

		filterCond := ""
		if satCond != "" && shaCond != "" {
			// For double-good staged fallback we switch between strict AND and
			// relaxed OR explicitly per stage.
			if dgStage != nil {
				if dgStage.AllowEitherGood {
					filterCond = "(" + satCond + " OR " + shaCond + ")"
				} else {
					filterCond = "(" + satCond + " AND " + shaCond + ")"
				}
			} else if req.FilterSat && req.FilterSha {
				filterCond = "(" + satCond + " AND " + shaCond + ")"
			} else if req.FilterSat {
				filterCond = satCond
			} else if req.FilterSha {
				filterCond = shaCond
			} else if req.SimilarMode && (!req.FilterSat || !req.FilterSha) {
				filterCond = "(" + satCond + " OR " + shaCond + ")"
			} else if relaxFilters {
				filterCond = "(" + satCond + " OR " + shaCond + ")"
			} else {
				filterCond = "(" + satCond + " AND " + shaCond + ")"
			}
		} else if satCond != "" {
			filterCond = satCond
		} else if shaCond != "" {
			filterCond = shaCond
		}

		if filterCond != "" {
			if req.Keyword != "" && !req.SimilarMode {
				query += " AND (" + filterCond + " OR thname = $" + fmt.Sprintf("%d", argCounter) + ")"
				args = append(args, req.Keyword)
				argCounter++
			} else {
				query += " AND (" + filterCond + ")"
			}
		}

		if req.FilterKaki && kakiColumn != "" {
			query += fmt.Sprintf(" AND %s = false", kakiColumn)
		}

		if req.Lastname != "" {
			query += " AND thname != $" + fmt.Sprintf("%d", argCounter)
			args = append(args, req.Lastname)
			argCounter++
		}

		if searchContext.HasKeywordSignal {
			switch pool {
			case "exact_prefix":
				query += " AND (thname = $" + fmt.Sprintf("%d", argCounter) + " OR thname ILIKE $" + fmt.Sprintf("%d", argCounter+1) + ")"
				args = append(args, req.Keyword, req.Keyword+"%")
				argCounter += 2
			case "trigram":
				minTrigram := 0.02
				if relaxFilters {
					minTrigram = 0.005
				}
				if doubleGoodMode {
					minTrigram = 0.002
					if utf8.RuneCountInString(req.Keyword) <= 3 {
						minTrigram = 0.0005
					}
				}
				if dgStage != nil {
					minTrigram = dgStage.MinTrigram
				}
				query += fmt.Sprintf(" AND GREATEST(COALESCE(similarity(thname, $2), 0), COALESCE(word_similarity(thname, $2), 0)) >= %g", minTrigram)
			}
		}

		if isBroadSearch {
			query += " ORDER BY name_id DESC "
		} else {
			switch pool {
			case "exact_prefix":
				if searchContext.HasKeywordSignal {
					query += " ORDER BY CASE WHEN thname = $2 THEN 3 WHEN thname ILIKE $2 || '' || '%' THEN 2 ELSE 1 END DESC, GREATEST(COALESCE(similarity(thname, $2), 0), COALESCE(word_similarity(thname, $2), 0)) DESC, char_length(thname) ASC "
				} else {
					query += " ORDER BY " + searchContext.OrderByExpression() + " "
				}
			case "trigram":
				if searchContext.HasKeywordSignal {
					query += " ORDER BY GREATEST(COALESCE(similarity(thname, $2), 0), COALESCE(word_similarity(thname, $2), 0)) DESC "
					if !fastKeywordMode {
						query += ", (1 - COALESCE(meaning_vector <=> $1, 1)) DESC "
					}
				} else {
					query += " ORDER BY " + searchContext.OrderByExpression() + " "
				}
			case "semantic":
				query += " ORDER BY (1 - COALESCE(meaning_vector <=> $1, 1)) DESC "
			case "spiritual":
				spiritualTerms := []string{}
				if req.FilterSat || req.SimilarMode {
					spiritualTerms = append(spiritualTerms, "CASE WHEN sat_sum = ANY("+fmt.Sprintf("$%d::int[]", 3)+") THEN 1 ELSE 0 END")
				}
				if req.FilterSha || req.SimilarMode {
					shaArg := 4
					if !(req.FilterSat || req.SimilarMode) {
						shaArg = 3
					}
					spiritualTerms = append(spiritualTerms, "CASE WHEN sha_sum = ANY("+fmt.Sprintf("$%d::int[]", shaArg)+") THEN 1 ELSE 0 END")
				}
				if len(spiritualTerms) == 0 {
					query += " ORDER BY " + searchContext.OrderByExpression() + " "
				} else {
					query += " ORDER BY (" + strings.Join(spiritualTerms, " + ") + ") DESC, sat_sum DESC, sha_sum DESC, " + searchContext.OrderByExpression() + " "
				}
			default:
				query += " ORDER BY " + searchContext.OrderByExpression() + " "
			}
		}

		limitVal := searchContext.CandidateLimit
		switch pool {
		case "exact_prefix":
			if limitVal < 80 {
				limitVal = 80
			}
			if doubleGoodMode && limitVal < 120 {
				limitVal = 120
			}
			if doubleGoodMode && keywordLen <= 3 && limitVal < 240 {
				limitVal = 240
			}
		case "trigram":
			if limitVal < 140 {
				limitVal = 140
			}
			if doubleGoodMode && limitVal < 500 {
				limitVal = 500
			}
			if doubleGoodMode && keywordLen <= 3 && limitVal < 600 {
				limitVal = 600
			}
		case "semantic":
			limitVal = 50
		case "spiritual":
			if limitVal < 180 {
				limitVal = 180
			}
			if doubleGoodMode && limitVal < 500 {
				limitVal = 500
			}
			if doubleGoodMode && keywordLen <= 3 && limitVal < 600 {
				limitVal = 600
			}
		default:
			if req.FilterSat || req.FilterSha || req.FilterKaki {
				if limitVal < 220 {
					limitVal = 220
				}
			}
		}
		if relaxFilters && limitVal < 320 {
			limitVal = 320
		}
		if doubleGoodMode && limitVal < 500 {
			limitVal = 500
		}
		if doubleGoodMode && keywordLen <= 3 && limitVal < 600 {
			limitVal = 600
		}
		query += fmt.Sprintf(" LIMIT %d", limitVal)

		log.Printf("DB Query pool=%s relax=%v: %s, Args: %+v", pool, relaxFilters, query, args)
		return query, args
	}

	runAndScanWithContext := func(queryCtx context.Context, stage string, pool string, relax bool, dgStage *doubleGoodStageConfig) ([]MobileNameResult, retrievalScanStats, error) {
		query, args := buildQuery(relax, pool, dgStage)
		rows, err := database.DB.QueryContext(queryCtx, query, args...)
		if err != nil {
			return nil, retrievalScanStats{Stage: stage, Pool: pool}, err
		}
		defer rows.Close()

		stats := retrievalScanStats{Stage: stage, Pool: pool}
		var out []MobileNameResult
		for rows.Next() {
			stats.RawRows++
			var r MobileNameResult
			var id int
			var phoneticSummary string
			var phoneticScore, pronunciationEase, euphonyScore, rhythmScore *int
			if err := rows.Scan(
				&id,
				&r.Name,
				&r.Meaning,
				&r.Gender,
				&r.SatSum,
				&r.ShaSum,
				&r.Distance,
				&r.RootScore,
				&r.SemanticScore,
				&r.HybridScore,
				&r.BonusCalculated,
				&phoneticSummary,
				&phoneticScore,
				&pronunciationEase,
				&euphonyScore,
				&rhythmScore,
			); err != nil {
				continue
			}
			r.PhoneticSummary = strings.TrimSpace(phoneticSummary)
			r.PhoneticScore = phoneticScore
			r.PronunciationEase = pronunciationEase
			r.EuphonyScore = euphonyScore
			r.RhythmScore = rhythmScore

			relaxLevel := 0
			strongSoftPenalty := false
			if relax {
				relaxLevel = 1
			}
			if dgStage != nil {
				relaxLevel = dgStage.PhoneticRelaxLevel
				strongSoftPenalty = dgStage.StrongSoftPenalty
			}

			if shouldExcludeInvalidCandidate(r) {
				continue
			}
			if shouldExcludeBrokenPronunciation(
				relaxLevel,
				doubleGoodMode,
				r.PhoneticScore,
				r.PronunciationEase,
				r.EuphonyScore,
				r.RhythmScore,
			) {
				stats.PhoneticFiltered++
				continue
			}
			if relaxLevel < 2 && shouldExcludeByPhoneticSummary(r.PhoneticSummary) {
				stats.PhoneticFiltered++
				continue
			}
			r.IsSatGood = goodSumMap[r.SatSum]
			r.IsShaGood = goodSumMap[r.ShaSum]

			if req.Lastname != "" {
				r.TotalSat = r.SatSum + lastnameSat
				r.TotalSha = r.ShaSum + lastnameSha
				r.IsTotalSatGood = goodSumMap[r.TotalSat]
				r.IsTotalShaGood = goodSumMap[r.TotalSha]

				if pairTypeMap != nil {
					r.TotalSatPairType = pairTypeMap[fmt.Sprintf("%d", r.TotalSat)]
					r.TotalShaPairType = pairTypeMap[fmt.Sprintf("%d", r.TotalSha)]
				}
				if pairPointMap != nil {
					r.TotalSatPairPoint = pairPointMap[fmt.Sprintf("%d", r.TotalSat)]
					r.TotalShaPairPoint = pairPointMap[fmt.Sprintf("%d", r.TotalSha)]
				}
			}

			if pairTypeMap != nil {
				r.SatPairType = pairTypeMap[fmt.Sprintf("%d", r.SatSum)]
				r.ShaPairType = pairTypeMap[fmt.Sprintf("%d", r.ShaSum)]
			}
			if pairPointMap != nil {
				r.SatPairPoint = pairPointMap[fmt.Sprintf("%d", r.SatSum)]
				r.ShaPairPoint = pairPointMap[fmt.Sprintf("%d", r.ShaSum)]
			}
			if r.IsSatGood && r.IsShaGood {
				stats.SatShaCount++
			}
			if dgStage != nil && !dgStage.DisableNumerology && isHardRejectedPairPoints(r.SatPairPoint, r.ShaPairPoint) {
				continue
			}
			if shouldExcludeExtremeNumerology(r, req.SimilarMode && req.Lastname != "") {
				continue
			}
			if dgStage != nil && dgStage.RelaxedNumerology {
				r.IsRelaxedNumerology = true
			}
			if relaxLevel == 2 {
				r.PhoneticPenalty = phoneticSoftPenalty(
					r.PhoneticScore,
					r.PronunciationEase,
					r.EuphonyScore,
					r.RhythmScore,
					r.PhoneticSummary,
					strongSoftPenalty,
				)
			}

			// Highlight Kaki: Individual codepoints (runes).
			// We no longer group them because the App will handle shaping with CustomPainter.
			if len(kakiCharMap) > 0 {
				for _, rn := range r.Name {
					ch := string(rn)
					r.KakiHighlight = append(r.KakiHighlight, CharHighlight{
						Char:   ch,
						IsKaki: kakiCharMap[ch],
					})
				}
			}

			// Calculate bonus score based on frontend logic
			r.BonusCalculated = calculateBonus(r.Name, r.IsSatGood, r.IsShaGood, r.IsTotalSatGood, r.IsTotalShaGood, r.KakiHighlight)
			r.FinalRankScore, r.RankReasons = calculateFinalRankScoreAndReasons(
				&r,
				req.Keyword,
				req.SimilarMode && req.Lastname != "",
				req.FilterSat,
				req.FilterSha,
				req.FilterKaki,
				searchContext.HasKeywordSignal,
			)
			sanitizeMobileResult(&r)

			out = append(out, r)
			stats.Accepted++
		}
		if err := rows.Err(); err != nil {
			if queryCtx.Err() != nil {
				log.Printf("[TIMEOUT] stage=%s pool=%s partial=%d", stage, pool, len(out))
				if len(out) > 0 {
					return out, stats, nil
				}
				return out, stats, queryCtx.Err()
			}
			return out, stats, err
		}
		return out, stats, nil
	}

	runAndScan := func(stage string, pool string, relax bool, dgStage *doubleGoodStageConfig) ([]MobileNameResult, retrievalScanStats, error) {
		return runAndScanWithContext(ctx, stage, pool, relax, dgStage)
	}

	runDesperateSearchWithContext := func(queryCtx context.Context, stage string, relaxLevel int, strongSoftPenalty bool) ([]MobileNameResult, retrievalScanStats, error) {
		query := "SELECT name_id, thname, meaning, gender, sat_sum, sha_sum, " +
			searchContext.SelectProjection() +
			"0.0 as bonus_calculated, phonetic_summary, phonetic_score, pronunciation_ease, euphony_score, rhythm_score FROM names_miracle WHERE 1=1"
		args := []interface{}{formatVector(embedding)}
		argCounter := 2
		if searchContext.HasKeywordSignal {
			args = append(args, searchContext.WordContext)
			argCounter = 3
		}

		// Gender filter removed per user request

		if req.FilterKaki && kakiColumn != "" {
			query += fmt.Sprintf(" AND %s = false", kakiColumn)
		}

		// DESPERATE LEVEL: Ignore Sat/Sha but keep Kaki/Gender

		if req.Lastname != "" {
			// Exclude the exact matching name/surname from being recommended as the first name
			query += " AND thname != $" + fmt.Sprintf("%d", argCounter)
			args = append(args, req.Lastname)
			argCounter++
		}

		limitVal := searchContext.CandidateLimit
		if req.FilterSat || req.FilterSha || req.FilterKaki {
			if limitVal < 220 {
				limitVal = 220
			}
		}
		if limitVal < 320 {
			limitVal = 320
		}
		if doubleGoodMode && limitVal < 1200 {
			limitVal = 1200
		}
		query += fmt.Sprintf(
			" ORDER BY %s LIMIT %d",
			searchContext.OrderByExpression(),
			limitVal,
		)

		rows, err := database.DB.QueryContext(queryCtx, query, args...)
		if err != nil {
			return nil, retrievalScanStats{Stage: stage, Pool: "fallback"}, err
		}
		defer rows.Close()

		stats := retrievalScanStats{Stage: stage, Pool: "fallback"}
		var out []MobileNameResult
		for rows.Next() {
			stats.RawRows++
			var r MobileNameResult
			var id int
			var phoneticSummary string
			var phoneticScore, pronunciationEase, euphonyScore, rhythmScore *int
			if err := rows.Scan(
				&id,
				&r.Name,
				&r.Meaning,
				&r.Gender,
				&r.SatSum,
				&r.ShaSum,
				&r.Distance,
				&r.RootScore,
				&r.SemanticScore,
				&r.HybridScore,
				&r.BonusCalculated,
				&phoneticSummary,
				&phoneticScore,
				&pronunciationEase,
				&euphonyScore,
				&rhythmScore,
			); err != nil {
				continue
			}
			r.PhoneticSummary = strings.TrimSpace(phoneticSummary)
			r.PhoneticScore = phoneticScore
			r.PronunciationEase = pronunciationEase
			r.EuphonyScore = euphonyScore
			r.RhythmScore = rhythmScore

			if shouldExcludeInvalidCandidate(r) {
				continue
			}
			if shouldExcludeBrokenPronunciation(
				relaxLevel,
				doubleGoodMode,
				r.PhoneticScore,
				r.PronunciationEase,
				r.EuphonyScore,
				r.RhythmScore,
			) {
				continue
			}
			if relaxLevel < 2 && shouldExcludeByPhoneticSummary(r.PhoneticSummary) {
				continue
			}
			r.IsSatGood = goodSumMap[r.SatSum]
			r.IsShaGood = goodSumMap[r.ShaSum]

			if req.Lastname != "" {
				r.TotalSat = r.SatSum + lastnameSat
				r.TotalSha = r.ShaSum + lastnameSha
				r.IsTotalSatGood = goodSumMap[r.TotalSat]
				r.IsTotalShaGood = goodSumMap[r.TotalSha]

				if pairTypeMap != nil {
					r.TotalSatPairType = pairTypeMap[fmt.Sprintf("%d", r.TotalSat)]
					r.TotalShaPairType = pairTypeMap[fmt.Sprintf("%d", r.TotalSha)]
				}
				if pairPointMap != nil {
					r.TotalSatPairPoint = pairPointMap[fmt.Sprintf("%d", r.TotalSat)]
					r.TotalShaPairPoint = pairPointMap[fmt.Sprintf("%d", r.TotalSha)]
				}
			}

			if pairTypeMap != nil {
				r.SatPairType = pairTypeMap[fmt.Sprintf("%d", r.SatSum)]
				r.ShaPairType = pairTypeMap[fmt.Sprintf("%d", r.ShaSum)]
			}
			if pairPointMap != nil {
				r.SatPairPoint = pairPointMap[fmt.Sprintf("%d", r.SatSum)]
				r.ShaPairPoint = pairPointMap[fmt.Sprintf("%d", r.ShaSum)]
			}
			if shouldExcludeExtremeNumerology(r, req.SimilarMode && req.Lastname != "") {
				continue
			}
			if relaxLevel == 2 {
				r.PhoneticPenalty = phoneticSoftPenalty(
					r.PhoneticScore,
					r.PronunciationEase,
					r.EuphonyScore,
					r.RhythmScore,
					r.PhoneticSummary,
					strongSoftPenalty,
				)
			}

			// Highlight Kaki: Individual codepoints (runes).
			if len(kakiCharMap) > 0 {
				for _, rn := range r.Name {
					ch := string(rn)
					r.KakiHighlight = append(r.KakiHighlight, CharHighlight{
						Char:   ch,
						IsKaki: kakiCharMap[ch],
					})
				}
			}

			// Calculate bonus score based on frontend logic
			r.BonusCalculated = calculateBonus(r.Name, r.IsSatGood, r.IsShaGood, r.IsTotalSatGood, r.IsTotalShaGood, r.KakiHighlight)
			r.FinalRankScore, r.RankReasons = calculateFinalRankScoreAndReasons(
				&r,
				req.Keyword,
				req.SimilarMode && req.Lastname != "",
				req.FilterSat,
				req.FilterSha,
				req.FilterKaki,
				searchContext.HasKeywordSignal,
			)
			sanitizeMobileResult(&r)

			out = append(out, r)
			stats.Accepted++
		}
		if err := rows.Err(); err != nil {
			if queryCtx.Err() != nil {
				log.Printf("[TIMEOUT] stage=%s pool=%s partial=%d", stage, stats.Pool, len(out))
				if len(out) > 0 {
					return out, stats, nil
				}
				return out, stats, queryCtx.Err()
			}
			return out, stats, err
		}
		return out, stats, nil
	}

	runDesperateSearch := func(stage string, relaxLevel int, strongSoftPenalty bool) ([]MobileNameResult, retrievalScanStats, error) {
		return runDesperateSearchWithContext(ctx, stage, relaxLevel, strongSoftPenalty)
	}

	var results []MobileNameResult
	targetUniqueResults := 0

	appendRelaxedDoubleGoodTopUp := func(incoming []MobileNameResult, strongSoftPenalty bool) {
		if len(incoming) == 0 {
			return
		}
		missingSlots := 50 - len(results)
		if missingSlots <= 0 {
			return
		}
		filteredTopUp := make([]MobileNameResult, 0, len(incoming))
		for _, result := range incoming {
			satPass, shaPass, _, _, satPairPoint, shaPairPoint := activeNumerologySignals(result, req.SimilarMode && req.Lastname != "")
			if isHardRejectedPairPoints(satPairPoint, shaPairPoint) {
				continue
			}
			if satPass || shaPass {
				result.IsRelaxedNumerology = true
				result.PhoneticPenalty = phoneticSoftPenalty(
					result.PhoneticScore,
					result.PronunciationEase,
					result.EuphonyScore,
					result.RhythmScore,
					result.PhoneticSummary,
					strongSoftPenalty,
				)
				result.FinalRankScore, result.RankReasons = calculateFinalRankScoreAndReasons(
					&result,
					req.Keyword,
					req.SimilarMode && req.Lastname != "",
					req.FilterSat,
					req.FilterSha,
					req.FilterKaki,
					searchContext.HasKeywordSignal,
				)
				filteredTopUp = append(filteredTopUp, result)
				if len(filteredTopUp) >= missingSlots {
					break
				}
			}
		}
		results, _ = mergeUniqueResultsWithCount(results, filteredTopUp)
	}

	stageCounters := []RetrievalStageMeta{}
	appendStage := func(stats retrievalScanStats) {
		stageCounters = append(stageCounters, RetrievalStageMeta{
			Stage:            stats.Stage,
			Pool:             stats.Pool,
			RawRows:          stats.RawRows,
			Accepted:         stats.Accepted,
			PhoneticFiltered: stats.PhoneticFiltered,
			SatShaCount:      stats.SatShaCount,
			UniqueTotal:      len(results),
		})
		logRetrievalStage(stats, len(results))
	}

	runParallelPools := func(stage string, pools []string, relax bool, dgStage *doubleGoodStageConfig) error {
		if len(pools) == 0 {
			return nil
		}
		stageStart := time.Now()
		poolCtx, poolCancel := context.WithCancel(ctx)
		defer poolCancel()

		resultCh := make(chan poolResult, len(pools))
		var wg sync.WaitGroup
		var dedupe sync.Map
		var stateMu sync.Mutex
		for _, item := range results {
			dedupe.Store(item.Name, struct{}{})
		}

		for _, pool := range pools {
			if pool == "semantic" {
				stateMu.Lock()
				alreadyEnough := len(results) >= 80
				stateMu.Unlock()
				if alreadyEnough {
					log.Printf("[PERF] stage=%s pool=%s skipped current_results=%d", stage, pool, len(results))
					continue
				}
			}
			poolName := pool
			wg.Add(1)
			go func() {
				defer wg.Done()
				poolStart := time.Now()
				poolResults, stats, runErr := runAndScanWithContext(poolCtx, stage, poolName, relax, dgStage)
				log.Printf(
					"[PERF] stage=%s pool=%s duration=%s results=%d err=%v",
					stage,
					poolName,
					time.Since(poolStart),
					len(poolResults),
					runErr,
				)
				resultCh <- poolResult{Pool: poolName, Results: poolResults, Stats: stats, Err: runErr}
			}()
		}

		go func() {
			wg.Wait()
			close(resultCh)
		}()

		var timeoutErr error
		for res := range resultCh {
			if res.Err != nil {
				if ctx.Err() != nil {
					timeoutErr = res.Err
					log.Printf("[TIMEOUT] stage=%s pool=%s err=%v partial=%d", stage, res.Pool, res.Err, len(results))
					poolCancel()
				} else {
					log.Printf("%s pool=%s failed: %v", stage, res.Pool, res.Err)
				}
			}

			added := 0
			stateMu.Lock()
			for _, item := range res.Results {
				if _, exists := dedupe.LoadOrStore(item.Name, struct{}{}); exists {
					continue
				}
				results = append(results, item)
				added++
			}
			appendStage(res.Stats)
			total := len(results)
			if total >= targetUniqueResults {
				poolCancel()
			}
			stateMu.Unlock()

			log.Printf(
				"[PERF] stage=%s pool=%s added=%d total=%d duration=%s",
				stage,
				res.Pool,
				added,
				total,
				time.Since(stageStart),
			)

			if total >= targetUniqueResults {
				log.Printf("[PERF] target hit, skipping remaining stages stage=%s total=%d target=%d", stage, total, targetUniqueResults)
			}
		}
		if timeoutErr != nil && len(results) == 0 {
			return timeoutErr
		}
		log.Printf("[PERF] stage=%s duration=%s results=%d", stage, time.Since(stageStart), len(results))
		return nil
	}

	finalizeAfterTimeout := func(reason string) {
		if len(results) > 0 {
			log.Printf("[TIMEOUT] %s finalize with partial results=%d", reason, len(results))
			return
		}

		log.Printf("[TIMEOUT] %s no results; running final 5s desperate search", reason)
		shortCtx, shortCancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer shortCancel()
		if _, err := database.DB.ExecContext(shortCtx, "SET LOCAL statement_timeout = '5s'"); err != nil {
			log.Printf("Failed to set short statement_timeout: %v", err)
		}
		fallbackResults, stats, runErr := runDesperateSearchWithContext(shortCtx, "timeout_final_desperate", 2, true)
		if runErr != nil {
			log.Printf("[TIMEOUT] final desperate search failed: %v", runErr)
			return
		}
		results, _ = mergeUniqueResultsWithCount(results, fallbackResults)
		appendStage(stats)
	}

	minResults := 100
	if req.Limit > minResults {
		minResults = req.Limit
	}
	targetUniqueResults = minResults
	if doubleGoodMode && targetUniqueResults < 200 {
		targetUniqueResults = 200
	}

	// We use the package-level DB. If you want transaction isolation for ef_search,
	// you would need to pass tx into runAndScan. Here we just set it globally or skip it.
	// For simplicity, we fallback to standard execution.
	strictPools := []string{"hybrid", "semantic"}
	if req.FilterSat || req.FilterSha || req.FilterKaki {
		strictPools = []string{"spiritual", "trigram", "hybrid", "semantic"}
		if doubleGoodMode {
			strictPools = []string{"spiritual", "trigram", "exact_prefix", "hybrid", "semantic"}
		}
	}
	if searchContext.HasKeywordSignal {
		strictPools = []string{"trigram", "exact_prefix", "hybrid", "semantic"}
		if req.FilterSat || req.FilterSha || req.FilterKaki {
			strictPools = []string{"spiritual", "trigram", "exact_prefix", "hybrid", "semantic"}
		}
		if fastKeywordMode {
			strictPools = []string{"trigram", "exact_prefix", "hybrid", "semantic"}
			if req.FilterSat || req.FilterSha || req.FilterKaki {
				strictPools = []string{"spiritual", "trigram", "exact_prefix", "hybrid", "semantic"}
			}
		}
	}

	if doubleGoodMode {
		dgStart := time.Now()
		doubleGoodStages := []doubleGoodStageConfig{
			{
				Name:               "stage_1",
				MinTrigram:         0.003,
				PhoneticRelaxLevel: 1,
				AllowEitherGood:    true,
			},
		}
		stagePools := []string{"trigram", "exact_prefix"}
		if !searchContext.HasKeywordSignal {
			stagePools = []string{"trigram"}
		}
		for idx, cfg := range doubleGoodStages {
			if time.Since(dgStart) > searchTimeout-5*time.Second {
				log.Printf(
					"[PERF] cumulative timeout reached, break with %d results",
					len(results),
				)
				break
			}
			stageCfg := cfg
			if runErr := runParallelPools(stageCfg.Name, stagePools, stageCfg.PhoneticRelaxLevel > 0, &stageCfg); runErr != nil {
				if ctx.Err() != nil {
					finalizeAfterTimeout(stageCfg.Name)
					goto finalizeResults
				}
			}
			if len(results) >= targetUniqueResults || (len(results) >= 80 && idx <= 1) {
				log.Printf("[PERF] target hit, skipping remaining double-good stages results=%d target=%d", len(results), targetUniqueResults)
				break
			}
		}
		if len(results) < minResults {
			topUpStart := time.Now()
			topUpResults, stats, runErr := runDesperateSearch("double_good_topup", 2, true)
			if runErr != nil {
				if ctx.Err() != nil {
					log.Printf("double-good top-up timed out: %v", runErr)
					finalizeAfterTimeout("double_good_topup")
					goto finalizeResults
				}
				log.Printf("double-good top-up failed: %v", runErr)
			} else {
				appendRelaxedDoubleGoodTopUp(topUpResults, true)
				appendStage(stats)
			}
			log.Printf("[PERF] stage=%s results=%d time=%v", "double_good_topup", len(results), time.Since(topUpStart))
			if len(results) >= 100 {
				goto finalizeResults
			}
		}
	} else {
		if runErr := runParallelPools("strict", strictPools, false, nil); runErr != nil {
			if ctx.Err() != nil {
				finalizeAfterTimeout("strict")
				goto finalizeResults
			}
		}
		if len(results) >= 100 {
			log.Printf("[PERF] target hit, skipping remaining stages results=%d", len(results))
			goto finalizeResults
		}
		log.Printf("Strict union results: %d", len(results))
		if len(results) == 0 {
			log.Printf("Strict union empty, running fallback strict search")
			fallbackResults, stats, runErr := runDesperateSearch("strict_fallback", 0, false)
			if runErr != nil {
				if ctx.Err() != nil {
					log.Printf("strict fallback timed out: %v", runErr)
					finalizeAfterTimeout("strict_fallback")
					goto finalizeResults
				}
			}
			results, _ = mergeUniqueResultsWithCount(results, fallbackResults)
			appendStage(stats)
			if len(results) >= 100 {
				goto finalizeResults
			}
		}
	}

	if !doubleGoodMode && len(results) < minResults {
		relaxedPools := []string{"hybrid", "semantic"}
		if req.FilterSat || req.FilterSha || req.FilterKaki {
			relaxedPools = []string{"spiritual", "trigram", "hybrid", "semantic"}
			if doubleGoodMode {
				relaxedPools = []string{"spiritual", "trigram", "exact_prefix", "hybrid", "semantic"}
			}
		}
		if searchContext.HasKeywordSignal {
			relaxedPools = []string{"trigram", "exact_prefix", "hybrid", "semantic"}
			if req.FilterSat || req.FilterSha || req.FilterKaki {
				relaxedPools = []string{"spiritual", "trigram", "exact_prefix", "hybrid", "semantic"}
				if doubleGoodMode {
					relaxedPools = []string{"spiritual", "trigram", "exact_prefix", "hybrid", "semantic"}
				}
			}
			if fastKeywordMode {
				relaxedPools = []string{"trigram", "hybrid", "semantic"}
				if req.FilterSat || req.FilterSha || req.FilterKaki {
					relaxedPools = []string{"spiritual", "trigram", "hybrid", "semantic"}
					if doubleGoodMode {
						relaxedPools = []string{"spiritual", "trigram", "exact_prefix", "hybrid", "semantic"}
					}
				}
			}
		}
		for _, pool := range relaxedPools {
			if pool == "semantic" && len(results) >= 80 {
				continue
			}
			poolStart := time.Now()
			poolResults, stats, runErr := runAndScan("relaxed", pool, true, nil)
			if runErr != nil {
				if ctx.Err() != nil {
					log.Printf("relaxed pool=%s timed out: %v", pool, runErr)
					finalizeAfterTimeout("relaxed:" + pool)
					goto finalizeResults
				}
				log.Printf("relaxed pool=%s failed: %v", pool, runErr)
				continue
			}
			results, _ = mergeUniqueResultsWithCount(results, poolResults)
			appendStage(stats)
			log.Printf("[PERF] stage=%s results=%d time=%v", "relaxed:"+pool, len(results), time.Since(poolStart))
			if len(results) >= 100 {
				goto finalizeResults
			}
			if len(results) >= targetUniqueResults {
				break
			}
		}
	}
	if len(results) < minResults {
		fallbackStart := time.Now()
		fallbackResults, stats, runErr := runDesperateSearch("fallback", 0, false)
		if runErr != nil {
			if ctx.Err() != nil {
				log.Printf("fallback timed out: %v", runErr)
				finalizeAfterTimeout("fallback")
				goto finalizeResults
			}
		}
		results, _ = mergeUniqueResultsWithCount(results, fallbackResults)
		appendStage(stats)
		log.Printf("[PERF] stage=%s results=%d time=%v", "fallback", len(results), time.Since(fallbackStart))
		if len(results) >= 100 {
			goto finalizeResults
		}
	}

finalizeResults:
	if results == nil {
		results = []MobileNameResult{}
	}

	filteredResults := make([]MobileNameResult, 0, len(results))
	for _, result := range results {
		if passesRequestedFilters(result, req) {
			filteredResults = append(filteredResults, result)
		}
	}
	results = filteredResults

	needsSingleFilterTopUp := false

	needsSingleFilterTopUp = (req.FilterSat != req.FilterSha) && len(results) < minResults
	if needsSingleFilterTopUp {
		topUpStart := time.Now()
		topUpResults, stats, runErr := runDesperateSearch("post_filter_topup", 0, false)
		if runErr != nil {
			if ctx.Err() != nil {
				log.Printf("post-filter top-up timed out: %v", runErr)
				finalizeAfterTimeout("post_filter_topup")
				goto finalizeAndRespond
			}
			log.Printf("post-filter top-up failed: %v", runErr)
		} else {
			filteredTopUp := make([]MobileNameResult, 0, len(topUpResults))
			for _, result := range topUpResults {
				if passesRequestedFilters(result, req) {
					filteredTopUp = append(filteredTopUp, result)
				}
			}
			results, _ = mergeUniqueResultsWithCount(results, filteredTopUp)
			appendStage(stats)
		}
		log.Printf("[PERF] stage=%s results=%d time=%v", "post_filter_topup", len(results), time.Since(topUpStart))
	}

	if !doubleGoodMode && len(results) < 100 {
		safetyFillStart := time.Now()
		relaxedResults, stats, runErr := runDesperateSearch("safety_fill", 2, true)
		if runErr != nil {
			if ctx.Err() != nil {
				log.Printf("safety fill timed out: %v", runErr)
				finalizeAfterTimeout("safety_fill")
				goto finalizeAndRespond
			}
			log.Printf("safety fill failed: %v", runErr)
		} else {
			filteredRelaxed := make([]MobileNameResult, 0, len(relaxedResults))
			for _, result := range relaxedResults {
				if doubleGoodMode {
					satPass, shaPass, _, _, satPairPoint, shaPairPoint := activeNumerologySignals(result, req.SimilarMode && req.Lastname != "")
					if isHardRejectedPairPoints(satPairPoint, shaPairPoint) {
						continue
					}
					if !(satPass || shaPass) {
						continue
					}
					result.IsRelaxedNumerology = true
					result.PhoneticPenalty = phoneticSoftPenalty(
						result.PhoneticScore,
						result.PronunciationEase,
						result.EuphonyScore,
						result.RhythmScore,
						result.PhoneticSummary,
						true,
					)
					result.FinalRankScore, result.RankReasons = calculateFinalRankScoreAndReasons(
						&result,
						req.Keyword,
						req.SimilarMode && req.Lastname != "",
						req.FilterSat,
						req.FilterSha,
						req.FilterKaki,
						searchContext.HasKeywordSignal,
					)
				} else if !passesRequestedFilters(result, req) {
					continue
				}
				filteredRelaxed = append(filteredRelaxed, result)
				if len(filteredRelaxed) >= 100-len(results) {
					break
				}
			}
			results, _ = mergeUniqueResultsWithCount(results, filteredRelaxed)
			appendStage(stats)
		}
		log.Printf("[PERF] stage=%s results=%d time=%v", "safety_fill", len(results), time.Since(safetyFillStart))
	}

finalizeAndRespond:
	sortStart := time.Now()
	sortResults := func() {
		sort.SliceStable(results, func(i, j int) bool {
			noKakiI := len(results[i].KakiHighlight) == 0 || !containsKaki(results[i].KakiHighlight)
			noKakiJ := len(results[j].KakiHighlight) == 0 || !containsKaki(results[j].KakiHighlight)
			muI := activeMuPriorityBonus(
				results[i].IsSatGood,
				results[i].IsShaGood,
				noKakiI,
				req.FilterSat,
				req.FilterSha,
				req.FilterKaki,
			)
			muJ := activeMuPriorityBonus(
				results[j].IsSatGood,
				results[j].IsShaGood,
				noKakiJ,
				req.FilterSat,
				req.FilterSha,
				req.FilterKaki,
			)
			if muI != muJ {
				return muI > muJ
			}
			if searchContext.HasKeywordSignal {
				shapeI := thaiShapeBonus(req.Keyword, results[i].Name)
				shapeJ := thaiShapeBonus(req.Keyword, results[j].Name)
				if shapeI != shapeJ {
					return shapeI > shapeJ
				}
			}
			if searchContext.HasKeywordSignal && results[i].RootScore != results[j].RootScore {
				return results[i].RootScore > results[j].RootScore
			}
			mi := normalizedMeaningKey(results[i].Meaning)
			mj := normalizedMeaningKey(results[j].Meaning)
			if mi != "" && mi == mj && results[i].NumerologyScore != results[j].NumerologyScore {
				return results[i].NumerologyScore > results[j].NumerologyScore
			}
			if results[i].IsRelaxedNumerology != results[j].IsRelaxedNumerology {
				return !results[i].IsRelaxedNumerology
			}
			if results[i].FinalRankScore != results[j].FinalRankScore {
				return results[i].FinalRankScore > results[j].FinalRankScore
			}
			if !searchContext.HasKeywordSignal && results[i].RootScore != results[j].RootScore {
				return results[i].RootScore > results[j].RootScore
			}
			if results[i].NumerologyScore != results[j].NumerologyScore {
				return results[i].NumerologyScore > results[j].NumerologyScore
			}
			if results[i].SemanticScore != results[j].SemanticScore {
				return results[i].SemanticScore > results[j].SemanticScore
			}
			if results[i].HybridScore != results[j].HybridScore {
				return results[i].HybridScore > results[j].HybridScore
			}
			return results[i].Name < results[j].Name
		})
	}
	sortResults()
	prioritizeEliteTopResult(results, req.SimilarMode && req.Lastname != "")
	if applyTopLengthValidationBoost(results) {
		sortResults()
		prioritizeEliteTopResult(results, req.SimilarMode && req.Lastname != "")
	}

	if len(results) > 100 {
		results = results[:100]
	}
	log.Printf("MobileSearchHandler sort+trim elapsed=%s", time.Since(sortStart))

	// 5. Build Final Response
	resp := MobileSearchResponse{
		Success: true,
		Results: results,
		Total:   len(results),
		RetrievalMeta: &RetrievalMeta{
			EntryMode:         string(searchContext.EntryMode),
			RetrievalStrategy: searchContext.RetrievalStrategy,
			MeaningContext:    searchContext.MeaningContext,
			SemanticWeight:    searchContext.SemanticWeight,
			WordWeight:        searchContext.WordWeight,
			CandidateLimit:    searchContext.CandidateLimit,
			StageCounters:     stageCounters,
		},
	}

	if req.Day != "" {
		dayThaiMap := map[string]string{
			"Sunday": "อาทิตย์", "Monday": "จันทร์", "Tuesday": "อังคาร",
			"Wednesday1": "พุธกลางวัน", "Wednesday2": "พุธกลางคืน",
			"Thursday": "พฤหัสบดี", "Friday": "ศุกร์", "Saturday": "เสาร์",
		}
		descMap := map[string]string{
			"Sunday": "ศ ษ ส ห ฬ ฮ", "Monday": "สระทั้งหมด + ตัวการันต์",
			"Tuesday": "ก ข ค ฆ ง", "Wednesday1": "จ ฉ ช ซ ฌ ญ",
			"Wednesday2": "บ ป ผ ฝ พ ฟ ภ ม", "Thursday": "ด ต ถ ท ธ น",
			"Friday": "ย ร ล ว", "Saturday": "ฎ ฏ ฐ ฑ ฒ ณ",
		}
		resp.KakiInfo = &KakiInfo{
			Day:         req.Day,
			DayThai:     dayThaiMap[req.Day],
			Description: descMap[req.Day],
			KakiChars:   kakiCharList,
		}
	}

	jsonResponse(w, http.StatusOK, resp)
	setCachedMobileSearchResponse(cacheKey, resp)
	log.Printf("[PERF] stage=%s results=%d time=%v", "total", len(resp.Results), time.Since(handlerStart))
}

type retrievalScanStats struct {
	Stage            string
	Pool             string
	RawRows          int
	Accepted         int
	PhoneticFiltered int
	SatShaCount      int
}

func logRetrievalStage(stats retrievalScanStats, uniqueTotal int) {
	log.Printf(
		"name-search stage=%s pool=%s candidates=%d sat_sha=%d phonetic_filtered=%d accepted=%d unique=%d",
		stats.Stage,
		stats.Pool,
		stats.RawRows,
		stats.SatShaCount,
		stats.PhoneticFiltered,
		stats.Accepted,
		uniqueTotal,
	)
}

func mergeUniqueResultsWithCount(base []MobileNameResult, incoming []MobileNameResult) ([]MobileNameResult, int) {
	if len(incoming) == 0 {
		return base, 0
	}

	seen := make(map[string]struct{}, len(base)+len(incoming))
	for _, item := range base {
		seen[item.Name] = struct{}{}
	}

	added := 0
	for _, item := range incoming {
		if _, exists := seen[item.Name]; exists {
			continue
		}
		base = append(base, item)
		seen[item.Name] = struct{}{}
		added++
	}

	return base, added
}

func previewEmbedding(embedding []float64, size int) []float64 {
	if len(embedding) == 0 || size <= 0 {
		return []float64{}
	}
	if len(embedding) < size {
		size = len(embedding)
	}
	return embedding[:size]
}

func mergeUniqueResults(base []MobileNameResult, incoming []MobileNameResult) []MobileNameResult {
	if len(incoming) == 0 {
		return base
	}

	seen := make(map[string]struct{}, len(base)+len(incoming))
	for _, item := range base {
		seen[item.Name] = struct{}{}
	}

	for _, item := range incoming {
		if _, exists := seen[item.Name]; exists {
			continue
		}
		base = append(base, item)
		seen[item.Name] = struct{}{}
	}

	return base
}
