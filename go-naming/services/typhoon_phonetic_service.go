package services

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"strings"
	"time"
)

const (
	DefaultTyphoonPhoneticURL     = "https://api.opentyphoon.ai/v1/chat/completions"
	DefaultTyphoonPhoneticModel   = "typhoon-v2.1-12b-instruct"
	DefaultTyphoonPhoneticVersion = "typhoon_v1_promptA"
	DefaultTyphoonMaxTokens       = 1200
	TyphoonPhoneticSystemPrompt   = `ประเมินเสียงชื่อภาษาไทยเท่านั้น
ดู 4 มิติ: ความไหลลื่น การออกเสียง ความไพเราะ และจังหวะ
ห้ามวิเคราะห์ความหมาย เลขศาสตร์ ดวง หรือเรื่องอื่น
ตอบ JSON เท่านั้น ไม่มี prose ไม่มี markdown
คะแนนเป็นจำนวนเต็ม 0-100
labels <= 3, issues <= 2, style_tone <= 3
summary เป็นประโยคไทยสั้นหนึ่งประโยค
ถ้าไม่มี issues ให้ส่ง []`
)

type TyphoonPhoneticClient struct {
	APIKey     string
	URL        string
	Model      string
	Version    string
	HTTPClient *http.Client
}

type PhoneticEvaluation struct {
	Score struct {
		Overall           int `json:"overall"`
		PronunciationEase int `json:"pronunciation_ease"`
		Euphony           int `json:"euphony"`
		Rhythm            int `json:"rhythm"`
	} `json:"score"`
	Labels    []string `json:"labels"`
	Summary   string   `json:"summary"`
	Issues    []string `json:"issues"`
	StyleTone []string `json:"style_tone"`
}

type BatchPhoneticEvaluation struct {
	Name string `json:"name"`
	PhoneticEvaluation
}

type typhoonChatResponse struct {
	Choices []struct {
		Message struct {
			Content string `json:"content"`
		} `json:"message"`
	} `json:"choices"`
	Error interface{} `json:"error,omitempty"`
}

func NewTyphoonPhoneticClientFromEnv(timeout time.Duration) (*TyphoonPhoneticClient, error) {
	apiKey := strings.TrimSpace(os.Getenv("TYPHOON_API_KEY"))
	if apiKey == "" {
		return nil, fmt.Errorf("TYPHOON_API_KEY is not set")
	}

	url := strings.TrimSpace(os.Getenv("TYPHOON_API_URL"))
	if url == "" {
		url = DefaultTyphoonPhoneticURL
	}

	model := strings.TrimSpace(os.Getenv("TYPHOON_MODEL"))
	if model == "" {
		model = DefaultTyphoonPhoneticModel
	}

	version := strings.TrimSpace(os.Getenv("TYPHOON_PHONETIC_VERSION"))
	if version == "" {
		version = DefaultTyphoonPhoneticVersion
	}

	return &TyphoonPhoneticClient{
		APIKey:  apiKey,
		URL:     url,
		Model:   model,
		Version: version,
		HTTPClient: &http.Client{
			Timeout: timeout,
		},
	}, nil
}

func BuildTyphoonPhoneticUserPrompt(name string) string {
	return fmt.Sprintf(`ประเมินชื่อภาษาไทยนี้: "%s"

ให้โฟกัสเฉพาะเสียงของชื่อ:
- ความไหลลื่นเวลาพูด
- ความง่ายในการออกเสียง
- ความไพเราะโดยรวม
- จังหวะของชื่อ

ตอบ JSON ตาม schema นี้เท่านั้น:
{
  "score": {
    "overall": 0,
    "pronunciation_ease": 0,
    "euphony": 0,
    "rhythm": 0
  },
  "labels": [],
  "summary": "",
  "issues": [],
  "style_tone": []
}`, name)
}

