---
name: php-backend
description: Use when tasks involve number-php API routes, Slim/PHP controllers, member auth/login, notification APIs, or Android-to-PHP request debugging and response-shape fixes.
---

# PHP Backend Skill (`number-php`)

Use this skill when debugging or implementing endpoints in the legacy/main PHP backend.

## Code Areas
- Route map: `number-php/app/routes.php`
- Controllers/managers: `number-php/app/Managers`
- Config and DB wiring: `number-php/configs`
- Views/admin pages: `number-php/views`

## Trigger Hints
Use this skill for prompts containing:
- `/member/login`, `/member/updateToken`, `/member/info/{memberid}`
- `/api/v2/notifications*` or notification history issues
- member/profile updates, admin web login flow, or route/controller mismatch
- Android receives unexpected payload/status from PHP backend

## Integration Contract Notes
- Android consumes many PHP endpoints via Retrofit in `number-androidx`.
- Preserve JSON field compatibility (`memberid` string formatting is important in some endpoints).
- Verify route method/path exactly as used by clients before changing controller logic.

## Debug Workflow
1. Locate route definition in `app/routes.php`.
2. Trace to manager/controller implementation.
3. Validate DB query assumptions and response JSON schema.
4. Compare against Android request model and expected response model.
5. Prefer additive, backward-compatible fixes for existing clients.
