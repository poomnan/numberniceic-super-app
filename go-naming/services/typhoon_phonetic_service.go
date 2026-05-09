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
	DefaultTyphoonPhoneticModel   = "typhoon-v2.5-30b-a3b-instruct"
	DefaultTyphoonPhoneticVersion = "typhoon_v2_name_euphony_json"
	DefaultTyphoonMaxTokens       = 1200
	DefaultTyphoonBatchMaxTokens  = 3000
	TyphoonPhoneticSystemPrompt   = `ประเมินความไพเราะและความไหลลื่นของชื่อภาษาไทยเท่านั้น
ประเมินเฉพาะเสียงและจังหวะการอ่าน
ห้ามพิจารณาความหมาย ความมงคล ความนิยม บุคลิก ภาพลักษณ์ เลขศาสตร์ หรือดวง
ตอบเป็น JSON เท่านั้น
ห้ามมี markdown
ห้ามมีข้อความอื่นนอก JSON
คะแนนทุกช่องต้องเป็นจำนวนเต็ม 0-100
labels ต้องเลือกจากชุดคำที่กำหนดเท่านั้น
ถ้าชื่อมีลักษณะเสียงแข็งหรือสะดุด ต้องมี "เสียงค่อนข้างแข็ง" หรือ "จังหวะสะดุดเล็กน้อย" อย่างน้อย 1 ค่า
summary ต้องเป็นภาษาไทยล้วน ห้ามมีอักษรจีน เกาหลี ญี่ปุ่น หรืออักษรต่างประเทศอื่น
issues ใส่เฉพาะเมื่อมีปัญหา ถ้าไม่มีให้เป็น []`
)

var allowedPhoneticLabels = map[string]struct{}{
	"เสียงลื่น":           {},
	"ออกเสียงง่าย":        {},
	"ฟังนุ่ม":             {},
	"เสียงค่อนข้างแข็ง":   {},
	"จังหวะสมดุล":         {},
	"จังหวะสะดุดเล็กน้อย": {},
	"ฟังคลาสสิก":          {},
	"ฟังร่วมสมัย":         {},
	"จำง่าย":              {},
	"มีเอกลักษณ์":         {},
}

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
	return fmt.Sprintf(`ประเมินความไพเราะและความไหลลื่นของชื่อภาษาไทยนี้:

ชื่อ: %s

ให้ตอบเป็น JSON เท่านั้น ห้ามมีข้อความอื่นนอก JSON

ใช้ schema นี้:
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
}

กฎสำคัญ:
1. labels ต้องเลือกจากชุดนี้เท่านั้น:
[
  "เสียงลื่น",
  "ออกเสียงง่าย",
  "ฟังนุ่ม",
  "เสียงค่อนข้างแข็ง",
  "จังหวะสมดุล",
  "จังหวะสะดุดเล็กน้อย",
  "ฟังคลาสสิก",
  "ฟังร่วมสมัย",
  "จำง่าย",
  "มีเอกลักษณ์"
]

2. ถ้าชื่อมีลักษณะเสียงแข็งหรือสะดุด ให้ใส่:
- "เสียงค่อนข้างแข็ง"
หรือ
- "จังหวะสะดุดเล็กน้อย"

3. ห้ามใช้คำอื่นนอกชุด labels
4. labels ต้องมี 2-4 ค่า
5. summary ต้องเป็นภาษาไทยล้วน ห้ามมีอักษรจีน เกาหลี ญี่ปุ่น
6. issues:
- ใส่เฉพาะถ้ามีปัญหา
- ถ้าไม่มีให้ใส่ []
7. style_tone ไม่เกิน 3 คำ
8. ห้ามอธิบายเพิ่มนอก JSON

เป้าหมาย:
- ประเมินว่าชื่อนี้อ่านลื่นหรือไม่
- ไม่ต้องเน้นความเพราะที่สุด
- เน้นจับว่า "แปลก/สะดุด/แข็ง" หรือไม่`, name)
}

