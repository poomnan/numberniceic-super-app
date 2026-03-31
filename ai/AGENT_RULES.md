# Agent Rules

These rules are for any future AI agent or new contributor working in this monorepo.

## 1. Read Before Editing
Always read these first:
- `README.md`
- `docs/PROJECT_MAP.md`
- `docs/ARCHITECTURE.md`
- `docs/API_CONTRACT.md`
- `docs/DOMAIN_RULES.md`
- `AGENTS.md`

## 2. Work In The Correct Project
Do not mix concerns across projects.
- Android UI/network changes: `number-androidx`
- Flutter naming UI/client contract changes: `flutter-naming`
- Naming/chat/astrology backend changes: `go-naming`
- Member/content/notification/admin backend changes: `number-php`

## 3. Do Not Move Business Logic Across Stacks Without Explicit Request
Examples:
- Do not copy naming ranking logic from Go into Flutter as a new source of truth.
- Do not rewrite PHP member logic inside Android.
- Do not move rengyam rules from PHP to Android unless the task explicitly requires it.

## 4. Preserve Existing Contracts
Before changing any endpoint or response field:
- inspect the client model
- inspect the server struct/controller
- verify whether another client also depends on that response

Fields that are especially sensitive:
- `final_rank_score`
- pair type / pair point fields
- `rank_reasons`
- `rengyam_access`
- notification payloads
- `display_tags` and related auspicious calendar fields

## 5. Safe Cross-Project Change Rule
If a task touches more than one project, follow this order:
1. Identify the source of truth
2. Change the source of truth first
3. Update dependent clients second
4. Validate the contract shape after the change

## 6. Treat These Files As High-Risk
Read carefully before editing:
- `go-naming/main.go`
- `go-naming/handlers/mobile_search_handler.go`
- `go-naming/services/numerology_service.go`
- `number-php/app/routes.php`
- `number-php/app/Managers/UserController.php`
- `number-php/app/Managers/ThaiCalendarHelper.php`
- `number-androidx/app/src/main/java/com/numberniceic/https/ApiService.kt`
- `number-androidx/app/src/main/java/com/numberniceic/ui/renkyam/RengYamF.kt`
- `flutter-naming/lib/src/services/api_service.dart`
- `flutter-naming/lib/src/models/name_model.dart`

## 7. Respect Existing Domain Behavior
- Naming score calculations must stay aligned between Go and Flutter display assumptions.
- Android rengyam screens depend on both backend tags and Android-side filters.
- PHP login/member info responses may drive feature access in Android.

## 8. Prefer Additive, Backward-Compatible Changes
Good changes:
- add a new response field while preserving old ones
- add documentation
- add admin tooling that reuses current data flow

Risky changes:
- renaming/removing JSON fields
- changing score semantics
- changing meaning of VIP/access flags
- replacing backend-derived logic with client-only logic

## 9. Validate In The Local Project You Touched
Minimum expectation:
- Android: run a Gradle build if code changed
- Flutter: run `flutter analyze` or targeted build if changed
- Go: run `go build` or the relevant package/server build if changed
- PHP: run syntax or smoke check if possible

## 10. Document Non-Obvious Decisions
If you discover a hidden rule while fixing something:
- update `docs/DOMAIN_RULES.md` or `docs/API_CONTRACT.md`
- keep future agents from having to rediscover it from chat history
