---
name: android-debug
description: Use when tasks involve Android app bugs, Retrofit/API mismatches, login/session issues, notification/token problems, or tracing request/response flow between number-androidx and backend services.
---

# Android Debug Skill (`number-androidx`)

Use this skill when the task is about Android runtime issues, API integration failures, or mobile-to-backend behavior mismatch.

## Code Areas
- Android app source: `number-androidx/app/src/main/java/com/numberniceic`
- Retrofit contract: `number-androidx/app/src/main/java/com/numberniceic/https/ApiService.kt`
- Backend host config: `number-androidx/app/src/main/java/com/numberniceic/https/BackendHosts.kt`
- FCM token sync: `number-androidx/app/src/main/java/com/numberniceic/services/MyFirebaseMessagingService.kt`

## Trigger Hints
Use this skill immediately for prompts containing:
- Android crash, ANR, fragment/activity behavior
- `member/login`, `member/info`, `updateToken`, or notification API issues
- Retrofit parse error, HTTP 4xx/5xx from app, JSON model mismatch
- "works in backend but not in Android", request payload mismatch, auth/session mismatch

## Primary API Connections
- Content/PHP backend via `BackendHosts.CONTENT_BASE` (e.g. `/member/login`, `/api/v2/notifications`)
- Go naming backend via `BackendHosts.NAMING_BASE` (e.g. `/api/v1/name-search`, `/api/v1/outfit-miracle/*`)

## Debug Workflow
1. Confirm target endpoint and host (CONTENT vs NAMING).
2. Verify Retrofit annotation + path/query/body match server contract.
3. Validate Kotlin model field names/types (especially nullable vs non-null).
4. Trace token/session flow: login -> local persistence -> authenticated calls.
5. Check cross-service assumptions when Android calls both PHP and Go in one user flow.

## Domain Notes
The app includes outfit-color and calendar logic that depends on backend-provided palette/astrology endpoints. Prioritize end-to-end consistency over local-only fixes.
