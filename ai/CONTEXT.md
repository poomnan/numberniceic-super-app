# Context

## Purpose
This monorepo powers the Numberniceic ecosystem:
- Android app for the main product
- Flutter app for naming search/ranking
- PHP backend for members/content/notifications/admin
- Go backend for naming, semantic search, astrology, chat, and supporting APIs

## Key Structures
- Android network layer: `number-androidx/app/src/main/java/com/numberniceic/https`
- Flutter network/models: `flutter-naming/lib/src/services`, `flutter-naming/lib/src/models`
- Go handlers/services: `go-naming/handlers`, `go-naming/services`
- PHP routes/managers: `number-php/app/routes.php`, `number-php/app/Managers`

## Important Constraints
- Android talks to both PHP and Go.
- Flutter naming talks to Go only.
- Naming ranking source of truth is Go.
- Member/auth/notification source of truth is PHP.
- Rengyam behavior is split: PHP generates tags, Android filters/displays them.
- Many contracts are implicit in code, so client and server models must be checked together.

## Start Here
1. Read `docs/PROJECT_MAP.md`
2. Read `docs/API_CONTRACT.md`
3. Read `docs/DOMAIN_RULES.md`
4. Then open only the project relevant to your task
