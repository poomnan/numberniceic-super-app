# Domain Rules

This document summarizes domain rules inferred from code only.

## Naming / Numerology Rules

Source files:
- `go-naming/services/numerology_service.go`
- `go-naming/handlers/mobile_search_handler.go`
- `flutter-naming/lib/src/models/name_model.dart`

## Name decoding
- A name is split into Thai characters.
- Each character is looked up in:
  - `sat_nums` for `sat_value`
  - `sha_nums` for `sha_value`
- Totals are accumulated into `TotalSat` and `TotalSha`.

## Kaki / inauspicious character rule
- If a birth day is provided, the service loads inauspicious characters from `kakis_day`.
- Character matches are flagged as `IsKaki`.
- A result with no kaki hits can receive ranking bonus.

## Pair generation rule
From `CreatePairs(sum int)`:
- sum `< 10` → zero-padded pair, example `1 -> ["01"]`
- sum `10..99` → direct pair, example `41 -> ["41"]`
- sum `>= 100` → sliding two-digit windows, example `641 -> ["64", "41"]`

## Pair meaning rule
From `analyzePairs()`:
- pair data is read from the `numbers` table
- if `pairtype` starts with `D`, the pair is treated as good
- code comments explicitly describe:
  - `D10`, `D8`, `D5` as good tiers
  - `R10`, `R7`, `R5` as bad tiers

## Ranking rule for naming search
`MobileSearchHandler` calculates a normalized `final_rank_score`.

### Similarity base
- Similarity is derived from distance: `100 * (1 - distance)` and clamped to `0..100`.

### Positive ranking components
From server code:
- SAT pass: `+20`
- SHA pass: `+20`
- both SAT and SHA pass: `+50`
- no kaki characters: `+10`
- pair-type bonus:
  - `D10 -> +14`
  - `D8 -> +8`
  - `D5 -> +3`
- pair-point bonus:
  - `>= 80 -> +20`
  - `>= 65 -> +14`
  - `>= 50 -> +9`
  - `>= 30 -> +5`
  - `>= 10 -> +2`
- length bonus:
  - length `<= 4 -> +15`
  - length `5 -> +10`
  - length `6 -> +5`
  - length `>= 9 -> -5`

### Negative ranking components
From pair-point bonus rule:
- pairpoint `<= -20 -> -8`
- pairpoint `< 0 -> -4`

### Score normalization
- Raw score is normalized with denominator `283`.
- Final result is clamped to `0..100`.

## Similar mode rule
`MobileSearchHandler` changes ranking context when `similar_mode` is enabled:
- semantic context may combine keyword and lastname
- total SAT/SHA fields become relevant for matching/ranking display
- Flutter model code also switches between base pair fields and total pair fields when `showMatching` is enabled

## Rank explanation rule
The API returns `rank_reasons` with human-readable explanations built from the actual scoring components, such as:
- semantic similarity
- SAT/SHA pass
- pair-type bonus
- pairpoint bonus
- kaki-free bonus
- length bonus

## Auspicious / Rengyam Rules

Source files:
- `number-php/app/Managers/ThaiCalendarHelper.php`
- `number-androidx/app/src/main/java/com/numberniceic/ui/renkyam/RengYamF.kt`

## Calendar tag composition
PHP `ThaiCalendarHelper::getAuspiciousStatus()`:
- queries a base status bundle
- computes `is_wanpra`
- builds tag bundles from at least two sources:
  - MyHora-style tags
  - Mahamodo-style tags
- merges tags and source-specific details into final display tags

## Safe-day rule (`ปลอด`)
From PHP code:
- if merged tags contain no positive tags and no negative tags, `ปลอด` is appended
- `ปลอด` is described as a general safe/neutral day

From Android code:
- Android also has `isPlainSafeDay(wp)` as a fallback interpretation
- a day is treated as plain-safe when it has no hard negative tags, no positive auspicious tags, and is not wanpra, or already contains the `ปลอด` tag

## Positive tag examples
PHP exposes flags based on merged tags for items such as:
- `วันธงชัย`
- `วันอธิบดี`
- `อำฤตโชค`
- `มหาสิทธิโชค`
- `สิทธิโชค`
- `ราชาโชค`
- `ชัยโชค`
- `วันลอย`
- `วันฟู`
- `ดิถีเรียงหมอน`

## Negative tag examples
Tag meanings and Android-side hard-negative filtering include tags such as:
- `วันอุบาทว์/อุบาสน`
- `วันโลกาวินาศ`
- `วันจม`
- `พิฆาต`
- `ดิถีพิฆาต`
- `กาลกิณี` or legacy `กาลกรรณี`
- `กาลสูร`
- `กาลโชค`
- `กาลทิน`
- `กาลทัณฑ์`
- `มฤตยู`
- `ทินสูญ`
- `ทักทินไฟ`
- `ยมขันธ์`

## Rengyam activity eligibility rule
Android `RengYamF.kt` applies category-specific filtering.
Examples from code:
- marriage/engagement favor `ดิถีเรียงหมอน`, `อำฤตโชค`, `อมุตโชค`, `มหาสิทธิโชค`, `สิทธิโชค`, `ราชาโชค`, `ชัยโชค`, or plain-safe days
- house/build/move favors `วันธงชัย`, `วันอธิบดี`, and major positive tags, while excluding warnings involving house/land/mountain keywords
- ordination allows `วันพระ` in addition to positive tags and plain-safe days
- many categories explicitly exclude warning keywords specific to that activity, such as `รถ`, `บ้าน`, `ป่า`, `น้ำ`, `สตรี`, `บุรุษ`

## Important implementation note
- Rengyam behavior is not purely backend-driven. PHP generates tags, but Android applies additional category filtering and fallback interpretation.
