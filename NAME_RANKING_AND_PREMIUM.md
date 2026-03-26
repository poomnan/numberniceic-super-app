# Name Ranking + Premium Logic (Current)

This document summarizes the current behavior of the "ชื่อดี" ranking and monetization flow across backend (`go-naming`) and frontend (`flutter-naming`).

## Executive Summary
- Ranking is **server-first**: backend computes `final_rank_score` and `rank_reasons`.
- Flutter still has local fallback scoring, but prefers backend score when present.
- Premium UX is now simplified to **one monetization action**:
  - names that are "double green" are shown with an overlay badge
  - user taps the badge to unlock via paywall
- Search filters are not trial-gated in the main flow anymore.

## Backend (Go) - Source of Truth
File: `go-naming/handlers/mobile_search_handler.go`

### What backend now returns
- `final_rank_score` (int)
- `rank_reasons` (string[])
- plus existing fields (`sat_sum`, `sha_sum`, `is_sat_good`, `is_sha_good`, etc.)

### Rank formula (current)
- Base: semantic similarity from `distance` -> `0..100`
- Bonuses:
  - SAT pass: `+20`
  - SHA pass: `+20`
  - SAT+SHA pass: `+50`
  - no kaki chars: `+10` (when highlight info exists)
  - length bonus: `+15/+10/+5/-5` by name length bands
- Normalize by `/215` to a 0-100 score.

### Sort order (current)
1. `final_rank_score` desc
2. `semantic_score` desc
3. `hybrid_score` desc
4. `name` asc

## Frontend (Flutter) - Display + Monetization
Files:
- `flutter-naming/lib/src/models/name_model.dart`
- `flutter-naming/lib/src/screens/naming_screen.dart`
- `flutter-naming/lib/src/widgets/name_list_item.dart`

### Ranking behavior
- Client sorts by backend `final_rank_score` when available.
- `calculateScore()` keeps a local fallback for safety.
- Score explanation UI uses backend `rank_reasons` where available.

### Premium behavior (current target)
- Non-VIP users can still browse rankings.
- For "double green" names:
  - the name area is covered by a badge overlay
  - tapping the badge triggers paywall
- This keeps card aesthetics visible while hiding the final premium name.

## Is the algorithm + conditions OK?
Yes, with current goals it is in a good state:
- transparent ranking reasons
- consistent server-driven ordering
- simple monetization trigger

Residual risk to monitor:
- if API ever omits `final_rank_score`, fallback scoring will apply.
- ranking quality is still sensitive to semantic query quality and vector data completeness.

## Recommended guardrails (next)
1. Add a small API contract test asserting `final_rank_score` and `rank_reasons` always exist.
2. Add a UI test for premium overlay:
   - non-VIP + double green => overlay shown
   - VIP => overlay hidden
3. Keep ranking formula changes backend-only and versioned in release notes.
