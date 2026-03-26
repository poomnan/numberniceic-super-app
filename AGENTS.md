# AGENTS: Numberniceic Super Apps

This repository hosts multiple apps/services that work together across mobile clients and backend APIs.

## Project Structure
- `number-androidx`: Android app (Kotlin, Retrofit, FCM).
- `flutter-naming`: Flutter client for naming features.
- `number-php`: PHP/Slim backend for core member/content/notification APIs.
- `go-naming`: Go backend for naming, chat, astrology, and related APIs.
- `.agents/skills`: Shared Codex skills for task-specific routing.

## Frontend <-> Backend Interaction
- Android (`number-androidx`) calls:
  - PHP backend (`number-php`) for member/auth/content/notification flows.
  - Go backend (`go-naming`) for naming/chat/astrology and outfit APIs.
- Flutter (`flutter-naming`) calls:
  - Go backend (`go-naming`) via `https://xn--b3cu8e7ah6h.com/api/v1/*`.
- Cross-stack debugging should verify endpoint, host, method, payload, and response model consistency.

## Skill Routing Rules
Use the matching skill before editing code in that area:
- `android-debug`: Android issues (`number-androidx`).
- `php-backend`: PHP API and controller issues (`number-php`).
- `go-service`: Go service/API/business logic (`go-naming`).
- `flutter-app`: Flutter UI/client integration (`flutter-naming`).

Skill locations:
- `.agents/skills/android-debug/SKILL.md`
- `.agents/skills/php-backend/SKILL.md`
- `.agents/skills/go-service/SKILL.md`
- `.agents/skills/flutter-app/SKILL.md`

## Usage Examples
- "trace login flow between android and backend"
  - Start with `android-debug`, then `php-backend` for `/member/login` and `/member/info`.
- "debug API failure"
  - Choose `php-backend` or `go-service` based on endpoint host/path, then compare client contract.
- "compare request between client and server"
  - Use `android-debug` or `flutter-app` for client payload, then `php-backend`/`go-service` for server handler expectations.

## Safety Rule
Do not modify runtime business logic unless explicitly requested. Prefer documenting contracts, tracing flow, and making backward-compatible fixes.
