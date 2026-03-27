# Numberniceic Super Apps

Monorepo for the `Number` / `numberniceic` experience and its supporting services. Everything lives at this root so the mobile clients, Go backend, and PHP backend share a single working tree and a single `.gitignore` / automation surface.

## Top-level contents
- `.agents/`: Codex skill metadata used by AGENTS.md and the available `android-debug`, `flutter-app`, `go-service`, and `php-backend` workflows.
- `AGENTS.md`: instructions on routing work through the right skill, how the stacks interact, and the safety rule that discourages touching runtime business logic without explicit direction.
- `NAME_RANKING_AND_PREMIUM.md`: describes the shared ranking/premium logic between `go-naming` and `flutter-naming` and explains how the API contract should behave.
- `flutter-naming/`: Flutter client that renders ranked names, premium overlays, and serves the naming UX on mobile/web/desktop.
- `number-androidx/`: Native Android (Kotlin/Retrofit/FCM) client with Gradle wrappers, release scripts, and a lot of deployment artifacts for Play Store submission.
- `go-naming/`: Go-based naming/chat/astrology backend (handlers, vector data, Go modules, deployment helpers, OpenAI/Redis setup notes).
- `number-php/`: PHP/Slim backend powering members, notifications, and admin tooling with `app/`, `public/`, `configs/`, and dozens of deploy scripts / ZIP artifacts.

## Component relationships
- `number-androidx` talks to **both** backends: PHP for member/auth/notification flows and Go for naming/chat/astrology/outfit APIs. Everything in AGENTS.md under “Frontend <-> Backend Interaction” describes the current endpoints.
- `flutter-naming` is a dedicated naming client that consumes `go-naming` (`https://xn--b3cu8e7ah6h.com/api/v1/*`). It displays rankings from `final_rank_score` and the backend `rank_reasons`, as described in `NAME_RANKING_AND_PREMIUM.md`.
- The PHP stack (`number-php`) currently drives member content, notifications, and admin utilities. It exposes REST endpoints under `public/` and is referenced by `number-androidx` for auth and notifications.
- Go (`go-naming`) is the source of truth for name scoring, chat, and related analytics; Android and Flutter clients rely on its JSON payloads.

## Project-specific notes

### `flutter-naming`
- **Purpose**: Flutter UI for ranking names and premium paywall flows. Still ships Android/iOS/web/desktop artifacts and carries `assets/`, `lib/`, `android/`, `ios/`, `web/`, etc.
- **Important files**: `pubspec.yaml` (packages), `lib/src/screens/naming_screen.dart`, `lib/src/models/name_model.dart`, the `assets/` folder, `nginx_frontend.conf`, and `PREMIUM_SYSTEM_PLAN.md`.
- **Prerequisites**: Flutter SDK (stable); Android/iOS toolchains as needed.
- **Key commands**:
  1. `flutter pub get`
  2. `flutter analyze` / `flutter test`
  3. `flutter run -d <device>` for dev/testing.
  4. `flutter build apk` / `flutter build appbundle` / `flutter build ios` depending on target.
  5. `run_flutter.sh` lives under `number-androidx/` and orchestrates the Flutter artifact alongside the native app when required.
- **Risks**: The folder also stores signed APK/AAB artifacts (`Chuedee_*`, `v1.4.0+25.aab`, etc.). None of these files should be regressed into build outputs again; consider moving future release binaries to a dedicated `artifacts/` directory and keeping source artifacts separate.

### `number-androidx`
- **Purpose**: Native Android client built with Gradle. Contains `app/`, deployment scripts, release notes, signed/unsigned binaries, and diagnostic helpers.
- **Important files**: `app/build.gradle`, `settings.gradle`, `gradlew`, `gradle.properties`, `local.properties` (machine-specific), `app/src/main/java` and `res`, release-checklist docs, and helpful scripts such as `BUILD_RELEASE_APK.sh`, `build_google_play_release.sh`, `install_all.sh`, `run_on_emulator.sh`, `run_flutter.sh`, `deploy_flutter.sh`, and `run_go.sh` (for integrated flows).
- **Prerequisites**: JDK 17+, Android SDK + build tools, Gradle wrapper handles most.
- **Commands**:
  1. `./gradlew clean` / `./gradlew assembleDebug` / `./gradlew bundleRelease`.
  2. Use `BUILD_RELEASE_APK.sh` or `build_google_play_release.sh` for Play Store bundles (see `QUICK_START.md`).
  3. `./run_android.sh` / `run_all_devices.sh` may target attached devices.
  4. `./run_flutter.sh` / `deploy_flutter.sh` glue Flutter artifact deployment into the native pipeline.
- **Configs**: `app/build.gradle` describes version code/name; `gradle.properties` sets keystore handles; `local.properties` (ignored) picks up SDK/NDK paths.
- **Risks**: Contains zipped PHP artifacts and release binaries at the repo root inside `number-androidx/`. These inflate repo size and should ideally live outside the Git repo or under a documented `releases/` dir.

### `go-naming`
- **Purpose**: Go backend responsible for name ranking, chat, astrology, and outfits. Implements `handlers/`, `models/`, `services/`, and `server` logic.
- **Important files**: `main.go` (entry point, contains database credentials), `go.mod` / `go.sum`, `handlers/mobile_search_handler.go`, `docs/`, `tools/`, `deploy_*.sh`, and vector data helpers (`update-vectors`, `vector*.json`).
- **Prerequisites**: Go toolchain (>=1.20) and network access to the remote PostgreSQL host (`43.228.85.200`). Check `README.md`/`SETUP_OPENAI.md` for DB/AI requirements.
- **Commands**:
  1. `go mod tidy` (pull deps).
  2. `go run main.go` to start the API server locally (it prints DB table info during startup).
  3. `./install_and_build.sh` / `./deploy_pkg` / `./deploy_server.sh` automates builds and deployments.
  4. Use `test_build_go.sh` / `test_local_api.go` for sanity checks.
