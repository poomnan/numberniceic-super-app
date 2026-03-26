package handlers

import (
	"encoding/json"
	"fmt"
	"go-naming/services"
	"io"
	"net/http"
	"net/url"
	"os"
	"strings"
)

// DreamSearchResult represents the result from dream search
type DreamSearchResult struct {
	Found bool `json:"found"`
	Dream struct {
		DreamKeyword        string `json:"dream_keyword"`
		DreamInterpretation string `json:"dream_interpretation"`
		LuckyNumbers        string `json:"lucky_numbers"`
		Category            string `json:"category"`
	} `json:"dream"`
	MatchType string `json:"match_type"`
	Query     string `json:"query"`
}

// OpenClawHandler handles both Dream Interpretation AND Naming Requests
func OpenClawHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Helper to extract JSON body
	var userReq struct {
		Message string `json:"message"`
		// Context fields (Optional) for Naming
		Surname    string `json:"surname"`
		DayOfBirth string `json:"day_of_birth"` // e.g., "Sunday"
	}
	if err := json.NewDecoder(r.Body).Decode(&userReq); err != nil {
		http.Error(w, "Invalid body", http.StatusBadRequest)
		return
	}

	message := strings.TrimSpace(userReq.Message)

	// 1. Intent Classification (Simple Heuristic for now, can be LLM powered later)
	isNamingRequest := strings.Contains(message, "ชื่อ") || strings.Contains(message, "ตั้งชื่อ") ||
		strings.Contains(message, "นามสกุล") || (userReq.Surname != "")

	if isNamingRequest {
		handleNamingRequest(w, message, userReq.Surname, userReq.DayOfBirth)
		return
	}

	// 2. Default: Dream Interpretation Flow
	handleDreamRequest(w, message)
}

