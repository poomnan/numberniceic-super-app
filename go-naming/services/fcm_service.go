package services

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"strings"
	"time"

	"golang.org/x/oauth2/google"
)

// FCMConfig holds the Firebase Cloud Messaging configuration
type FCMConfig struct {
	ProjectID      string
	ServiceAccount *google.Credentials
}

var fcmConfig *FCMConfig

// InitFCM initializes Firebase Cloud Messaging with Service Account
func InitFCM(serviceAccountPath string) error {
	// Read service account JSON file
	data, err := ioutil.ReadFile(serviceAccountPath)
	if err != nil {
		return fmt.Errorf("failed to read service account file: %v", err)
	}

	// Parse to get project ID
	var sa map[string]interface{}
	if err := json.Unmarshal(data, &sa); err != nil {
		return fmt.Errorf("failed to parse service account JSON: %v", err)
	}

	projectID, ok := sa["project_id"].(string)
	if !ok {
		return fmt.Errorf("project_id not found in service account JSON")
	}

	// Create credentials
	creds, err := google.CredentialsFromJSON(
		context.Background(),
		data,
		"https://www.googleapis.com/auth/firebase.messaging",
	)
	if err != nil {
		return fmt.Errorf("failed to create credentials: %v", err)
	}

	fcmConfig = &FCMConfig{
		ProjectID:      projectID,
		ServiceAccount: creds,
	}

	log.Printf("✅ FCM initialized for project: %s", projectID)
	return nil
}

// getAccessToken retrieves OAuth 2.0 access token
func getAccessToken() (string, error) {
	if fcmConfig == nil {
		return "", fmt.Errorf("FCM not initialized")
	}

	token, err := fcmConfig.ServiceAccount.TokenSource.Token()
	if err != nil {
		return "", fmt.Errorf("failed to get access token: %v", err)
	}

	return token.AccessToken, nil
}

// SendFCMNotificationV1 sends notification using FCM V1 API
func SendFCMNotificationV1(fcmToken, title, body string, data map[string]string) error {
	if fcmConfig == nil {
		return fmt.Errorf("FCM not initialized")
	}

	// Get OAuth 2.0 access token
	accessToken, err := getAccessToken()
	if err != nil {
		return err
	}

	// Build FCM V1 API URL
	url := fmt.Sprintf("https://fcm.googleapis.com/v1/projects/%s/messages:send", fcmConfig.ProjectID)

	// Build message payload
	message := map[string]interface{}{
		"message": map[string]interface{}{
			"token": fcmToken,
			"notification": map[string]string{
				"title": title,
				"body":  body,
			},
			"data": data,
			"android": map[string]interface{}{
				"priority": "high",
			},
		},
	}

	jsonData, err := json.Marshal(message)
	if err != nil {
		return fmt.Errorf("failed to marshal message: %v", err)
	}

	// Create HTTP request
	req, err := http.NewRequest("POST", url, strings.NewReader(string(jsonData)))
	if err != nil {
		return fmt.Errorf("failed to create request: %v", err)
	}

	// Set headers
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+accessToken)

	// Send request
	client := &http.Client{Timeout: 10 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return fmt.Errorf("failed to send request: %v", err)
	}
	defer resp.Body.Close()

	// Read response
	respBody, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return fmt.Errorf("failed to read response: %v", err)
	}

	// Check status
	if resp.StatusCode != http.StatusOK {
		log.Printf("❌ FCM Error: Status %d, Body: %s", resp.StatusCode, string(respBody))
		return fmt.Errorf("FCM returned status %d: %s", resp.StatusCode, string(respBody))
	}

	log.Printf("✅ FCM notification sent successfully. Response: %s", string(respBody))
	return nil
}

// SendFCMDataMessageV1 sends a DATA-ONLY message using FCM V1 API
// This is critical for VoIP calls to ensure onMessageReceived is triggered in background
func SendFCMDataMessageV1(fcmToken string, data map[string]string) error {
	if fcmConfig == nil {
		return fmt.Errorf("FCM not initialized")
	}

	accessToken, err := getAccessToken()
	if err != nil {
		return err
	}

	url := fmt.Sprintf("https://fcm.googleapis.com/v1/projects/%s/messages:send", fcmConfig.ProjectID)

	message := map[string]interface{}{
		"message": map[string]interface{}{
			"token": fcmToken,
			"data":  data,
			"android": map[string]interface{}{
				"priority": "high",
				"ttl":      "0s", // Deliver immediately or fail
			},
		},
	}

	jsonData, err := json.Marshal(message)
	if err != nil {
		return fmt.Errorf("failed to marshal message: %v", err)
	}

	req, err := http.NewRequest("POST", url, strings.NewReader(string(jsonData)))
	if err != nil {
		return fmt.Errorf("failed to create request: %v", err)
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+accessToken)

	client := &http.Client{Timeout: 10 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return fmt.Errorf("failed to send request: %v", err)
	}
	defer resp.Body.Close()

	respBody, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return fmt.Errorf("failed to read response: %v", err)
	}

	if resp.StatusCode != http.StatusOK {
		log.Printf("❌ FCM Data Message Error: Status %d, Body: %s", resp.StatusCode, string(respBody))
		return fmt.Errorf("FCM returned status %d: %s", resp.StatusCode, string(respBody))
	}

	log.Printf("✅ FCM Data Message sent successfully. Response: %s", string(respBody))
	return nil
}

// SendFCMNotification is backward compatible wrapper
func SendFCMNotification(payload map[string]interface{}) error {
	// Extract data from legacy payload format
	to, _ := payload["to"].(string)

	notification, _ := payload["notification"].(map[string]interface{})
	title, _ := notification["title"].(string)
	body, _ := notification["body"].(string)

	data, _ := payload["data"].(map[string]interface{})
	dataStr := make(map[string]string)
	for k, v := range data {
		dataStr[k] = fmt.Sprintf("%v", v)
	}

	// Use V1 API
	return SendFCMNotificationV1(to, title, body, dataStr)
}