func BuildTyphoonBatchPhoneticUserPrompt(names []string) string {
	var b strings.Builder
	b.WriteString("ประเมินชื่อเหล่านี้และตอบ JSON array เท่านั้น\n")
	b.WriteString("ทุก object ต้องมี: name, score, labels, summary, issues, style_tone\n")
	b.WriteString("name ต้องตรงกับ input เดิมทุกตัวอักษร\n")
	for i, name := range names {
		fmt.Fprintf(&b, "%d:%s\n", i+1, name)
	}
	b.WriteString(`\nตอบรูปแบบนี้เท่านั้น:[{"name":"นาลีตา","score":{"overall":0,"pronunciation_ease":0,"euphony":0,"rhythm":0},"labels":[],"summary":"","issues":[],"style_tone":[]}]`)
	return b.String()
}

func (c *TyphoonPhoneticClient) EvaluateThaiName(ctx context.Context, name string) (PhoneticEvaluation, json.RawMessage, error) {
	var result PhoneticEvaluation

	reqBody := map[string]interface{}{
		"model":       c.Model,
		"temperature": 0.1,
		"max_tokens":  DefaultTyphoonMaxTokens,
		"response_format": map[string]string{
			"type": "json_object",
		},
		"messages": []map[string]string{
			{"role": "system", "content": TyphoonPhoneticSystemPrompt},
			{"role": "user", "content": BuildTyphoonPhoneticUserPrompt(name)},
		},
	}

	payload, err := json.Marshal(reqBody)
	if err != nil {
		return result, nil, err
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.URL, bytes.NewBuffer(payload))
	if err != nil {
		return result, nil, err
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+c.APIKey)

	resp, err := c.HTTPClient.Do(req)
	if err != nil {
		return result, nil, fmt.Errorf("typhoon request failed: %w", err)
	}
	defer resp.Body.Close()

	rawBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return result, nil, fmt.Errorf("read typhoon response failed: %w", err)
	}

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return result, rawBody, fmt.Errorf("typhoon status %d: %s", resp.StatusCode, string(rawBody))
	}

	content, err := extractTyphoonContent(rawBody)
	if err != nil {
		return result, rawBody, err
	}

	if err := json.Unmarshal([]byte(content), &result); err != nil {
		return result, rawBody, fmt.Errorf("parse structured phonetic json failed: %w", err)
	}

	result = sanitizePhoneticEvaluation(result)
	return result, rawBody, nil
}

func (c *TyphoonPhoneticClient) EvaluateThaiNamesBatch(ctx context.Context, names []string) ([]BatchPhoneticEvaluation, json.RawMessage, error) {
	results := make([]BatchPhoneticEvaluation, 0)
	if len(names) == 0 {
		return results, json.RawMessage("[]"), nil
	}

	reqBody := map[string]interface{}{
		"model":       c.Model,
		"temperature": 0.1,
		"max_tokens":  1800,
		"messages": []map[string]string{
			{"role": "system", "content": TyphoonPhoneticSystemPrompt},
			{"role": "user", "content": BuildTyphoonBatchPhoneticUserPrompt(names)},
		},
	}

	payload, err := json.Marshal(reqBody)
	if err != nil {
		return results, nil, err
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.URL, bytes.NewBuffer(payload))
	if err != nil {
		return results, nil, err
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+c.APIKey)

	resp, err := c.HTTPClient.Do(req)
	if err != nil {
		return results, nil, fmt.Errorf("typhoon batch request failed: %w", err)
	}
	defer resp.Body.Close()

	rawBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return results, nil, fmt.Errorf("read typhoon batch response failed: %w", err)
	}

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return results, rawBody, fmt.Errorf("typhoon status %d: %s", resp.StatusCode, string(rawBody))
	}

	content, err := extractTyphoonContent(rawBody)
	if err != nil {
		return results, rawBody, err
	}

	if err := json.Unmarshal([]byte(content), &results); err != nil {
		return results, rawBody, fmt.Errorf("parse structured phonetic batch json failed: %w", err)
	}

	for i := range results {
		results[i].Name = strings.TrimSpace(results[i].Name)
		results[i].PhoneticEvaluation = sanitizePhoneticEvaluation(results[i].PhoneticEvaluation)
	}
	return results, rawBody, nil
}

