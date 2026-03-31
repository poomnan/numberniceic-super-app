# Numberniceic Super Apps

Monorepo for the Numberniceic ecosystem. This repository contains:
- `number-androidx`: native Android app
- `flutter-naming`: Flutter naming client
- `number-php`: PHP/Slim member/content/notification backend
- `go-naming`: Go naming/chat/astrology backend

## Where To Start

New contributors and AI agents should read these in order:
1. `docs/PROJECT_MAP.md`
2. `docs/ARCHITECTURE.md`
3. `docs/API_CONTRACT.md`
4. `docs/DOMAIN_RULES.md`
5. `AGENTS.md`
6. `ai/AGENT_RULES.md`
7. `ai/CONTEXT.md`

## Project Overview

### `number-androidx`
- Kotlin Android app
- Uses Retrofit + FCM
- Talks to both backends
- Main hosts are configured in `number-androidx/app/src/main/java/com/numberniceic/https/BackendHosts.kt`

### `flutter-naming`
- Flutter app for naming search/ranking
- Calls Go naming APIs only
- Main API wrapper: `flutter-naming/lib/src/services/api_service.dart`

### `number-php`
- PHP/Slim backend
- Handles auth, member info, notifications, lucky number, content, and admin tools
- Main route file: `number-php/app/routes.php`

### `go-naming`
- Go backend
- Handles naming ranking, semantic search, astrology, wanpra, chat, products, and some admin routes
- Main entry point: `go-naming/main.go`

## Run Commands

### Android
```bash
cd number-androidx
./gradlew assembleDebug
```

### Flutter
```bash
cd flutter-naming
flutter pub get
flutter run
```

### Go backend
```bash
cd go-naming
go mod tidy
go run main.go
```

### PHP backend
```bash
cd number-php
composer install
php -S localhost:8000 index.php
```

## Frontend ↔ Backend Map

### Android
- Default base URL: `https://numberniceic.online`
- Also calls Go directly at `https://xn--b3cu8e7ah6h.com`

### Flutter
- Calls Go directly at `https://xn--b3cu8e7ah6h.com`

## Important Warnings

- Do not assume one backend owns all business logic.
- Naming score logic lives in Go and must stay consistent with Flutter display assumptions.
- Member/auth/notification logic lives in PHP and drives Android access behavior.
- Rengyam behavior is split across PHP tag generation and Android filtering/display logic.
- Many API contracts are implicit in code rather than formal schemas.
- This repository contains many deploy/debug/temp artifacts; do not confuse them with active entry points.

## Important Files
- `docs/PROJECT_MAP.md`
- `docs/ARCHITECTURE.md`
- `docs/API_CONTRACT.md`
- `docs/DOMAIN_RULES.md`
- `AGENTS.md`
- `ai/AGENT_RULES.md`
- `ai/CONTEXT.md`
- `ai/PROMPT_TEMPLATE.md`

## Practical Workflow For New Agents
1. Read the docs listed above.
2. Identify which project owns the task.
3. Find the source of truth before editing.
4. Make the smallest safe change.
5. Validate only in the project(s) you touched.
6. If you discover a hidden rule, document it.