// handleNamingRequest processes naming intent with OpenClaw persona
func handleNamingRequest(w http.ResponseWriter, intent string, surname string, day string) {
	// A. Fetch Recommendations using Fast Algorithm
	// Default limit 5 for chat context
	recommendations, err := services.RecommendNamesFast(intent, surname, day, 5)

	var reply string
	if err != nil {
		fmt.Printf("Error recommending names: %v\n", err)
		reply = "ขออภัยค่ะ ระบบขัดข้องชั่วคราว คุณนินกำลังรีบตรวจสอบให้นะคะ"
	} else if len(recommendations) == 0 {
		reply = "เสียดายจัง ไม่พบชื่อที่ตรงกับเงื่อนไขเลขศาสตร์ชั้นสูงในฐานข้อมูลเลยค่ะ อาจจะต้องลองปรับความหมายกว้างขึ้นนิดนึงนะคะ"
	} else {
		// B. Formulate Response with Analysis (Storytelling)
		var sb strings.Builder
		sb.WriteString("คุณนินคัดเลือกชื่อมงคลที่สุดมาให้ค่ะ:\n\n")

		for i, rec := range recommendations {
			grandTotalSat := rec.NameSatSum + rec.SurnameSatSum
			sb.WriteString(fmt.Sprintf("%d. **%s** + (นามสกุล) = **ผลรวมชีวิต %d**\n", i+1, rec.ThName, grandTotalSat))
			sb.WriteString(fmt.Sprintf("   (เฉพาะชื่อ: ✨เลขศาสตร์ %d | 🌑พลังเงา %d)\n", rec.SatSum, rec.ShaSum))

			// 1. Sat Sum Highlight (Numerology)
			satSnippets := []string{}
			seenSat := make(map[string]bool)
			for _, m := range rec.TotalSatMeanings {
				parts := strings.Split(m, ":")
				if len(parts) >= 2 {
					pair := parts[0]
					if !seenSat[pair] {
						desc := getProfessionalPairDesc(pair)
						if desc != "" {
							satSnippets = append(satSnippets, fmt.Sprintf("**คู่ %s**: %s", pair, desc))
							seenSat[pair] = true
						}
					}
				}
				if len(satSnippets) >= 1 {
					break
				} // Top 1 Sat Pair
			}
			if len(satSnippets) > 0 {
				sb.WriteString(fmt.Sprintf("   ✨ **พลังเลขศาสตร์:** %s\n", strings.Join(satSnippets, " ")))
			}

			// 2. Sha Sum Highlight (Shadow Power)
			shaSnippets := []string{}
			seenSha := make(map[string]bool)
			for _, m := range rec.TotalShaMeanings {
				parts := strings.Split(m, ":")
				if len(parts) >= 2 {
					pair := parts[0]
					if !seenSha[pair] {
						desc := getProfessionalPairDesc(pair)
						if desc != "" {
							shaSnippets = append(shaSnippets, fmt.Sprintf("**คู่ %s**: %s", pair, desc))
							seenSha[pair] = true
						}
					}
				}
				if len(shaSnippets) >= 1 {
					break
				} // Top 1 Sha Pair
			}
			if len(shaSnippets) > 0 {
				sb.WriteString(fmt.Sprintf("   🌑 **พลังเงาซ่อนเร้น:** %s\n", strings.Join(shaSnippets, " ")))
			}

			// Kalakini Reassurance
			if day != "" {
				sb.WriteString("   ✅ ตรวจสอบแล้ว: ไม่มีอักษรกาลกิณีวันเกิด\n")
			}

			sb.WriteString("\n")
		}

		if day == "" {
			// No birthday provided -> Skip kalakini mention entirely
			sb.WriteString("\n💡 ชื่อเหล่านี้คัดมาเฉพาะเกรด A+ ตามหลักเลขศาสตร์แน่นอนค่ะ")
		} else {
			sb.WriteString(fmt.Sprintf("\n💡 ทุกชื่อผ่านการตรวจสอบกาลกิณีสำหรับคนเกิด**%s** เรียบร้อยค่ะ", day))
		}

		reply = sb.String()
	}

	response := map[string]interface{}{
		"reply": reply,
		"type":  "naming_result",
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// Basic map for "Professional Review" generation
// Helper to get miracle description from DB directly
// In a real optimized system, this map should be cached at start up
func getProfessionalPairDesc(pair string) string {
	descMap, err := services.GetPairDescriptionsMap()
	if err != nil {
		fmt.Printf("Error fetching miracle desc: %v\n", err)
		return ""
	}
	if v, ok := descMap[pair]; ok {
		return v // Return raw description
	}
	return ""
}

// handleDreamRequest keeps the original logic intact
func handleDreamRequest(w http.ResponseWriter, message string) {
	// ... (Original Code for Dream Search & AI Fallback) ...
	// 1. Search dream in database (Using Simple Schema)
	dreamResp, err := http.Get(fmt.Sprintf("http://localhost:8095/search-dream?q=%s", url.QueryEscape(message)))

	var dreamResult DreamSearchResult
	var reply string

	// Default fallback content if search fails
	if err == nil && dreamResp.StatusCode == 200 {
		body, _ := io.ReadAll(dreamResp.Body)
		dreamResp.Body.Close()
		json.Unmarshal(body, &dreamResult)
	}
	if dreamResult.Found {
		// Aggressive pre-cleaning of raw data
		interp := dreamResult.Dream.DreamInterpretation
		interp = strings.ReplaceAll(interp, "หาก", "ถ้า")
		interp = strings.ReplaceAll(interp, "มีคำทำนายดังนี้:", "")
		interp = strings.ReplaceAll(interp, "มีคำทำนายดังนี้", "")
		interp = strings.ReplaceAll(interp, "สรุปคำทำนายดังนี้:", "")
		interp = strings.ReplaceAll(interp, "สรุปคำทำนายดังนี้", "")
		interp = strings.ReplaceAll(interp, "แม่หมอ", "ผู้ทำนาย")
		interp = strings.ReplaceAll(interp, "หมอ", "ผู้ทำนาย")
		interp = strings.ReplaceAll(interp, "คุณลูกค้า", "คุณ")
		dreamResult.Dream.DreamInterpretation = interp
	}

	if dreamResult.Found {
		prefix := ""
		// If it's a semantic match (nearby keyword), we show it but with a polite prefix
		if dreamResult.MatchType == "semantic" {
			prefix = fmt.Sprintf("ไม่พบคำทำนายสำหรับ '%s' โดยตรงในตำรา แต่พบสิ่งที่ใกล้เคียงที่สุดคือ: ", message)
			if dreamResult.Dream.Category == "ปรึกษา" {
				reply = fmt.Sprintf("%sสำหรับเรื่อง %s ขอให้ %s เลขนำโชคของคุณคือ: %s",
					prefix, dreamResult.Dream.DreamKeyword, dreamResult.Dream.DreamInterpretation, dreamResult.Dream.LuckyNumbers)
			} else {
				reply = fmt.Sprintf("%sถ้าฝันเห็น %s ทำนายว่า %s เลขนำโชคคือ: %s",
					prefix, dreamResult.Dream.DreamKeyword, dreamResult.Dream.DreamInterpretation, dreamResult.Dream.LuckyNumbers)
			}
		} else if dreamResult.MatchType == "text" {
			// CRITICAL IMPROVEMENT: If it's only a text match in interpretation (likely noisy),
			// we call AI to synthesize a better answer using the DB result as context.
			apiKey := os.Getenv("OPENAI_API_KEY")
			if apiKey != "" {
				systemPrompt := `ทำหน้าเป็น "คุณนิน" ผู้เชี่ยวชาญการทำนายฝัน
ภารกิจ: สรุปคำทำนายให้กับผู้ใช้โดยใช้ข้อมูลจากตำราเป็นพื้นฐาน
คำสั่ง:
1. หากหัวข้อในตำรา (Keyword) ไม่ตรงกับสิ่งที่ผู้ใช้ค้นหา แต่มีข้อความข้างในที่เกี่ยวข้อง ให้พยายามเชื่อมโยงให้ผู้ใช้เข้าใจ
2. หากไม่เกี่ยวข้องกันเลย ให้ทำนายตามความหมายสากลแต่แจ้งผู้ใช้ว่าสังเคราะห์ข้อมูลขึ้นมาใหม่
3. ใช้ภาษาที่เป็นทางการ สุภาพ และกระชับ
4. ห้ามทักทาย ห้ามเวิ่นเว้อ ห้ามแทนตัวว่าแม่หมอ
5. เริ่มต้นประโยคด้วยรูปแบบ "ถ้าฝันเห็น...ทำนายว่า..." ห้ามใช้คำว่า "มีคำทำนายดังนี้" หรือ "สรุปได้ดังนี้"
6. ห้ามใช้คำว่า "หาก" ให้ใช้คำว่า "ถ้า" แทนทั้งหมด`

				userPrompt := fmt.Sprintf(`ผู้ใช้ค้นหาคำว่า: "%s"
ข้อมูลในตำราที่พบ (Keyword: %s): "%s"
เลขนำโชคในตำรา: %s

กรุณาสรุปคำทำนายสั้นๆ ให้ผู้ใช้ โดยเน้นวิเคราะห์ตามสิ่งที่ผู้ใช้ค้นหา ("%s") เป็นหลัก`,
					message, dreamResult.Dream.DreamKeyword, dreamResult.Dream.DreamInterpretation, dreamResult.Dream.LuckyNumbers, message)

				var err error
				reply, err = services.CallOpenAI(userPrompt, systemPrompt)
				if err != nil {
					fmt.Printf("Error calling OpenAI for Synthesis: %v\n", err)
				}
			} else {
				// Fallback if AI fails
				prefix = fmt.Sprintf("ในตำราไม่ได้ลงรายละเอียดเรื่อง '%s' ไว้โดยตรง แต่พอจะมีคำทำนายที่กล่าวถึงคือ: ", message)
				reply = fmt.Sprintf("%sถ้าฝันเห็น %s ทำนายว่า %s เลขนำโชคคือ: %s",
					prefix, dreamResult.Dream.DreamKeyword, dreamResult.Dream.DreamInterpretation, dreamResult.Dream.LuckyNumbers)
			}
		} else {
			// Exact or Fuzzy Keyword Match (High Confidence)
			if dreamResult.Dream.Category == "ปรึกษา" {
				reply = fmt.Sprintf("สำหรับเรื่อง %s ขอให้ %s เลขนำโชคของคุณคือ: %s",
					dreamResult.Dream.DreamKeyword, dreamResult.Dream.DreamInterpretation, dreamResult.Dream.LuckyNumbers)
			} else {
				reply = fmt.Sprintf("ถ้าฝันเห็น %s ทำนายว่า %s เลขนำโชคคือ: %s",
					dreamResult.Dream.DreamKeyword, dreamResult.Dream.DreamInterpretation, dreamResult.Dream.LuckyNumbers)
			}
		}
	} else {
		// Only call AI if not found in database
		apiKey := os.Getenv("OPENAI_API_KEY") // Re-read since it was in local scope before
		if apiKey == "" {
			// fallback without AI
			reply = "ขออภัยค่ะ ไม่พบข้อมูลในตำราทำนายฝัน"
		} else {
			systemPrompt := `หน้าที่: สรุปข้อมูลจากฐานข้อมูลให้กระชับและเป็นทางการ
คำสั่ง:
1. เรียบเรียงคำทำนายให้สั้น กระชับ และเป็นภาษาที่สุภาพ
2. ห้ามทักทาย หรือลงท้ายเวิ่นเว้อ
3. ห้ามแทนตัวเองว่า "แม่หมอ" ให้แทนตัวเองว่า "คุณนิน" หรือไม่แทนตัวเลย
4. หากไม่พบข้อมูล ให้แจ้งสั้นๆ ว่าไม่พบในตำรา และกดปุ่มคุยกับคุณนิน (ห้ามเกิน 2 บรรทัด)`

			userPrompt := fmt.Sprintf(`คำถามจากผู้ใช้: "%s" (ไม่พบข้อมูลในฐานข้อมูลปัจจุบัน)`, message)

			var err error
			reply, err = services.CallOpenAI(userPrompt, systemPrompt)
			if err != nil {
				fmt.Printf("Error calling OpenAI Chat: %v\n", err)
			}
		}
	}

	if reply != "" {
		// Aggressive Post-Processing
		reply = strings.ReplaceAll(reply, "แม่หมอนินิน:", "")
		reply = strings.ReplaceAll(reply, "แม่หมอนินิน", "คุณนิน")
		reply = strings.ReplaceAll(reply, "แม่หมอ:", "")
		reply = strings.ReplaceAll(reply, "แม่หมอ", "คุณนิน")
		reply = strings.ReplaceAll(reply, "คุณลูกค้า", "คุณ")
		reply = strings.ReplaceAll(reply, "นะจ๊ะ", "นะคะ")
		reply = strings.ReplaceAll(reply, "หาก", "ถ้า")
		reply = strings.ReplaceAll(reply, "มีคำทำนายสรุปดังนี้", "")
		reply = strings.ReplaceAll(reply, "มีคำทำนายดังนี้:", "")
		reply = strings.ReplaceAll(reply, "มีคำทำนายดังนี้", "")
		reply = strings.ReplaceAll(reply, "สรุปคำทำนายดังนี้:", "")
		reply = strings.ReplaceAll(reply, "สรุปคำทำนายดังนี้", "")
		// Handle double "ทำนายว่า"
		reply = strings.ReplaceAll(reply, "ทำนายว่า ทำนายว่า", "ทำนายว่า")
		reply = strings.TrimSpace(reply)
	}

	response := map[string]interface{}{
		"reply":               strings.TrimSpace(reply),
		"show_consult_button": !dreamResult.Found,
	}

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(response); err != nil {
		fmt.Printf("Error encoding response: %v\n", err)
	}
}
