---
name: flutter-app
description: Use when tasks involve Flutter UI/UX behavior, state issues, API integration in flutter-naming, request payload bugs, or mismatch between Flutter client models and Go API responses.
---

# Flutter App Skill (`flutter-naming`)

Use this skill for Flutter app work, especially client-side API usage and screen behavior.

## Code Areas
- App entry: `flutter-naming/lib/main.dart`
- Screens: `flutter-naming/lib/src/screens`
- API client: `flutter-naming/lib/src/services/api_service.dart`
- Models: `flutter-naming/lib/src/models`
- UI widgets: `flutter-naming/lib/src/widgets`

## Trigger Hints
Use this skill for prompts containing:
- Flutter screen bug, loading/error state, rendering issue
- Name search response parsing issue
- API failure from `https://xn--b3cu8e7ah6h.com/api/v1/*`
- Request body/parameter mismatch for `name-search`, `name-root`, `number-meaning`, `saved-names`

## Integration Contract Notes
- Flutter client targets the Go naming backend domain.
- `ApiService` is the source of truth for request structure and timeout/error handling.
- Keep UI behavior aligned with API semantics before adding local workarounds.
- Ranking flow uses backend-provided `final_rank_score` and `rank_reasons` when present.
- Premium flow is simplified: do not add extra trial gates in search flow; use the name-overlay tap action as the primary paywall trigger.

## Ranking + Premium Rules (Must Keep)
- For ranking list ordering, prefer backend score (`final_rank_score`) first.
- For non-VIP users, premium-name lock should be visual overlay on the name area, not full-card blur.
- Overlay tap should open paywall directly.
- Do not hide/remove premium rows from list; show the card and lock only the final name display.
- In `similarMode` (`รวมให้เป็น "ชื่อดี"`), the card shows both base-name scores and combined `total` scores. If `เลขศาสตร์ดี` or `พลังเงาดี` is active, do not filter by `total` alone; require the visible base score to pass too, or users will see red circles in a "good" filtered list.
- When computing ranking statistics in `similarMode`, use the same pass/fail rule as the visible list so summary counts do not disagree with the rendered cards.
- If ranking cards visually expose `pairType` strength (`D5/D8/D10`) or `pairpoint`, the order should not contradict that visual signal for names with the same meaning. In that case, numerology quality should win before semantic tie-break noise.
- Debug/proof UI for ranking should stay compact. Prefer merging `pairType/pairpoint` into existing `เลขศาสตร์` / `พลังเงา` lines on the back of the card rather than adding a separate debug panel that repeats the same information.

## Known UX / Screen Notes
- The compact status banner above results is presentation-only. It should not behave like a navigation CTA unless it truly changes the result set.
- In the ranking cards, the matching/combined section should stay compact. Extra label rows can create misleading empty space inside the card.

## Debug Workflow
1. Reproduce in the target screen and capture endpoint called.
2. Verify `ApiService` URL, query/body shape, and timeout behavior.
3. Validate model parsing against real JSON.
4. Confirm state transitions in screen widget (`loading`, `success`, `error`).
5. If needed, compare behavior with Android implementation for parity.
