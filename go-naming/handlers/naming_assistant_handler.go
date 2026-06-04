package handlers

import (
	"bytes"
	"encoding/json"
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"net/http"
	"os"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/lib/pq"
)

const TYPHOON_CHAT_API_URL = "https://api.opentyphoon.ai/v1/chat/completions"
const TYPHOON_MODEL = "typhoon-v2.5-30b-a3b-instruct"

// NamingAssistantHandler handles naming consultation chat
func NamingAssistantHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	apiKey := os.Getenv("OPENAI_API_KEY")
	if apiKey == "" {
		http.Error(w, "API Configuration error", http.StatusInternalServerError)
		return
	}

	var req struct {
		Message  string              `json:"message"`
		GuestID  string              `json:"guest_id"`
		MemberID string              `json:"member_id"`
		History  []map[string]string `json:"history"`
		Context  struct {
			Day      string `json:"day"`
			Gender   string `json:"gender"`
			Keywords string `json:"keywords"`
		} `json:"context"` // Persistent context from frontend
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid JSON body", http.StatusBadRequest)
		return
	}

	userMessage := strings.TrimSpace(req.Message)
	if userMessage == "" {
		http.Error(w, "Message cannot be empty", http.StatusBadRequest)
		return
	}

	// DEBUG: Log received IDs
	fmt.Printf("DEBUG Naming: GuestID='%s', MemberID='%s', Message='%s'\n", req.GuestID, req.MemberID, userMessage)

	// Usage tracking only (no limit for naming chat)
	namingUsageId := "naming_" + req.GuestID
	if req.MemberID != "" {
		namingUsageId = "naming_member_" + req.MemberID
	}
	_, _ = database.DB.Exec(`INSERT INTO guest_usage (guest_id, message_count) VALUES ($1, 1) 
		ON CONFLICT (guest_id) DO UPDATE SET message_count = guest_usage.message_count + 1, last_used_at = CURRENT_TIMESTAMP`, namingUsageId)

	// Build context from history if available
	historyText := ""
	for _, m := range req.History {
		historyText += fmt.Sprintf("%s: %s\n", m["role"], m["content"])
	}

	// 1. Extract context: Use saved context from frontend as base, then extract new info
	prevDay := strings.TrimSpace(req.Context.Day)
	prevGender := strings.TrimSpace(req.Context.Gender)
	prevKeywords := strings.TrimSpace(req.Context.Keywords)

	extractionPrompt := fmt.Sprintf(`บทบาท: สกัดข้อมูลจากบทสนทนาทั้งหมด (ประวัติ + ข้อความล่าสุด)

ข้อมูลที่รู้แล้วจากก่อนหน้า:
- วันเกิด: "%s"
- เพศ: "%s"
- คำค้นหาความหมาย: "%s"

ประวัติการสนทนา:
%s
ข้อความล่าสุด: "%s"

ภารกิจ: สกัดข้อมูลสะสมทั้งหมด (รวมข้อมูลที่รู้แล้ว + ข้อมูลใหม่จากประวัติและข้อความล่าสุด)
1. "day": วันเกิด (จันทร์, อังคาร, พุธกลางวัน, พุธกลางคืน, พฤหัสบดี, ศุกร์, เสาร์, อาทิตย์) หรือค่าจากก่อนหน้าถ้ามี
2. "gender": เพศ ("ช" = ชาย, "ญ" = หญิง, "ค" = คละเพศ/ไม่ระบุ) หรือค่าจากก่อนหน้าถ้ามี
3. "keywords": คำค้นหาความหมายชื่อ (เช่น "ร่ำรวย", "ฉลาด", "เป็นที่รัก") ห้ามใส่คำว่า เกิด/ตั้งชื่อ/ลูก/ชาย/หญิง หรือค่าจากก่อนหน้าถ้ามี
สำคัญ: หากข้อมูลใดเคยรู้แล้วจากก่อนหน้า ให้คงค่าเดิมไว้ หากข้อความใหม่มีข้อมูลใหม่ให้เว้นว่าง
4. คืน JSON เท่านั้น:
{"day":"...", "gender":"...", "keywords":"..."}

ตอบ JSON:`, prevDay, prevGender, prevKeywords, historyText, userMessage)

	extractedData := struct {
		Keywords string `json:"keywords"`
		Day      string `json:"day"`
		Gender   string `json:"gender"`
	}{}

	jsonExtracted, _ := callOpenAISimple(apiKey, extractionPrompt)

	// Helper to strip markdown if AI sends it
	cleanJSON := func(s string) string {
		s = strings.TrimSpace(s)
		if strings.HasPrefix(s, "```") {
			s = strings.TrimPrefix(s, "```json")
			s = strings.TrimPrefix(s, "```")
			s = strings.TrimSuffix(s, "```")
			s = strings.TrimSpace(s)
		}
		return s
	}

	json.Unmarshal([]byte(cleanJSON(jsonExtracted)), &extractedData)

	// Clean up extracted day — only use if AI explicitly extracted it or from saved context
	cleanDay := strings.TrimSpace(extractedData.Day)
	cleanDay = strings.TrimPrefix(cleanDay, "วัน")
	cleanDay = strings.TrimSpace(cleanDay)

	// Use extracted gender (fallback to previous context)
	cleanGender := strings.TrimSpace(extractedData.Gender)
	if cleanGender == "" {
		cleanGender = prevGender
	}
	// Robust gender detection from message+history
	if cleanGender == "" {
		allText := historyText + " " + userMessage
		if strings.Contains(allText, "ลูกชาย") || strings.Contains(allText, "ผู้ชาย") {
			cleanGender = "ช"
		} else if strings.Contains(allText, "ลูกสาว") || strings.Contains(allText, "ผู้หญิง") {
			cleanGender = "ญ"
		}
	}

	fmt.Printf("DEBUG: Extracted Day='%s', Gender='%s', Keywords='%s'\n", cleanDay, cleanGender, extractedData.Keywords)

	// Map Thai Day to Column Name (only used if user explicitly provided birthday)
	dayToKakiColumn := map[string]string{
		"อาทิตย์":    "k_sunday",
		"จันทร์":     "k_monday",
		"อังคาร":     "k_tuesday",
		"พุธกลางวัน": "k_wednesday1",
		"พุธกลางคืน": "k_wednesday2",
		"พฤหัสบดี":   "k_thursday",
		"ศุกร์":      "k_friday",
		"เสาร์":      "k_saturday",
	}
	kakiColumn := dayToKakiColumn[cleanDay] // empty string if no birthday → no kalakini filter

	searchQuery := strings.TrimSpace(extractedData.Keywords)

	// Aggressive Filler Check: If keywords are just fillers or day names, clear them
	blacklist := []string{"เกิด", "ชื่อ", "ตั้งชื่อ", "มงคล", "หาชื่อ", "ลูก", "ชาย", "หญิง", "วันจันทร์", "วันอังคาร", "วันพุธ", "วันพฤหัสบดี", "วันศุกร์", "วันเสาร์", "วันอาทิตย์"}
	isOnlyFiller := true
	if searchQuery != "" {
		words := strings.Fields(searchQuery)
		for _, w := range words {
			found := false
			for _, b := range blacklist {
				if strings.Contains(w, b) {
					found = true
					break
				}
			}
			if !found {
				isOnlyFiller = false
				break
			}
		}
	}

	if isOnlyFiller {
		searchQuery = ""
	}

	if searchQuery == "" {
		// If no real keywords, clean the user message as fallback
		searchQuery = userMessage
		daysAndFillers := []string{
			"วันจันทร์", "วันอังคาร", "วันพุธกลางวัน", "วันพุธกลางคืน", "วันพุธ", "วันพฤหัสบดี", "วันศุกร์", "วันเสาร์", "วันอาทิตย์",
			"จันทร์", "อังคาร", "พุธ", "พฤหัส", "ศุกร์", "เสาร์", "อาทิตย์",
			"คนเกิด", "ผู้เกิด", "เกิด", "อยากได้ชื่อ", "ตั้งชื่อ", "ชื่อ", "ช่วยตั้ง", "หาชื่อ", "มงคล",
			"ของ", "สำหรับ", "ลูก", "ชาย", "หญิง", "ความหมาย", "แนว", "สไตล์", "เป็น", "เอา", "ครับ", "ค่ะ", "หน่อย",
		}
		for _, d := range daysAndFillers {
			searchQuery = strings.ReplaceAll(searchQuery, d, "")
		}
		searchQuery = strings.TrimSpace(searchQuery)
	}

	// Double check: If after all cleaning we still have nothing or just filler,
	// use a generic "auspicious" search instead of searching for the word "born"
	if len(searchQuery) < 2 {
		searchQuery = "เป็นสิริมงคล" // Default high-quality concept
	}

	// 2. Perform Semantic Search ONLY if Birthday is provided
	type PairDetail struct {
		Pair  string `json:"pair"`
		Grade string `json:"grade"`
		Label string `json:"label"`
		Desc  string `json:"desc"`
	}
	type NameResult struct {
		ID             int          `json:"id"`
		ThName         string       `json:"thname"`
		Meaning        string       `json:"meaning"`
		SatSum         int          `json:"sat_sum"`
		ShaSum         int          `json:"sha_sum"`
		SatPairs       []string     `json:"sat_pairs"`
		ShaPairs       []string     `json:"sha_pairs"`
		SatGrade       string       `json:"sat_grade"`
		ShaGrade       string       `json:"sha_grade"`
		IsGoodSat      bool         `json:"is_good_sat"`
		IsGoodSha      bool         `json:"is_good_sha"`
		SatDesc        string       `json:"sat_desc"`
		ShaDesc        string       `json:"sha_desc"`
		SatPairDetails []PairDetail `json:"sat_pair_details"`
		ShaPairDetails []PairDetail `json:"sha_pair_details"`
	}

	getGrade := func(pairs []string, meanings map[string]string) string {
		// Map meanings like "Good (D10)" back to "D10"
		rank := map[string]int{"R10": 0, "R7": 1, "R5": 2, "D5": 3, "D8": 4, "D10": 5}
		currentWorstRank := 10
		result := "D10"
		found := false

		for _, p := range pairs {
			if m, ok := meanings[p]; ok {
				// Extraction logic: "Good (D10)" -> "D10"
				start := strings.Index(m, "(")
				end := strings.Index(m, ")")
				pType := m
				if start != -1 && end != -1 {
					pType = strings.TrimSpace(m[start+1 : end])
				}

				if r, ok := rank[pType]; ok {
					found = true
					if r < currentWorstRank {
						currentWorstRank = r
						result = pType
					}
				}
			}
		}
		if !found {
			return "D5"
		} // Default for unknown
		return result
	}

	var nameResults []NameResult
	meanings, _ := services.GetPairMeaningsMap()
	descriptions, _ := services.GetPairDescriptionsMap()
	gradeThai := map[string]string{
		"D10": "ดีเยี่ยม", "D8": "ดีมาก", "D5": "ดี",
		"R10": "ร้ายมาก", "R7": "ร้าย", "R5": "ค่อนข้างร้าย",
	}

	// 🚀 Pre-compute good sums at DB level (sat_sum IN (...) AND sha_sum IN (...))
	goodSums, _ := services.GetGoodSums()
	goodSumsSQL := ""
	if len(goodSums) > 0 {
		parts := make([]string, len(goodSums))
		for i, s := range goodSums {
			parts[i] = strconv.Itoa(s)
		}
		goodSumsSQL = strings.Join(parts, ",")
	}
	fmt.Printf("DEBUG: Good sums count: %d\n", len(goodSums))

	// Helper: run vector search with good-sum filtering
	searchWithFilter := func(queryText string) []NameResult {
		embedding, err := services.GetEmbedding(queryText)
		if err != nil {
			fmt.Printf("DEBUG: Embedding Error: %v\n", err)
			return nil
		}
		vectorStr := formatVector(embedding)
		query := `
				SELECT name_id, thname, COALESCE(meaning, ''), satnum, shanum, sat_sum, sha_sum
				FROM names_miracle
				WHERE char_length(meaning) >= 25
				  AND meaning_vector IS NOT NULL
				  AND thname != 'เกิด'
			  AND thname != 'เกตุเกิด'
			  AND thname NOT LIKE '%ไม่ระบุ%'
			  AND thname NOT LIKE '%ขึ้นชื่อ%'
			  AND thname NOT LIKE '%การเปลี่ยนแปลง%'
			  AND length(thname) >= 2
			  AND thname ~ '^[^a-zA-Z]+$'
		`
		// 🚀 DB-level numerology filter: only good sat AND sha
		if goodSumsSQL != "" {
			query += fmt.Sprintf(" AND sat_sum IN (%s) AND sha_sum IN (%s) ", goodSumsSQL, goodSumsSQL)
		}
		if kakiColumn != "" {
			query += fmt.Sprintf(" AND %s = false ", kakiColumn)
		}
		if cleanGender == "ช" || cleanGender == "ญ" {
			query += fmt.Sprintf(" AND (gender = '%s' OR gender = 'ค') ", cleanGender)
		}
		query += " ORDER BY meaning_vector <=> $1 ASC LIMIT 10"

		rows, err := database.DB.Query(query, vectorStr)
		if err != nil {
			fmt.Printf("DEBUG: Database Query Error: %v\n", err)
			return nil
		}
		defer rows.Close()

		var results []NameResult
		for rows.Next() {
			var res NameResult
			var satArr, shaArr pq.StringArray
			if err := rows.Scan(&res.ID, &res.ThName, &res.Meaning, &satArr, &shaArr, &res.SatSum, &res.ShaSum); err == nil {
				res.SatPairs = services.CreatePairs(res.SatSum)
				res.SatGrade = getGrade(res.SatPairs, meanings)
				res.IsGoodSat = !strings.HasPrefix(res.SatGrade, "R")
				for _, p := range res.SatPairs {
					pd := PairDetail{Pair: p}
					if m, ok := meanings[p]; ok {
						start := strings.Index(m, "(")
						end := strings.Index(m, ")")
						if start != -1 && end != -1 {
							pd.Grade = strings.TrimSpace(m[start+1 : end])
						}
					}
					pd.Label = gradeThai[pd.Grade]
					if d, ok := descriptions[p]; ok {
						pd.Desc = d
					}
					res.SatPairDetails = append(res.SatPairDetails, pd)
				}
				res.SatDesc = ""
				for _, pd := range res.SatPairDetails {
					if pd.Desc != "" {
						res.SatDesc += pd.Desc + " "
					}
				}
				res.SatDesc = strings.TrimSpace(res.SatDesc)

				res.ShaPairs = services.CreatePairs(res.ShaSum)
				res.ShaGrade = getGrade(res.ShaPairs, meanings)
				res.IsGoodSha = !strings.HasPrefix(res.ShaGrade, "R")
				for _, p := range res.ShaPairs {
					pd := PairDetail{Pair: p}
					if m, ok := meanings[p]; ok {
						start := strings.Index(m, "(")
						end := strings.Index(m, ")")
						if start != -1 && end != -1 {
							pd.Grade = strings.TrimSpace(m[start+1 : end])
						}
					}
					pd.Label = gradeThai[pd.Grade]
					if d, ok := descriptions[p]; ok {
						pd.Desc = d
					}
					res.ShaPairDetails = append(res.ShaPairDetails, pd)
				}
				res.ShaDesc = ""
				for _, pd := range res.ShaPairDetails {
					if pd.Desc != "" {
						res.ShaDesc += pd.Desc + " "
					}
				}
				res.ShaDesc = strings.TrimSpace(res.ShaDesc)
				results = append(results, res)
			}
		}
		return results
	}

	// Primary search
	fmt.Printf("DEBUG: Starting search for '%s' day='%s' gender='%s'\n", searchQuery, cleanDay, cleanGender)
	candidates := searchWithFilter(searchQuery)

	// Sort by meaning length DESC (prefer descriptive names)
	sort.Slice(candidates, func(i, j int) bool {
		return len([]rune(candidates[i].Meaning)) > len([]rune(candidates[j].Meaning))
	})
	if len(candidates) > 3 {
		nameResults = candidates[:3]
	} else {
		nameResults = candidates
	}
	fmt.Printf("DEBUG: Primary search. Pool: %d, Selected: %d\n", len(candidates), len(nameResults))

	// Fallback if no results
	if len(nameResults) == 0 {
		fmt.Printf("DEBUG: No results for '%s', trying fallback...\n", searchQuery)
		fbCandidates := searchWithFilter("ชื่อมงคล ความหมายดี เจริญรุ่งเรือง")
		if len(fbCandidates) > 3 {
			nameResults = fbCandidates[:3]
		} else {
			nameResults = fbCandidates
		}
		fmt.Printf("DEBUG: Fallback results: %d\n", len(nameResults))
	}

	// 3. Construct Final Persona Response
	namesInfo := ""
	for _, n := range nameResults {
		// Build per-pair detail strings
		var satPairStrs []string
		for _, pd := range n.SatPairDetails {
			satPairStrs = append(satPairStrs, fmt.Sprintf("คู่ %s (%s) %s", pd.Pair, pd.Label, pd.Desc))
		}
		var shaPairStrs []string
		for _, pd := range n.ShaPairDetails {
			shaPairStrs = append(shaPairStrs, fmt.Sprintf("คู่ %s (%s) %s", pd.Pair, pd.Label, pd.Desc))
		}

		namesInfo += fmt.Sprintf("- %s: %s\n  เลขศาสตร์ รวม %d → %s\n  พลังเงา รวม %d → %s\n",
			n.ThName, n.Meaning,
			n.SatSum, strings.Join(satPairStrs, ", "),
			n.ShaSum, strings.Join(shaPairStrs, ", "))
	}

	// Build context summary for AI
	contextLines := []string{}
	if cleanDay != "" {
		contextLines = append(contextLines, fmt.Sprintf("ผู้ใช้เกิดวัน: %s (กรองชื่อกาลกิณีออกไปให้แล้ว)", cleanDay))
	}
	genderLabel := map[string]string{"ช": "ชาย", "ญ": "หญิง", "ค": "คละเพศ"}
	if gl, ok := genderLabel[cleanGender]; ok {
		contextLines = append(contextLines, fmt.Sprintf("เพศ: %s (กรองเฉพาะชื่อ%sแล้ว)", gl, gl))
	}
	contextSummary := strings.Join(contextLines, "\n")

	systemPrompt := `คุณคือ "คุณทญา" (Khun Taya) ที่ปรึกษาตั้งชื่อมงคลตามหลักเลขศาสตร์ชั้นสูง
ความเชี่ยวชาญ:
- เลขศาสตร์: ค่าตัวเลขของตัวอักษรไทยแต่ละตัว นำมารวมเป็นคู่ตัวเลข แล้วเทียบกับตำราเลขศาสตร์ คู่ที่ขึ้นต้นด้วย D (เช่น D10, D8, D5) คือดี คู่ที่ขึ้นต้นด้วย R (เช่น R10, R7, R5) คือร้าย
- พลังเงา: คำนวณเหมือนเลขศาสตร์แต่ใช้ตารางค่าเงา (shadow number) เป็นพลังแฝงที่ส่งเสริมชื่อ
- กาลกิณี: อักษรต้องห้ามตามวันเกิด ถ้ารู้วันเกิดจะกรองอักษรเหล่านี้ออกให้อัตโนมัติ

คำสั่งเฉพาะ:
1. หากมี "รายชื่อแนะนำ": ให้วิเคราะห์รายชื่อโดยละเอียด ระบุผลลัพธ์ "เลขศาสตร์" และ "พลังเงา" ควบคู่กันเสมอ
   * หากชื่อใดมีผลดีทั้งเลขศาสตร์และพลังเงา ให้ยกย่องเป็น "ชื่อมงคลดีเยี่ยม" 🌟
   * หากมีวันเกิด ให้บอกว่ากรองอักษรกาลกิณีออกแล้ว
2. หากไม่มีรายชื่อ: ให้ชวนคุยขอรายละเอียดเพิ่ม เช่น แนวความหมาย, เพศ
3. ห้ามถามซ้ำสิ่งที่รู้แล้ว ห้ามทักทาย ห้ามกล่าวสวัสดี ห้ามพูดเรื่องวันเกิดหรือกาลกิณีเอง (ระบบจะแจ้งเอง)
4. แทนตัวเองว่า "คุณทญา" ใช้ภาษาแชทนุ่มนวลตรงประเด็น (ใช้ "ค่ะ")
5. ห้ามใช้คำว่า "แม่หมอ" หรือ "ลูกค้า"`

	userPrompt := fmt.Sprintf(`ข้อมูลปัจจุบัน:
- ข้อความผู้ใช้: "%s"
%s
- รายชื่อแนะนำ:
%s`, userMessage, contextSummary, namesInfo)

	// Use OpenAI (GPT-4o) directly for reliability
	reply, err := callOpenAISimple(apiKey, systemPrompt+"\n\n"+userPrompt+"\n\nตอบกลับ:")
	if err != nil {
		fmt.Printf("OpenAI Error: %v\n", err)
		reply = "ขออภัยค่ะ ระบบกำลังประมวลผลข้อมูลจำนวนมาก รบกวนลองส่งข้อความใหม่อีกครั้งนะคะ"
	}

	// Clean up persona
	reply = strings.ReplaceAll(reply, "แม่หมอ", "คุณทญา")
	reply = strings.ReplaceAll(reply, "ลูกค้า", "คุณ")
	reply = strings.ReplaceAll(reply, "คุณนิน", "คุณทญา")

	// Append birthday prompt if day is still unknown (guaranteed, not relying on AI)
	if cleanDay == "" && len(nameResults) > 0 {
		reply = strings.TrimSpace(reply) + "\n\n📌 หากแจ้งวันเกิดด้วย คุณทญาจะกรองอักษรกาลกิณี (อักษรต้องห้ามตามวันเกิด) ออกให้ด้วย เพื่อให้ชื่อเป็นมงคลที่สุดค่ะ"
	}

	// Return extracted context for frontend to persist
	response := map[string]interface{}{
		"reply": strings.TrimSpace(reply),
		"data":  nameResults,
		"context": map[string]string{
			"day":      cleanDay,
			"gender":   cleanGender,
			"keywords": extractedData.Keywords,
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetNameRootHandler analyzes the etymology of a name
func GetNameRootHandler(w http.ResponseWriter, r *http.Request) {
	name := r.URL.Query().Get("name")
	meaning := r.URL.Query().Get("meaning") // Receive meaning context

	if name == "" {
		http.Error(w, "Name is required", http.StatusBadRequest)
		return
	}

	apiKey := os.Getenv("TYPHOON_API_KEY")
	if apiKey == "" {
		// Fallback to OpenAI if Typhoon key is missing
		apiKey = os.Getenv("OPENAI_API_KEY")
		prompt := fmt.Sprintf(`วิเคราะห์รากศัพท์ของชื่อ "%s"
บริบทความหมายที่ต้องการสื่อ: "%s"

คำสั่ง:
1. อธิบายว่าชื่อนี้มาจากคำบาลี/สันสกฤต หรือคำไทยคำไหนผสมกัน
2. พยายามเชื่อมโยงให้สอดคล้องกับ "บริบทความหมายที่ต้องการสื่อ" (ถ้าบริบทมีเหตุผล)
3. ตอบสั้นๆ กระชับ เข้าใจง่าย
4. ตอบเป็นภาษาไทยเท่านั้น`, name, meaning)
		explanation, err := callOpenAISimple(apiKey, prompt)
		if err != nil {
			jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Failed to analyze root word"})
			return
		}
		jsonResponse(w, http.StatusOK, map[string]string{
			"name":      name,
			"root_word": explanation,
		})
		return
	}

	prompt := fmt.Sprintf(`วิเคราะห์รากศัพท์ของชื่อ "%s"
บริบทความหมายที่ต้องการสื่อ: "%s"

คำสั่ง:
1. อธิบายว่าชื่อนี้มาจากคำบาลี/สันสกฤต หรือคำไทยคำไหนผสมกัน
2. พยายามเชื่อมโยงให้สอดคล้องกับ "บริบทความหมายที่ต้องการสื่อ"
3. ตอบสั้นๆ กระชับ เข้าใจง่าย (ห้ามเกริ่นนำ)
4. ตอบเป็นภาษาไทยเท่านั้น`, name, meaning)

	explanation, err := callTyphoonSimple(apiKey, prompt)
	if err != nil {
		fmt.Printf("Typhoon error: %v, falling back to OpenAI\n", err)
		openaiKey := os.Getenv("OPENAI_API_KEY")
		explanation, err = callOpenAISimple(openaiKey, prompt)
		if err != nil {
			jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Failed to analyze root word with both AI"})
			return
		}
	}

	jsonResponse(w, http.StatusOK, map[string]string{
		"name":      name,
		"root_word": explanation,
	})
}

// Helper for quick OpenAI calls (internal use) — uses gpt-4o
func callOpenAISimple(apiKey, prompt string) (string, error) {
	return callOpenAIWithModel(apiKey, prompt, "gpt-4o", 0)
}

// callOpenAIFast uses gpt-4o-mini with a token cap — fastest model available, ideal for structured JSON extraction
func callOpenAIFast(apiKey, prompt string, maxTokens int) (string, error) {
	return callOpenAIWithModel(apiKey, prompt, "gpt-4o-mini", maxTokens)
}

// callOpenAIWithModel is the shared implementation
func callOpenAIWithModel(apiKey, prompt, model string, maxTokens int) (string, error) {
	type reqBody struct {
		Model     string              `json:"model"`
		Messages  []map[string]string `json:"messages"`
		MaxTokens int                 `json:"max_tokens,omitempty"`
	}
	body := reqBody{
		Model:    model,
		Messages: []map[string]string{{"role": "user", "content": prompt}},
	}
	if maxTokens > 0 {
		body.MaxTokens = maxTokens
	}
	jsonData, _ := json.Marshal(body)
	reqAI, _ := http.NewRequest("POST", OPENAI_CHAT_API_URL, bytes.NewBuffer(jsonData))
	reqAI.Header.Set("Content-Type", "application/json")
	reqAI.Header.Set("Authorization", "Bearer "+apiKey)

	client := &http.Client{
		Timeout: 15 * time.Second,
	}
	resp, err := client.Do(reqAI)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	var gptResp OpenAIChatResponse
	json.NewDecoder(resp.Body).Decode(&gptResp)

	if len(gptResp.Choices) > 0 {
		return gptResp.Choices[0].Message.Content, nil
	}
	return "", fmt.Errorf("no response from AI")
}

func callTyphoonSimple(apiKey, prompt string) (string, error) {
	type reqBody struct {
		Model    string              `json:"model"`
		Messages []map[string]string `json:"messages"`
	}
	body := reqBody{
		Model:    TYPHOON_MODEL,
		Messages: []map[string]string{{"role": "user", "content": prompt}},
	}
	jsonData, _ := json.Marshal(body)
	reqTy, _ := http.NewRequest("POST", TYPHOON_CHAT_API_URL, bytes.NewBuffer(jsonData))
	reqTy.Header.Set("Content-Type", "application/json")
	reqTy.Header.Set("Authorization", "Bearer "+apiKey)

	client := &http.Client{
		Timeout: 15 * time.Second,
	}
	resp, err := client.Do(reqTy)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	var tyResp OpenAIChatResponse // Typhoon uses same OpenAI compatible format
	json.NewDecoder(resp.Body).Decode(&tyResp)

	if len(tyResp.Choices) > 0 {
		return tyResp.Choices[0].Message.Content, nil
	}
	return "", fmt.Errorf("no response from Typhoon")
}