func BuildTyphoonBatchPhoneticUserPrompt(names []string) string {
	var b strings.Builder
	b.WriteString("ประเมินความไพเราะและความไหลลื่นของชื่อภาษาไทยต่อไปนี้\n")
	b.WriteString("ตอบเป็น JSON array เท่านั้น ไม่มีข้อความอื่น\n")
	b.WriteString("ทุก object ต้องมี: name, score, labels, summary, issues, style_tone\n")
	b.WriteString("name ต้องตรงกับ input เดิมทุกตัวอักษร\n")
	b.WriteString("summary ต้องเป็นภาษาไทยล้วน ห้ามมีอักษรจีน เกาหลี ญี่ปุ่น\n")
	for i, name := range names {
		fmt.Fprintf(&b, "%d:%s\n", i+1, name)
	}
	b.WriteString(`\nกฎ labels ใช้ได้เฉพาะ:
["เสียงลื่น","ออกเสียงง่าย","ฟังนุ่ม","เสียงค่อนข้างแข็ง","จังหวะสมดุล","จังหวะสะดุดเล็กน้อย","ฟังคลาสสิก","ฟังร่วมสมัย","จำง่าย","มีเอกลักษณ์"]`)
	b.WriteString(`\nถ้าชื่อมีลักษณะเสียงแข็งหรือสะดุด ต้องมี "เสียงค่อนข้างแข็ง" หรือ "จังหวะสะดุดเล็กน้อย"`)
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
		"max_tokens":  DefaultTyphoonBatchMaxTokens,
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

	if !looksLikeJSONArray(content) {
		return results, rawBody, fmt.Errorf(
			"structured phonetic batch content looks truncated or invalid: len=%d head=%q tail=%q",
			len(content),
			snippetForLog(content, 160, true),
			snippetForLog(content, 160, false),
		)
	}

	if err := json.Unmarshal([]byte(content), &results); err != nil {
		return results, rawBody, fmt.Errorf(
			"parse structured phonetic batch json failed: %w len=%d head=%q tail=%q",
			err,
			len(content),
			snippetForLog(content, 160, true),
			snippetForLog(content, 160, false),
		)
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

func looksLikeJSONArray(content string) bool {
	trimmed := strings.TrimSpace(content)
	return strings.HasPrefix(trimmed, "[") && strings.HasSuffix(trimmed, "]")
}

func snippetForLog(content string, max int, fromStart bool) string {
	trimmed := strings.TrimSpace(content)
	if len(trimmed) <= max {
		return trimmed
	}
	if fromStart {
		return trimmed[:max] + "..."
	}
	return "..." + trimmed[len(trimmed)-max:]
}

func sanitizePhoneticEvaluation(in PhoneticEvaluation) PhoneticEvaluation {
	in.Score.Overall = clampScore(in.Score.Overall)
	in.Score.PronunciationEase = clampScore(in.Score.PronunciationEase)
	in.Score.Euphony = clampScore(in.Score.Euphony)
	in.Score.Rhythm = clampScore(in.Score.Rhythm)

	in.Labels = trimAllowedLabels(in.Labels, 4)
	in.Issues = trimSlice(in.Issues, 2)
	for i := range in.Issues {
		in.Issues[i] = stripNonThaiText(in.Issues[i])
	}
	in.StyleTone = trimSlice(in.StyleTone, 3)
	for i := range in.StyleTone {
		in.StyleTone[i] = stripNonThaiText(in.StyleTone[i])
	}
	in.Summary = stripNonThaiText(in.Summary)

	return in
}

func trimAllowedLabels(items []string, max int) []string {
	if len(items) == 0 {
		return []string{}
	}
	out := make([]string, 0, max)
	seen := make(map[string]struct{}, max)
	for _, item := range items {
		clean := strings.TrimSpace(item)
		if clean == "" {
			continue
		}
		if _, ok := allowedPhoneticLabels[clean]; !ok {
			continue
		}
		if _, ok := seen[clean]; ok {
			continue
		}
		seen[clean] = struct{}{}
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

func stripNonThaiText(s string) string {
	var b strings.Builder
	b.Grow(len(s))
	for _, r := range s {
		if r == '\n' || r == '\t' {
			b.WriteRune(' ')
			continue
		}
		if (r >= '\u0E00' && r <= '\u0E7F') ||
			(r >= 'a' && r <= 'z') ||
			(r >= 'A' && r <= 'Z') ||
			(r >= '0' && r <= '9') ||
			r == ' ' || r == '.' || r == ',' || r == '!' ||
			r == '?' || r == '-' || r == '(' || r == ')' ||
			r == '"' || r == '\'' || r == ':' {
			b.WriteRune(r)
		}
	}
	return strings.TrimSpace(b.String())
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
