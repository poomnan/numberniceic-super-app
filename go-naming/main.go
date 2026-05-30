package main

import (
	"encoding/json"
	"go-naming/astro"
	"go-naming/database"
	"go-naming/handlers"
	"go-naming/models"
	"go-naming/services"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

// Helper to send JSON response
func jsonResponse(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(data); err != nil {
		log.Printf("Error encoding response: %v", err)
	}
}

const (
	AdminUser         = "admin"
	AdminPass         = "Lydh@58LTG"
	SessionCookieName = "admin_session"
	SessionToken      = "chuedee_admin_secret_2026"
)

// isAdminAuthenticated checks if the request has a valid admin session cookie
func isAdminAuthenticated(r *http.Request) bool {
	cookie, err := r.Cookie(SessionCookieName)
	if err != nil {
		return false
	}
	return cookie.Value == SessionToken
}

// adminOnly middleware wrapper
func adminOnly(h http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if !isAdminAuthenticated(r) {
			http.Redirect(w, r, "/admin/login", http.StatusSeeOther)
			return
		}
		h(w, r)
	}
}

func main() {
	// Initialize Database Connection

	// Initialize FCM (Firebase Cloud Messaging)
	serviceAccountPath := os.Getenv("FCM_SERVICE_ACCOUNT_PATH")
	if serviceAccountPath == "" {
		serviceAccountPath = "/home/tayap/firebase-service-account.json"
	}
	if err := services.InitFCM(serviceAccountPath); err != nil {
		log.Printf("Warning: FCM initialization failed: %v", err)
	}
	database.Connect()
	defer database.DB.Close()

	// Initialize MySQL Connection
	database.ConnectMySQL()
	if database.MySQLDB != nil {
		defer database.MySQLDB.Close()
	}

	// Serve Static Files & Uploads
	cwd, _ := os.Getwd()
	uploadsDir := cwd + "/uploads"
	templatesDir := cwd + "/templates"

	// Explicitly register static routes
	http.HandleFunc("/uploads/", func(w http.ResponseWriter, r *http.Request) {
		filePath := filepath.Join(uploadsDir, strings.TrimPrefix(r.URL.Path, "/uploads/"))
		log.Printf("Serving upload: %s (Exists: %v)", filePath, func() bool { _, err := os.Stat(filePath); return err == nil }())
		http.ServeFile(w, r, filePath)
	})
	http.HandleFunc("/api/uploads/", func(w http.ResponseWriter, r *http.Request) {
		filePath := filepath.Join(uploadsDir, strings.TrimPrefix(r.URL.Path, "/api/uploads/"))
		log.Printf("Serving upload via API path: %s (Exists: %v)", filePath, func() bool { _, err := os.Stat(filePath); return err == nil }())
		http.ServeFile(w, r, filePath)
	})
	http.Handle("/templates/", http.StripPrefix("/templates/", http.FileServer(http.Dir(templatesDir))))

	// Assets at root
	staticAssets := []string{"styles.css", "main_highlight.png", "sub_highlight_1.png", "sub_highlight_2.png", "sub_highlight_3.png", "app_screen.png", "favicon.ico", "app_logo.png"}
	for _, asset := range staticAssets {
		assetPath := asset
		http.HandleFunc("/"+asset, func(w http.ResponseWriter, r *http.Request) {
			path := filepath.Join(templatesDir, assetPath)
			http.ServeFile(w, r, path)
		})
	}

	// Handler: /ping
	http.HandleFunc("/ping", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
			return
		}
		jsonResponse(w, http.StatusOK, map[string]string{
			"status":  "ok",
			"version": "1.0.9-landing-v1",
		})
	})

	// Handler: /users
	http.HandleFunc("/users", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
			return
		}

		rows, err := database.DB.Query("SELECT id, username, email, display_name, created_at, is_admin, updated_at FROM users LIMIT 10")
		if err != nil {
			jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
			return
		}
		defer rows.Close()

		var users []models.User
		for rows.Next() {
			var u models.User
			if err := rows.Scan(&u.ID, &u.Username, &u.Email, &u.DisplayName, &u.CreatedAt, &u.IsAdmin, &u.UpdatedAt); err != nil {
				jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
				return
			}
			users = append(users, u)
		}

		// Handle empty result case
		if users == nil {
			users = []models.User{}
		}

		jsonResponse(w, http.StatusOK, users)
	})

	// Handler: Root (Serve Landing Page)
	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/" {
			if r.URL.Path == "/search" || r.URL.Path == "/lab" {
				http.NotFound(w, r)
				return
			}
			// Try serving from templates if not root (fallback for other static assets)
			filePath := filepath.Join(templatesDir, strings.TrimPrefix(r.URL.Path, "/"))
			if _, err := os.Stat(filePath); err == nil {
				http.ServeFile(w, r, filePath)
				return
			}
			http.NotFound(w, r)
			return
		}
		handlers.LandingPageHandler(w, r)
	})

	// Keep /home alias for backward compatibility or explicit landing
	http.HandleFunc("/home", handlers.LandingPageHandler)

	// Move Chat to /chat
	http.HandleFunc("/chat", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/chat.html")
	})

	// Handler: API Documentation
	http.HandleFunc("/api-doc", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/api_doc.html")
	})
	http.HandleFunc("/astro-api", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/astro_api_doc.html")
	})
	http.HandleFunc("/how-ranking", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/ranking_explain.html")
	})

	// Name Handlers
	http.HandleFunc("/names", handlers.GetNames)
	http.HandleFunc("/name-detail", handlers.GetNameDetail)
	http.HandleFunc("/name-similar", handlers.GetSimilarNames)
	http.HandleFunc("/decode", handlers.DecodeNameHandler)
	http.HandleFunc("/recommend", handlers.RecommendHandler)
	http.HandleFunc("/debug-embedding", handlers.DebugEmbeddingHandler)
	http.HandleFunc("/debug-db", handlers.DebugDBHandler)
	http.HandleFunc("/debug-log", handlers.DebugLogHandler)
	http.HandleFunc("/debug-gender-log", handlers.DebugGenderLogHandler)
	http.HandleFunc("/debug-gender-stats", handlers.DebugGenderStatsHandler)
	http.HandleFunc("/openclaw", handlers.OpenClawHandler)
	http.HandleFunc("/api/openclaw", handlers.OpenClawHandler) // Alias for Demo
	http.HandleFunc("/api/v1/ninin/chat", handlers.NininChatHandler)
	http.HandleFunc("/api/v1/ninin/redeem-access", handlers.NininRedeemAccessHandler)
	http.HandleFunc("/api/v1/naming/chat", handlers.NamingAssistantHandler)
	http.HandleFunc("/api/demo/search", handlers.DemoSearchHandler)
	http.HandleFunc("/api/v1/name-search", handlers.MobileSearchHandler)
	http.HandleFunc("/api/v1/name-intent", handlers.NameIntentHandler)
	http.HandleFunc("/api/v1/name-root", handlers.GetNameRootHandler)
	http.HandleFunc("/api/v1/name-keywords", handlers.GetNameKeywordsHandler)
	http.HandleFunc("/api/v1/number-meaning", handlers.GetNumberMeaningHandler)
	http.HandleFunc("/api/v1/number-meaning/all-status", handlers.GetAllPairStatusHandler)
	http.HandleFunc("/api/v1/good-sums", handlers.GetGoodSumsHandler)
	http.HandleFunc("/api/v1/name-suggestions", handlers.GetNameSuggestionsHandler)
	http.HandleFunc("/api/v1/name-meaning", handlers.GetNameMeaningHandler)
	http.HandleFunc("/debug-numbers", handlers.DebugNumbersHandler)
	http.HandleFunc("/api-mobile-doc", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Cache-Control", "no-store, no-cache, must-revalidate")
		http.ServeFile(w, r, "templates/api_mobile_doc.html")
	})
	http.HandleFunc("/naming-demo", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/demo_naming.html")
	})
	http.HandleFunc("/tester", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/test_app.html")
	})
	// --- Admin Auth Handlers ---
	http.HandleFunc("/admin/login", func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodGet {
			if isAdminAuthenticated(r) {
				http.Redirect(w, r, "/admin/dashboard", http.StatusSeeOther)
				return
			}
			http.ServeFile(w, r, "templates/admin_login.html")
			return
		}
		if r.Method == http.MethodPost {
			var creds struct {
				Username string `json:"username"`
				Password string `json:"password"`
			}
			if err := json.NewDecoder(r.Body).Decode(&creds); err != nil {
				http.Error(w, "Invalid request", http.StatusBadRequest)
				return
			}

			log.Printf("Admin login attempt - User: %s, PassLen: %d", creds.Username, len(creds.Password))
			if creds.Username == AdminUser && creds.Password == AdminPass {
				log.Println("Admin login successful")
				http.SetCookie(w, &http.Cookie{
					Name:     SessionCookieName,
					Value:    SessionToken,
					Path:     "/",
					HttpOnly: true,
					MaxAge:   3600 * 24, // 1 day
				})
				w.WriteHeader(http.StatusOK)
				return
			}
			log.Println("Admin login failed: Invalid credentials")
			http.Error(w, "Unauthorized", http.StatusUnauthorized)
		}
	})

	http.HandleFunc("/admin/logout", func(w http.ResponseWriter, r *http.Request) {
		http.SetCookie(w, &http.Cookie{
			Name:     SessionCookieName,
			Value:    "",
			Path:     "/",
			HttpOnly: true,
			MaxAge:   -1,
		})
		http.Redirect(w, r, "/admin/login", http.StatusSeeOther)
	})

	http.HandleFunc("/admin/dashboard", adminOnly(func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/admin_dashboard.html")
	}))
	http.HandleFunc("/admin/users-list", adminOnly(func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/admin_users.html")
	}))
	http.HandleFunc("/admin/orders", adminOnly(func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/admin_orders.html")
	}))
	http.HandleFunc("/admin/bag-colors", adminOnly(func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/admin_bagcolor.html")
	}))
	http.HandleFunc("/admin/naming-examples", adminOnly(func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/admin_naming_examples.html")
	}))
	http.HandleFunc("/dream", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/dream.html")
	})
	http.HandleFunc("/privacy-policy", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/privacy_policy.html")
	})
	http.HandleFunc("/privacy", func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, "/privacy-policy", http.StatusMovedPermanently)
	})
	http.HandleFunc("/policy", func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, "/privacy-policy", http.StatusMovedPermanently)
	})
	// /play-store
	http.HandleFunc("/play-store", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/play_store_assets.html")
	})
	http.HandleFunc("/guide-change-name", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/change_name_guide.html")
	})

	// Dynamic Article Reader
	http.HandleFunc("/article/", handlers.ArticleReaderHandler)
	http.HandleFunc("/articles", handlers.ArticlesListHandler)

	// App Download Redirects
	http.HandleFunc("/download", func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, "https://play.google.com/store/apps/details?id=com.numberniceic.app", http.StatusMovedPermanently)
	})
	http.HandleFunc("/app", func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, "https://play.google.com/store/apps/details?id=com.numberniceic.app", http.StatusMovedPermanently)
	})

	// Naming Example Handlers (Celebrities & Avatars)
	http.HandleFunc("/api/v1/naming-examples", handlers.ListNamingExamplesHandler)
	http.HandleFunc("/api/v1/admin/naming-examples/add", adminOnly(handlers.AddNamingExampleHandler))
	http.HandleFunc("/api/v1/admin/naming-examples/update", adminOnly(handlers.UpdateNamingExampleHandler))
	http.HandleFunc("/api/v1/admin/naming-examples/delete", adminOnly(handlers.DeleteNamingExampleHandler))
	http.HandleFunc("/api/v1/admin/naming-examples/upload", adminOnly(handlers.UploadNamingAvatarHandler))
	http.HandleFunc("/api/v1/saved-names/save", handlers.SaveUserNameHandler)
	http.HandleFunc("/api/v1/saved-names/list", handlers.ListUserSavedNamesHandler)
	http.HandleFunc("/api/v1/saved-names/delete", handlers.DeleteUserSavedNameHandler)

	// Dream Interpretation Handlers
	http.HandleFunc("/search-dream", handlers.SearchDreamHandler)
	http.HandleFunc("/random-dream", handlers.GetRandomDreamHandler)
	http.HandleFunc("/add-dream", handlers.AddDreamHandler)
	http.HandleFunc("/update-dream", handlers.UpdateDreamHandler)
	http.HandleFunc("/delete-dream", handlers.DeleteDreamHandler)
	http.HandleFunc("/get-all-dreams", handlers.GetAllDreamsHandler)

	http.HandleFunc("/api/search-feeling", handlers.SearchFeelingHandler)
	http.HandleFunc("/search-feeling", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/semantic_search.html")
	})
	http.HandleFunc("/admin/add-dream", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/add_dream.html")
	})

	// Assignment Handlers (New)
	http.HandleFunc("/check-assignment", handlers.CheckAssignmentHandler)
	http.HandleFunc("/get-buddha-pangs", handlers.GetBuddhaPangsHandler)
	http.HandleFunc("/assign-buddha", handlers.AssignBuddhaHandler)
	http.HandleFunc("/add-buddha-pang", handlers.AddBuddhaPangHandler)

	// Buddha Assignment Aliases for Android Component
	log.Println("Registering /admin/buddha and /api/buddha routes...")
	http.HandleFunc("/admin/buddha/pangs", handlers.GetBuddhaPangsHandler)
	http.HandleFunc("/admin/buddha/assign", handlers.AssignBuddhaHandler)
	http.HandleFunc("/api/buddha/assigned/", handlers.GetAssignedBuddhaHandler)
	http.HandleFunc("/api/buddha/delete", handlers.DeleteBuddhaAssignmentHandler)
	http.HandleFunc("/api/inauspicious/assigned/", handlers.GetAssignedInauspiciousHandler)

	// Spell & Temple Handlers
	http.HandleFunc("/api/spell/assigned/", handlers.GetAssignedSpellsHandler)
	http.HandleFunc("/api/spell/delete", handlers.DeleteSpellAssignmentHandler)
	http.HandleFunc("/api/temple/assigned/", handlers.GetAssignedSacredTempleHandler)
	http.HandleFunc("/api/temple/delete", handlers.DeleteTempleAssignmentHandler)
	http.HandleFunc("/member/merit/assigned/", handlers.GetAssignedMeritHandler)
	http.HandleFunc("/api/merit/delete", handlers.DeleteMeritAssignmentHandler)

	// Product Management Handlers
	http.HandleFunc("/api/v1/products", handlers.GetProductsHandler)
	http.HandleFunc("/api/v1/product-categories", handlers.GetProductCategoriesHandler)
	http.HandleFunc("/api/v1/admin/products/add", handlers.AddProductHandler)
	http.HandleFunc("/api/v1/admin/products/update", handlers.UpdateProductHandler)
	http.HandleFunc("/api/v1/admin/products/delete", handlers.DeleteProductHandler)
	http.HandleFunc("/api/v1/admin/products/update-shipping", handlers.UpdateShippingStatusHandler)
	http.HandleFunc("/api/v1/admin/categories/add", handlers.AddCategoryHandler)
	http.HandleFunc("/api/v1/admin/categories/update", handlers.UpdateCategoryHandler)
	http.HandleFunc("/api/v1/admin/categories/delete", handlers.DeleteCategoryHandler)
	http.HandleFunc("/admin/zircon-orders", handlers.AdminGetZirconOrdersHandler)

	// Payment Handlers (PaySolutions)
	paymentService := services.NewPaymentService()
	paymentHandler := handlers.NewPaymentHandler(paymentService)
	http.HandleFunc("/api/v1/payment/qr", paymentHandler.CreatePaymentQRHandler)
	http.HandleFunc("/api/v1/payment/status", paymentHandler.PaymentStatusHandler)
	http.HandleFunc("/api/v1/payment/webhook", paymentHandler.PaymentWebhookHandler)
	http.HandleFunc("/api/pay/willback", paymentHandler.PaymentWebhookHandler) // Add user-specified route

	// Android Compatibility API
	http.HandleFunc("/get-assigned-inauspicious", handlers.GetAssignedInauspiciousHandler)
	http.HandleFunc("/get-assigned-buddha", handlers.GetAssignedBuddhaHandler)

	// Admin API
	// http.HandleFunc("/admin/add-name", handlers.AddNameHandler)
	// http.HandleFunc("/admin/update-name", handlers.UpdateNameHandler)
	// http.HandleFunc("/admin/delete-name", handlers.DeleteNameHandler)

	// Call Handler
	// http.HandleFunc("/call/initiate", handlers.InitiateCallHandler)
	// http.HandleFunc("/call/reject", handlers.RejectCallHandler)

	// Phonetic Handlers
	http.HandleFunc("/phonetic-code", handlers.PhoneticHandler)
	http.HandleFunc("/phonetic-match", handlers.CheckPhoneticMatchHandler)
	http.HandleFunc("/recommend-spelling", handlers.RecommendSpellingHandler)

	// Chat Handlers
	http.HandleFunc("/chat/init", handlers.InitChatHandler)
	http.HandleFunc("/chat/send", handlers.SendMessageHandler)
	http.HandleFunc("/chat/poll", handlers.PollMessageHandler)
	http.HandleFunc("/chat/history", handlers.GetHistoryHandler)
	http.HandleFunc("/chat/admin/poll", handlers.AdminPollHandler)
	http.HandleFunc("/chat/admin/recent", handlers.GetRecentAdminMessagesHandler)
	http.HandleFunc("/chat/delete", handlers.DeleteMessageHandler) // DELETE
	http.HandleFunc("/chat/unread-count", handlers.GetUnreadCountHandler)
	http.HandleFunc("/chat/mark-read", handlers.MarkReadHandler)
	http.HandleFunc("/chat/admin/mark-read", handlers.AdminMarkReadHandler)
	http.HandleFunc("/chat/upload", handlers.UploadHandler)
	// http.HandleFunc("/chat/call/initiate", handlers.InitiateCallHandler)
	// http.HandleFunc("/chat/call/reject", handlers.RejectCallHandler)

	// Admin User Management
	http.HandleFunc("/admin/users/list", handlers.ListUsersHandler)
	http.HandleFunc("/admin/member/update-status", handlers.UpdateUserStatusHandler)
	http.HandleFunc("/admin/member/edit", handlers.EditUserHandler)
	http.HandleFunc("/admin/member/delete", handlers.DeleteMemberHandler)

	// Admin API (Android App - BagColor)
	http.HandleFunc("/admin/guest-addresses", handlers.GetGuestAddressesHandler)
	http.HandleFunc("/admin/guest-addresses/toggle-shipping", handlers.ToggleShippingStatusHandler)
	http.HandleFunc("/admin/guest-addresses/update-address", handlers.UpdateGuestAddressHandler)
	http.HandleFunc("/admin/finduser/bagcolor/", handlers.FindUserBagColorHandler)
	http.HandleFunc("/admin/bagcolor/", handlers.GetColorSixHandler)
	http.HandleFunc("/admin/bagcolor", func(w http.ResponseWriter, r *http.Request) {
		switch r.Method {
		case http.MethodPost:
			handlers.InsertBagColorHandler(w, r)
		case http.MethodPut:
			handlers.UpdateBagColorHandler(w, r)
		default:
			http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		}
	})
	http.HandleFunc("/admin/notifications/send-bag-colors", handlers.SendBagColorNotificationHandler)

	// Admin Media Library
	http.HandleFunc("/admin/media", adminOnly(func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/admin_media.html")
	}))
	http.HandleFunc("/api/v1/admin/media/list", adminOnly(handlers.ListMediaHandler))
	http.HandleFunc("/api/v1/admin/media/upload", adminOnly(handlers.UploadMediaHandler))
	http.HandleFunc("/api/v1/admin/media/delete", adminOnly(handlers.DeleteMediaHandler))

	// Admin Article Management
	http.HandleFunc("/admin/articles", adminOnly(func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "templates/admin_articles.html")
	}))
	http.HandleFunc("/api/v1/admin/articles/list", adminOnly(handlers.ListArticlesAdminHandler))
	http.HandleFunc("/api/v1/admin/articles/get", adminOnly(handlers.GetArticleHandler))
	http.HandleFunc("/api/v1/admin/articles/add", adminOnly(handlers.AddArticleHandler))
	http.HandleFunc("/api/v1/admin/articles/update", adminOnly(handlers.UpdateArticleHandler))
	http.HandleFunc("/api/v1/admin/articles/delete", adminOnly(handlers.DeleteArticleHandler))

	// Astro Calendar API Endpoints
	http.HandleFunc("/api/foo-days/", astro.FooDaysHandler)
	http.HandleFunc("/api/foo-days", astro.FooDaysCurrentHandler)
	http.HandleFunc("/api/health", astro.HealthHandler)
	http.HandleFunc("/api/kalagni", astro.KalagniHandler)
	http.HandleFunc("/api/kalagni/age", astro.KalagniByAgeHandler)
	http.HandleFunc("/api/sitti-chok", astro.SittiChokHandler)
	http.HandleFunc("/api/ubath", astro.UbathHandler)
	http.HandleFunc("/api/lokawinat", astro.LokawinatHandler)
	http.HandleFunc("/api/sri-position", astro.SriPositionHandler)
	http.HandleFunc("/api/v1/outfit-miracle/by-age", astro.OutfitByAgeHandler)
	http.HandleFunc("/api/v1/outfit-miracle/by-birth-day", astro.OutfitByBirthDayHandler)
	http.HandleFunc("/api/v1/outfit-miracle/by-current-day", astro.OutfitByCurrentDayHandler)
	http.HandleFunc("/api/v1/outfit-miracle/color-sets", astro.OutfitColorSetsHandler)
	http.HandleFunc("/api/v1/outfit-miracle/day-palettes", astro.OutfitDayPalettesHandler)

	// Wanpra API Endpoints
	http.HandleFunc("/api/v1/wanpra/calculate", astro.WanpraHandler)

	log.Println("Server starting on port 8095...")
	// Run the server
	if err := http.ListenAndServe(":8095", nil); err != nil {
		log.Fatalf("Server failed to start: %v", err)
	}
}