- **Configs**: Database settings are currently hardcoded in `main.go`, but the repo has `database/`, `templates/`, and `configs` directories for other helpers. `go-naming.service(.remote)` shows how the service is launched in production.

### `number-php`
- **Purpose**: PHP/Slim backend handling auth/member content/notifications/admin tools. Contains procedural scripts, deploy bundles, and historical `.php` helpers.
- **Important files**: `app/` (controllers, Managers), `public/index.php`, `configs/` (database configs), `composer.json`/`composer.lock`, `README_DEPLOY.md`, numerous `deploy_*.sh` scripts, `notification_system_*.zip`, `error_log`, and `app/Managers/NotificationManager.php` for the notification system described in the deploy doc.
- **Prerequisites**: PHP 8+ CLI/web, Composer, MySQL/PostgreSQL, Apache/Nginx (scripts assume `public/` as web root).
- **Commands**:
  1. `composer install` (sets up autoloader, vendor directories).
  2. `php -S localhost:8000 -t public` for quick local testing.
  3. `./deploy.sh` / `start.sh` / `run_debug.sh` orchestrate environment-specific entry points.
  4. Use `php scripts/<script>.php` or `run_<something>.sh` to run maintenance tasks.
- **Configs**: `configs/database.php`/`configs/app.php`, `app/routes.php`, and `public/index.php` define request handling and middleware. Deployment docs mention `notification_system_*.zip` and SQL migrations located in this folder.
- **Risks**: The directory already tracks many archive files (`*.zip`, `*.tar.gz`) and debug logs. Keep those out of future commits by putting new release bundles into a dedicated `artifacts/` or `deployments/` folder and leaving caches/logs to `.gitignore`.

## Documentation & AI readiness
- `AGENTS.md` is the go-to overview of the stack, routing rules, and the cannot-touch-business-logic safety rule. Codex agents rely on `.agents/skills/*/SKILL.md` for task guidance.
- `NAME_RANKING_AND_PREMIUM.md` maps out the ranking/premium contract between the Flutter client and Go backend.
- Each service folder has its own README (Flutter stub, Go backend README, PHP deploy doc, Android quick-start). Update those README files so they describe real workflows rather than default templates.
- **Missing docs**: no single root README existed (now created), there is no plain-English “how to bootstrap everything” guide listing exact SDK versions, or `gotchas` for connecting to the external database or PHP stack. Keep `README.md`, `AGENTS.md`, and `NAME_RANKING_AND_PREMIUM.md` in sync to help new contributors and AI agents understand the interplay.

## High-level run matrix / prerequisites
| Component | Prerequisites | Primary run command(s) | Entry point / config |
| --- | --- | --- | --- |
| `flutter-naming` | Flutter SDK, Android/iOS toolchain | `flutter pub get`, `flutter run`, `flutter build apk/appbundle` | `lib/main.dart`, `pubspec.yaml`, `assets/`, `nginx_frontend.conf` |
| `number-androidx` | JDK, Android SDK, keystore | `./gradlew clean assembleDebug`, `./gradlew bundleRelease`, `./build_google_play_release.sh` | `app/build.gradle`, `gradle.properties`, `scripts/` under `number-androidx/` |
| `go-naming` | Go toolchain, access to PostgreSQL host | `go mod tidy`, `go run main.go`, `./install_and_build.sh` | `main.go`, `handlers/`, `go.mod`, `deploy_*.sh` |
| `number-php` | PHP + Composer + MySQL/PostgreSQL | `composer install`, `php -S localhost:8000 -t public`, `./deploy.sh`, `./start.sh` | `public/index.php`, `app/`, `configs/`, `vendor/` |

## Suggestions for documentation improvements
1. Keep each subproject README focused on how to run/build it (e.g., `flutter-naming/README.md` still contains the default Flutter template and should list the actual SDK commands, emulator setup, how it contacts `go-naming`, and where to find the `PREMIUM` logic).
2. Add a `docs/` or `playbooks/` folder for cross-stack runbooks (e.g., how to wire Android → PHP → Go for tests, or how to replicate the notification deployment described in `README_DEPLOY.md`).
3. Track environment prerequisites centrally (Flutter version, JDK version, Go version, PHP version, DB host). Consider adding a `setup.md` referencing these, plus the open AI/vector requirements from `go-naming/SETUP_OPENAI.md`.
4. Maintain an `artifacts/` or `releases/` folder for large generated binaries/deployable zips so that they are easier to archive or clean without git tracking them in random directories.

## Risks and to-dos
- Several directories currently contain large binaries/logs (e.g., `Chuedee_*.apk`, `number-php/*.zip`, `go-naming/deploy_pkg.tar.gz`). Limit future commits to source files and keep those release artifacts behind a separate storage process.
- Some config values (e.g., database credentials in `go-naming/main.go`, `number-androidx/local.properties`, `number-php/configs/database.php`) are environment-specific. Do not commit local overrides and document the expected location of secrets.
- No automated tests or CI scripts are part of this repo yet—adding a lightweight `scripts/ci/` stub or verifying instructions in each README would help AI agents know how to run validation.
- Once a README is kept here, keep it synchronized with `AGENTS.md` (who should be contacted for tasks, what skill to use, etc.) so Codex agents always have the latest context.
