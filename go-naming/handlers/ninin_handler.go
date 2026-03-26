package handlers

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"go-naming/database"
	"go-naming/models"
	"go-naming/services"
	"io"
	"net/http"
	"net/url"
	"os"
	"strings"
)

// OpenAI Configuration
const OPENAI_CHAT_API_URL = "https://api.openai.com/v1/chat/completions"

// OpenAIChatRequest structure
type OpenAIChatRequest struct {
	Model    string              `json:"model"`
	Messages []map[string]string `json:"messages"`
}

// OpenAIChatResponse structure
type OpenAIChatResponse struct {
	Choices []struct {
		Message struct {
			Content string `json:"content"`
		} `json:"message"`
	} `json:"choices"`
}

// NininChatRequest represents the request from mobile/web
type NininChatRequest struct {
	Message  string `json:"message"`
	GuestID  string `json:"guest_id"`
	MemberID string `json:"member_id"`
}

const freeDreamChatLimit = 30

func buildPackageLockReply(accessMsg string) string {
	if strings.Contains(accessMsg, "หมดอายุ") {
		return accessMsg + " กรุณาเลือกแพ็คเกจทำนายฝันเพื่อใช้งานต่อเนื่อง ✨"
	}
	return "คุณได้ใช้งานสิทธิ์ฟรีครบ 30 ครั้งแล้ว กรุณาเลือกแพ็คเกจทำนายฝันเพื่อใช้งานไม่จำกัด ✨"
}

func incrementUsageCount(usageID string) int {
	if strings.TrimSpace(usageID) == "" {
		return 0
	}
	var count int
	err := database.DB.QueryRow(`
		INSERT INTO guest_usage (guest_id, message_count, last_used_at)
		VALUES ($1, 1, CURRENT_TIMESTAMP)
		ON CONFLICT (guest_id)
		DO UPDATE SET message_count = guest_usage.message_count + 1, last_used_at = CURRENT_TIMESTAMP
		RETURNING message_count
	`, usageID).Scan(&count)
	if err != nil {
		fmt.Printf("Database error incrementing usage count (%s): %v\n", usageID, err)
		return 0
	}
	return count
}