func extractTyphoonContent(raw []byte) (string, error) {
	var chatResp typhoonChatResponse
	if err := json.Unmarshal(raw, &chatResp); err == nil && len(chatResp.Choices) > 0 {
		content := strings.TrimSpace(chatResp.Choices[0].Message.Content)
		return cleanupJSONContent(content), nil
	}

	var direct map[string]interface{}
	if err := json.Unmarshal(raw, &direct); err == nil {
		if _, ok := direct["score"]; ok {
			return string(raw), nil
		}
	}

	return "", fmt.Errorf("unsupported typhoon response shape: %s", string(raw))
}

func cleanupJSONContent(content string) string {
	content = strings.TrimSpace(content)
	content = strings.TrimPrefix(content, "```json")
	content = strings.TrimPrefix(content, "```")
	content = strings.TrimSuffix(content, "```")
	return strings.TrimSpace(content)
}

func sanitizePhoneticEvaluation(in PhoneticEvaluation) PhoneticEvaluation {
	in.Score.Overall = clampScore(in.Score.Overall)
	in.Score.PronunciationEase = clampScore(in.Score.PronunciationEase)
	in.Score.Euphony = clampScore(in.Score.Euphony)
	in.Score.Rhythm = clampScore(in.Score.Rhythm)

	in.Labels = trimSlice(in.Labels, 3)
	in.Issues = trimSlice(in.Issues, 2)
	in.StyleTone = trimSlice(in.StyleTone, 3)
	in.Summary = strings.TrimSpace(in.Summary)

	return in
}

func trimSlice(items []string, max int) []string {
	if len(items) == 0 {
		return []string{}
	}
	out := make([]string, 0, max)
	for _, item := range items {
		clean := strings.TrimSpace(item)
		if clean == "" {
			continue
		}
		out = append(out, clean)
		if len(out) >= max {
			break
		}
	}
	if len(out) == 0 {
		return []string{}
	}
	return out
}

func clampScore(v int) int {
	if v < 0 {
		return 0
	}
	if v > 100 {
		return 100
	}
	return v
}

// GenerateThaiNameMeaning uses Typhoon AI to generate a beautiful, auspicious Thai meaning for a name.
func (c *TyphoonPhoneticClient) GenerateThaiNameMeaning(ctx context.Context, name string) (string, json.RawMessage, error) {
	reqBody := map[string]interface{}{
		"model":       c.Model,
		"temperature": 0.3,
		"max_tokens":  150,
		"messages": []map[string]string{
			{"role": "system", "content": "คุณคือผู้เชี่ยวชาญด้านภาษาไทยและคลังชื่อมงคล ให้ระบุความหมายมงคลที่ถูกต้อง ไพเราะ และกระชับ สำหรับชื่อภาษาไทยที่กำหนดให้ ตอบเฉพาะคำแปลหรือความหมายสั้นๆ ไม่ต้องมีอารัมภบทหรืออธิบายเพิ่มเติม เช่น 'ผู้มีชื่อเสียงอันดีงาม', 'ผู้มีความเจริญรุ่งเรือง'"},
			{"role": "user", "content": fmt.Sprintf("ขอความหมายมงคลของชื่อ: %s", name)},
		},
	}

	payload, err := json.Marshal(reqBody)
	if err != nil {
		return "", nil, err
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.URL, bytes.NewBuffer(payload))
	if err != nil {
		return "", nil, err
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+c.APIKey)

	resp, err := c.HTTPClient.Do(req)
	if err != nil {
		return "", nil, fmt.Errorf("typhoon request failed: %w", err)
	}
	defer resp.Body.Close()

	rawBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", nil, fmt.Errorf("read typhoon response failed: %w", err)
	}

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return "", rawBody, fmt.Errorf("typhoon status %d: %s", resp.StatusCode, string(rawBody))
	}

	var chatResp typhoonChatResponse
	if err := json.Unmarshal(rawBody, &chatResp); err == nil && len(chatResp.Choices) > 0 {
		content := strings.TrimSpace(chatResp.Choices[0].Message.Content)
		content = strings.Trim(content, "\"`'")
		return content, rawBody, nil
	}

	return "", rawBody, fmt.Errorf("unsupported typhoon response shape: %s", string(rawBody))
}

