# API Contract

This document lists the main API surfaces that appear to be used by the clients in this repository.

## Host Map
- PHP content/member backend: `https://numberniceic.online`
- Go naming backend: `https://xn--b3cu8e7ah6h.com`

## Android → PHP Main APIs

Defined in `number-androidx/app/src/main/java/com/numberniceic/https/ApiService.kt` and routed in `number-php/app/routes.php`.

### Auth and member
- `POST /member/login`
- `POST /member/updateToken`
- `POST /member/register/v2`
- `POST /member/update`
- `GET /member/info/{memberid}`
- `POST /member/vipcode`
- `GET /member/currenttime`

Common request style:
- JSON body via Retrofit `JsonObject`

Common response style:
- PHP-specific wrapper objects like `Serverx`, `ServerMessage`, `ServerVip`
- `member/info` returns a `Serverx`-style payload that includes user data and may include `rengyam_access`

### Notifications v2
- `GET /api/v2/notifications`
- `GET /api/v2/notifications/by-type`
- `POST /api/v2/notifications/mark-read`
- `GET /api/v2/notifications/unread-count`
- `POST /api/v2/notifications/save`

### Fortune/content
- `GET /member/bagcolor/{memberid}/{age1}/{age2}`
- `GET /member/wanpra/{wandate}`
- `GET /member/wanspecial/{wandate}`
- `GET /member/mirado/{activity}/{birthday}/{today}`
- `GET /member/miradoV2/{activity}/{birthday}/{today}/{currentday}`
- `GET /member/lengyam`
- `GET /member/dresscolor/{days}`
- `GET /vipcode/{vipcode}`
- `GET /lucky/number`
- `POST /lucky/number`
- `POST /lucky/number/v2`

### Guest/order/admin-support
- `POST /guest/register`
- `POST /guest/save-address`
- `POST /guest/save-order`
- `GET /admin/guest-addresses/{guestId}`
- `GET /admin/guest-addresses/search/{searchTerm}`
- `GET /admin/guest-addresses/stats`

## Android → Go Main APIs

Also defined in Android `ApiService.kt` using absolute URLs.

### Astrology / rengyam support
- `POST /api/v1/wanpra/calculate`
- `GET /api/kalagni`
- `GET /api/kalagni/age`
- `GET /api/foo-days`
- `GET /api/sitti-chok`
- `GET /api/ubath`
- `GET /api/lokawinat`

### Outfit miracle
- `POST /api/v1/outfit-miracle/color-sets`
- `GET /api/v1/outfit-miracle/day-palettes`
- `POST /api/v1/outfit-miracle/day-palettes`

### Chat and commerce
- `POST /api/v1/ninin/chat`
- `POST /api/v1/ninin/redeem-access`
- `POST /api/v1/naming/chat`
- `GET /api/v1/products`
- `GET /api/v1/product-categories`
- `POST /api/v1/admin/products/add`
- `POST /api/v1/payment/qr`
- `GET /api/v1/payment/status`

## Flutter → Go Main APIs

Defined in `flutter-naming/lib/src/services/api_service.dart`.

### `POST /api/v1/name-search`
Request body fields from code:
```json
{
  "keyword": "...",
  "lastname": "...",
  "day": "...",
  "semantic_meaning": "...",
  "filter_sat": true,
  "filter_sha": true,
  "filter_kaki": false,
  "similar_mode": false,
  "limit": 50
}
```

Response shape from `MobileSearchResponse`:
```json
{
  "success": true,
  "results": ["MobileNameResult"],
  "kaki_info": {
    "day": "...",
    "day_th": "...",
    "description": "...",
    "kaki_chars": ["..."]
  },
  "total": 123
}
```

### Other naming endpoints
- `GET /api/v1/name-suggestions?q=...&meaning=...`
- `GET /api/v1/name-meaning?name=...`
- `GET /decode?name=...&day=...`
- `GET /api/v1/name-root?name=...&meaning=...`
- `GET /api/v1/number-meaning?number=...`

## Important Models

### `MobileNameResult`
Defined on the server in `go-naming/handlers/mobile_search_handler.go` and mirrored in Flutter at `flutter-naming/lib/src/models/name_model.dart`.

Main fields:
- identity: `name`, `meaning`, `gender`
- numerology: `sat_sum`, `sha_sum`, `total_sat`, `total_sha`
- similarity metrics: `distance`, `root_score`, `semantic_score`, `hybrid_score`
- pass/fail flags: `is_sat_good`, `is_sha_good`, `is_total_sat_good`, `is_total_sha_good`
- pair metadata: `sat_pair_type`, `sha_pair_type`, `total_sat_pair_type`, `total_sha_pair_type`
- pair points: `sat_pair_point`, `sha_pair_point`, `total_sat_pair_point`, `total_sha_pair_point`
- ranking: `final_rank_score`, `semantic_rank_score`, `numerology_rank_score`
- explanation: `rank_reasons`
- kaki breakdown: `kaki_highlight`

### `MobileSearchResponse`
Fields:
- `success`
- `error`
- `results`
- `kaki_info`
- `total`

### Notification payloads
Android expects:
- `NotificationListResponse`
- `UnreadCountResponse`
- `MarkReadRequest`
These are PHP-backed APIs under `/api/v2/notifications*`.

### Rengyam / calendar payloads
Android expects PHP models such as:
- `LengYamDao`
- `WanpraDao`
- `WanSpecialCollection`

The PHP backend assembles these from `UserController` plus `ThaiCalendarHelper`.

## Contract Risks
- Many contracts are implicit rather than OpenAPI-defined.
- Some Android Go calls are hardcoded absolute URLs, bypassing the default Retrofit base URL.
- Flutter and Go naming ranking must stay in sync for fields like `final_rank_score`, pair-type bonuses, and `rank_reasons`.