// NininChatHandler handles dream interpretation requests for the mobile app
func NininChatHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	apiKey := os.Getenv("OPENAI_API_KEY")
	if apiKey == "" {
		http.Error(w, "API Configuration error", http.StatusInternalServerError)
		return
	}

	var req NininChatRequest
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
	fmt.Printf("DEBUG Ninin: GuestID='%s', MemberID='%s', Message='%s'\n", req.GuestID, req.MemberID, userMessage)

	usageCount := 0
	isLimitedUser := false

	// 0. Check Paid Access via shop_products (Requirement: ID 1=1mo, 2=1yr, 3=lifetime)
	hasPaidAccess, accessMsg, expiry := services.CheckDreamChatAccess(req.MemberID, req.GuestID)
	if hasPaidAccess {
		fmt.Printf("DEBUG Ninin: Paid Access Found: %s (Expiry: %v)\n", accessMsg, expiry)

		// Record usage but don't limit
		idForUsage := req.GuestID
		if req.MemberID != "" {
			idForUsage = "member_" + req.MemberID
		}
		usageCount = incrementUsageCount(idForUsage)
	} else {
		// 1. Fallback to Free Limits (Guest/Member normal: 30)
		if req.MemberID == "" && req.GuestID != "" {
			isLimitedUser = true
			usageCount = incrementUsageCount(req.GuestID)
			freeRemaining := freeDreamChatLimit - usageCount
			if freeRemaining < 0 {
				freeRemaining = 0
			}
			freeLimit := freeDreamChatLimit
			if usageCount > freeDreamChatLimit {
				finalResponse := models.NininChatResponse{
					Reply: buildPackageLockReply(accessMsg),
					NininPersona: map[string]string{
						"name":        "คุณนิน",
						"description": "ผู้เชี่ยวชาญด้านศาสตร์ทำนายฝัน",
						"avatar_url":  "/static/ninin_avatar.png",
					},
					ShowConsultButton: true,
					ShowPackages:      true,
					UsageCount:        &usageCount,
					FreeLimit:         &freeLimit,
					FreeRemaining:     &freeRemaining,
				}
				w.Header().Set("Content-Type", "application/json")
				json.NewEncoder(w).Encode(finalResponse)
				return
			}
		} else if req.MemberID != "" {
			// If member, check VIP status from MySQL
			vipCode := ""
			mysqlDB := database.GetMySQLDB()
			if mysqlDB != nil {
				err := mysqlDB.QueryRow("SELECT vipcode FROM membertb WHERE memberid = ?", req.MemberID).Scan(&vipCode)
				if err != nil && err != sql.ErrNoRows {
					fmt.Printf("MySQL error checking vipcode: %v\n", err)
				}
			}

			// If normal member, check limit (30 messages)
			if vipCode == "normal" || vipCode == "" {
				isLimitedUser = true
				usageCount = incrementUsageCount("member_" + req.MemberID)
				freeRemaining := freeDreamChatLimit - usageCount
				if freeRemaining < 0 {
					freeRemaining = 0
				}
				freeLimit := freeDreamChatLimit
				if usageCount > freeDreamChatLimit {
					finalResponse := models.NininChatResponse{
						Reply: buildPackageLockReply(accessMsg),
						NininPersona: map[string]string{
							"name":        "คุณนิน",
							"description": "ผู้เชี่ยวชาญด้านศาสตร์ทำนายฝัน",
							"avatar_url":  "/static/ninin_avatar.png",
						},
						ShowConsultButton: true,
						ShowPackages:      true,
						UsageCount:        &usageCount,
						FreeLimit:         &freeLimit,
						FreeRemaining:     &freeRemaining,
					}
					w.Header().Set("Content-Type", "application/json")
					json.NewEncoder(w).Encode(finalResponse)
					return
				}
			} else {
				// VIP member (Legacy) or other status - still record usage
				usageCount = incrementUsageCount("member_" + req.MemberID)
			}
		}
	}

	// 1. Search dream in database via internal search API
	dreamResp, err := http.Get(fmt.Sprintf("http://localhost:8095/search-dream?q=%s", url.QueryEscape(userMessage)))

	type DreamSearchResult struct {
		Found bool `json:"found" `
		Dream struct {
			DreamID             int    `json:"dream_id"`
			DreamKeyword        string `json:"dream_keyword"`
			DreamInterpretation string `json:"dream_interpretation"`
			LuckyNumbers        string `json:"lucky_numbers"`
			Category            string `json:"category"`
		} `json:"dream"`
		MatchType string `json:"match_type"`
	}

	var dreamResult DreamSearchResult
	var dreamData *models.NininDreamData

	if err == nil && dreamResp.StatusCode == 200 {
		body, _ := io.ReadAll(dreamResp.Body)
		dreamResp.Body.Close()
		json.Unmarshal(body, &dreamResult)

		if dreamResult.Found {
			interp := dreamResult.Dream.DreamInterpretation

			dreamData = &models.NininDreamData{
				Keyword:        dreamResult.Dream.DreamKeyword,
				Interpretation: interp,
				LuckyNumbers:   dreamResult.Dream.LuckyNumbers,
			}
		}
	}

	var reply string
	if dreamResult.Found {
		// Exact, Fuzzy, or Semantic match - Use data directly without modifications
		luckyText := ""
		if dreamResult.Dream.LuckyNumbers != "" && dreamResult.Dream.LuckyNumbers != "null" && dreamResult.Dream.LuckyNumbers != "None" && dreamResult.Dream.LuckyNumbers != "-" {
			luckyText = " เลขเด็ด: " + dreamResult.Dream.LuckyNumbers
		}

		reply = dreamResult.Dream.DreamInterpretation
		if luckyText != "" {
			reply += luckyText
		}
	} else {
		// No data found -> Inform user and invite to chat
		reply = "เรื่องที่คุณค้นหายังไม่มีในตำรา คุณสามารถพิมพ์รายละเอียด เพื่อให้คุณนินช่วยวิเคราะห์เชิงลึกให้ได้จ้า"
	}

	if usageCount > 0 {
		if isLimitedUser {
			remaining := freeDreamChatLimit - usageCount
			if remaining < 0 {
				remaining = 0
			}
			reply = strings.TrimSpace(reply) + fmt.Sprintf("\n\nสิทธิ์ใช้ฟรีคงเหลือ %d/%d ครั้ง", remaining, freeDreamChatLimit)
		} else {
			reply = strings.TrimSpace(reply) + fmt.Sprintf("\n\nจำนวนครั้งที่ใช้งานสะสม: %d ครั้ง", usageCount)
		}
	}

	// 3. Prepare Final Response
	var freeLimitPtr *int
	var freeRemainingPtr *int
	if isLimitedUser {
		freeLimit := freeDreamChatLimit
		remaining := freeLimit - usageCount
		if remaining < 0 {
			remaining = 0
		}
		freeLimitPtr = &freeLimit
		freeRemainingPtr = &remaining
	}

	finalResponse := models.NininChatResponse{
		Reply:             strings.TrimSpace(reply),
		DreamFound:        dreamResult.Found,
		DreamData:         dreamData,
		ShowConsultButton: !dreamResult.Found, // Show button if dream not found
		UsageCount:        &usageCount,
		FreeLimit:         freeLimitPtr,
		FreeRemaining:     freeRemainingPtr,
		NininPersona: map[string]string{
			"name":        "คุณนิน",
			"description": "ผู้เชี่ยวชาญด้านศาสตร์ทำนายฝัน",
			"avatar_url":  "/static/ninin_avatar.png",
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(finalResponse)
}
