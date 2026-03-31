# Project Map

This repository is a monorepo for the Numberniceic ecosystem. It contains two client applications and two backend services.

## Projects

### `number-androidx`
- Role: Native Android application for the broader Numberniceic product.
- Stack: Kotlin, Android Views/Fragments, Retrofit, Firebase Cloud Messaging.
- Entry points:
  - Android app manifest: `number-androidx/app/src/main/AndroidManifest.xml`
  - Launcher activity: `com.numberniceic.ui.SplashActivity`
  - Main shell activity: `com.numberniceic.ui.AppActivity`
- Build/run:
  - `cd number-androidx`
  - `./gradlew assembleDebug`
  - `./gradlew bundleRelease`
- Notes:
  - Talks to both backends.
  - Uses `https://numberniceic.online` as the default Retrofit base URL.
  - Calls Go endpoints via absolute URLs under `https://xn--b3cu8e7ah6h.com`.

### `flutter-naming`
- Role: Flutter naming-focused client for name search, ranking, saved names, and premium flows.
- Stack: Flutter, Dart, HTTP, SharedPreferences.
- Entry points:
  - App bootstrap: `flutter-naming/lib/main.dart`
  - Initial screen: `NamingScreen`
- Build/run:
  - `cd flutter-naming`
  - `flutter pub get`
  - `flutter run`
  - `flutter analyze`
  - `flutter test`
- Notes:
  - Targets `https://xn--b3cu8e7ah6h.com` only.
  - Main API wrapper lives in `flutter-naming/lib/src/services/api_service.dart`.

### `go-naming`
- Role: Go backend for naming, semantic search, numerology, chat, astrology, wanpra/outfit APIs, products, and selected admin pages.
- Stack: Go `net/http`, PostgreSQL, MySQL, HTML templates.
- Entry points:
  - Server bootstrap: `go-naming/main.go`
  - Main route registration also lives in `go-naming/main.go`
- Build/run:
  - `cd go-naming`
  - `go mod tidy`
  - `go run main.go`
- Notes:
  - Default HTTP server port from code: `:8095`.
  - Contains many debug tools and temp scripts; they are not the main service entry point.

### `number-php`
- Role: PHP/Slim backend for auth, member data, content, notifications, lucky-number flows, and web admin tools.
- Stack: PHP 8+, Slim 4, PHP-DI, Slim PHP View, PDO.
- Entry points:
  - Front controller: `number-php/index.php`
  - Route table: `number-php/app/routes.php`
- Build/run:
  - `cd number-php`
  - `composer install`
  - `php -S localhost:8000 index.php`
- Notes:
  - `index.php` serves `public/` assets when using the PHP built-in server.
  - Large amount of web-admin and API logic lives directly in `app/routes.php` and `app/Managers/`.

## Shared And Cross-Cutting Logic

### API contracts shared across client/server
- Android ↔ PHP/Go contracts are defined implicitly by Retrofit models in `number-androidx/app/src/main/java/com/numberniceic/data` and `https/ApiService.kt`.
- Flutter ↔ Go naming contract is defined by:
  - request code in `flutter-naming/lib/src/services/api_service.dart`
  - response models in `flutter-naming/lib/src/models/name_model.dart`
  - server structs in `go-naming/handlers/mobile_search_handler.go`

### Domain logic split
- Naming and ranking logic: primarily `go-naming/services/` and `go-naming/handlers/mobile_search_handler.go`
- Rengyam / auspicious calendar logic: primarily `number-php/app/Managers/ThaiCalendarHelper.php`
- Android UI interpretation of auspicious data: primarily `number-androidx/app/src/main/java/com/numberniceic/ui/renkyam/RengYamF.kt`

## Frontend ↔ Backend Relationship Map

### Android (`number-androidx`)
Uses `numberniceic.online` as the default base URL and calls PHP routes such as:
- `POST /member/login`
- `GET /member/info/{memberid}`
- `GET /member/lengyam`
- `GET /member/wanpra/{wandate}`
- `GET /api/v2/notifications`

Also calls Go routes via absolute URL constants, such as:
- `POST /api/v1/wanpra/calculate`
- `GET /api/kalagni`
- `GET /api/foo-days`
- `POST /api/v1/ninin/chat`
- `POST /api/v1/naming/chat`
- `GET /api/v1/products`

### Flutter (`flutter-naming`)
Calls Go naming endpoints only, including:
- `POST /api/v1/name-search`
- `GET /api/v1/name-suggestions`
- `GET /api/v1/name-meaning`
- `GET /decode`
- `GET /api/v1/name-root`
- `GET /api/v1/number-meaning`

## Directory Heuristics For New Contributors
- `number-androidx/app/src/main/java/com/numberniceic/ui`: screens and UI workflows
- `number-androidx/app/src/main/java/com/numberniceic/https`: network layer and host routing
- `flutter-naming/lib/src/screens`: Flutter app flows
- `go-naming/handlers`: HTTP handlers and request/response contracts
- `go-naming/services`: business logic helpers
- `number-php/app/Managers`: PHP controllers/managers/business helpers
- `number-php/views`: web admin and HTML views

## Build/Run Summary Table

| Project | Primary command | Purpose |
| --- | --- | --- |
| `number-androidx` | `./gradlew assembleDebug` | Build Android debug APK |
| `flutter-naming` | `flutter run` | Run Flutter naming client |
| `go-naming` | `go run main.go` | Start Go backend |
| `number-php` | `php -S localhost:8000 index.php` | Start PHP backend locally |
