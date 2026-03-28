# Name Ranking + Premium Logic (Current)

This document summarizes the current behavior of the "ชื่อดี" ranking and monetization flow across backend (`go-naming`) and frontend (`flutter-naming`).

## Executive Summary
- Ranking is **server-first**: backend computes `final_rank_score` and `rank_reasons`.
- Flutter still has local fallback scoring, but prefers backend score when present.
- Ranking now uses both `pairType` and `pairpoint` so names with the same green tier can still be ordered more precisely.
- If two names have effectively the same meaning, the backend should let **numerology quality** decide before semantic tie-breakers.
- Premium UX is now simplified to **one monetization action**:
  - names that are "double green" are shown with an overlay badge
  - user taps the badge to unlock via paywall
- Search filters are not trial-gated in the main flow anymore.

## Backend (Go) - Source of Truth
File: `go-naming/handlers/mobile_search_handler.go`

### What backend now returns
- `final_rank_score` (int)
- `rank_reasons` (string[])
- `semantic_rank_score` (int)
- `numerology_rank_score` (int)
- `pair_type_bonus` (int)
- `pair_point_bonus` (int)
- `sat_bonus` / `sha_bonus` / `double_bonus` / `kaki_bonus` / `length_bonus`
- plus existing fields (`sat_sum`, `sha_sum`, `is_sat_good`, `is_sha_good`, etc.)

### Rank formula (current)
- Base: semantic similarity from `distance` -> `0..100`
- Bonuses:
  - SAT pass: `+20`
  - SHA pass: `+20`
  - SAT+SHA pass: `+50`
  - pair type tier:
    - `D10 = +14`
    - `D8 = +8`
    - `D5 = +3`
  - pairpoint tier:
    - `>= 80 = +20`
    - `>= 65 = +14`
    - `>= 50 = +9`
    - `>= 30 = +5`
    - `>= 10 = +2`
    - `< 0 = -4`
    - `<= -20 = -8`
  - no kaki chars: `+10` (when highlight info exists)
  - length bonus: `+15/+10/+5/-5` by name length bands
- Normalize by `/283` to a 0-100 score.

### Sort order (current)
1. If normalized `meaning` is the same, compare `numerology_rank_score` first
2. `final_rank_score` desc
3. `numerology_rank_score` desc
4. `semantic_score` desc
5. `hybrid_score` desc
6. `name` asc

### Why this tie-break exists
- Users read the visible green circles as a quality promise.
- If two cards show the same meaning, but one card has stronger `pairType/pairpoint`, the stronger numerology should win.
- This avoids cases like:
  - name A looks lighter/ weaker on the circles
  - name B looks darker/ stronger
  - but name A still ranks above name B only because of small semantic noise

### Sanity-check rule for debugging
When users question why one name beats another, verify in this order:
1. Are the meanings effectively the same?
2. Compare `pairType` and `pairpoint` of SAT + SHA
3. Compare `numerology_rank_score`
4. Only then use semantic similarity / distance as the final explanation

## Frontend (Flutter) - Display + Monetization
Files:
- `flutter-naming/lib/src/models/name_model.dart`
- `flutter-naming/lib/src/screens/naming_screen.dart`
- `flutter-naming/lib/src/widgets/name_list_item.dart`

### Ranking behavior
- Client sorts by backend `final_rank_score` when available.
- `calculateScore()` keeps a local fallback for safety.
- Score explanation UI uses backend `rank_reasons` where available.
- The back side of the ranking card is allowed to show lightweight numerology proof for sanity-checking.
- Current compact explanation favors merging `pairType/pairpoint` into the visible `เลขศาสตร์` / `พลังเงา` rows instead of repeating a separate debug box.

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
- exact text equality of `meaning` is a practical heuristic; if wording differs slightly but meaning is still conceptually the same, semantic noise can still affect order.

## Recommended guardrails (next)
1. Add a small API contract test asserting `final_rank_score` and `rank_reasons` always exist.
2. Add an API contract test for the new ranking detail fields:
   - `semantic_rank_score`
   - `numerology_rank_score`
   - `pair_type_bonus`
   - `pair_point_bonus`
3. Add a regression case where 2 names share the same meaning, and the stronger `pairType/pairpoint` must rank first.
4. Add a UI test for premium overlay:
   - non-VIP + double green => overlay shown
   - VIP => overlay hidden
5. Keep ranking formula changes backend-only and versioned in release notes.
