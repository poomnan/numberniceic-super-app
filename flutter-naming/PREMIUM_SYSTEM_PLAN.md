# 🔒 Premium System - Implementation Plan
# ระบบ Premium สำหรับแอปตั้งชื่อมงคล (Flutter)

## สถาปัตยกรรมภาพรวม

```
┌─────────────────────────────────────────────────────────────┐
│                     Flutter App                             │
│                                                             │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────────┐ │
│  │ NamingScreen  │   │ PremiumMgr   │   │ PurchaseService  │ │
│  │              │──▶│              │──▶│ (iOS StoreKit)   │ │
│  │ Toggle logic │   │ Trial count  │   │                  │ │
│  │ Paywall show │   │ Premium flag │   │ Buy / Restore    │ │
│  └──────────────┘   └──────┬───────┘   └──────────────────┘ │
│                            │                                │
│                     ┌──────┴───────┐                        │
│                     │SharedPrefs   │                        │
│                     │(Local Cache) │                        │
│                     └──────────────┘                        │
│                            │                                │
└────────────────────────────┼────────────────────────────────┘
                             │ HTTP API
                     ┌───────┴────────┐
                     │   Go Backend   │
                     │                │
                     │ /api/v1/       │
                     │   premium/     │
                     │   status       │
                     │   verify       │
                     └───────┬────────┘
                             │
                     ┌───────┴────────┐
                     │  PostgreSQL    │
                     │                │
                     │ user_premium   │
                     │ trial_usage    │
                     └────────────────┘
```

---

## 1. Business Logic

### Free Tier (ฟรี)
- ค้นหาชื่อ: **ไม่จำกัด** (toggle ปิดทั้งสองตัว)
- Toggle "เลขศาสตร์ดี" + "พลังเงาดี": **ทดลอง 10 ครั้ง** (นับจากจำนวนครั้งที่กดค้นหาขณะ toggle เปิด)

### Premium Tier (ซื้อขาด ครั้งเดียว)
- Toggle "เลขศาสตร์ดี" + "พลังเงาดี": **ไม่จำกัด**
- ทุกฟีเจอร์: **ใช้ได้ตลอดไป**

### การนับ Trial
- นับเมื่อ: กดปุ่ม "วิเคราะห์ชื่อตามตำรา" + toggle เปิดอย่างน้อย 1 ตัว (`_filterSat == true || _filterSha == true`)
- ไม่นับเมื่อ: toggle ปิดทั้งสองตัว (ค้นหาแบบปกติ ฟรีไม่จำกัด)
- เก็บจำนวนครั้ง: **SharedPreferences (local)** + **Server (backup)**

---

## 2. Files ที่ต้องสร้าง/แก้ไข

### ไฟล์ใหม่ (Flutter)
| ไฟล์ | หน้าที่ |
|---|---|
| `lib/src/services/premium_manager.dart` | จัดการสถานะ Premium, นับ trial, เช็คสิทธิ์ |
| `lib/src/widgets/paywall_dialog.dart` | UI Dialog ชำระเงิน |

### ไฟล์ที่ต้องแก้ไข (Flutter)
| ไฟล์ | สิ่งที่แก้ |
|---|---|
| `lib/src/screens/naming_screen.dart` | เพิ่ม premium check ที่ toggle + ปุ่มค้นหา, แสดง trial remaining |

### ไฟล์ใหม่ (Go Backend) — ในอนาคต
| ไฟล์ | หน้าที่ |
|---|---|
| `handlers/premium_handler.go` | API เช็คสถานะ + verify receipt |
| SQL migration | ตาราง `user_premium` |

---

## 3. Implementation Details

### 3.1 PremiumManager (`lib/src/services/premium_manager.dart`)

```dart
class PremiumManager {
  static final PremiumManager _instance = PremiumManager._();
  factory PremiumManager() => _instance;
  PremiumManager._();

  // Constants
  static const int trialLimit = 10;
  static const String _keyTrialCount = 'premium_trial_count';
  static const String _keyIsPremium = 'is_premium';
  static const String _keyPurchaseDate = 'premium_purchase_date';

  // State
  bool _isPremium = false;
  int _trialUsed = 0;

  // Getters
  bool get isPremium => _isPremium;
  int get trialUsed => _trialUsed;
  int get trialRemaining => (trialLimit - _trialUsed).clamp(0, trialLimit);
  bool get hasTrialLeft => _trialUsed < trialLimit;
  bool get canUsePremiumFeature => _isPremium || hasTrialLeft;

  // Initialize — call once at app start
  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    _isPremium = prefs.getBool(_keyIsPremium) ?? false;
    _trialUsed = prefs.getInt(_keyTrialCount) ?? 0;
  }

  // Increment trial count when user searches with premium filters
  Future<void> incrementTrial() async {
    if (_isPremium) return; // Premium users don't count
    _trialUsed++;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt(_keyTrialCount, _trialUsed);
  }

  // Unlock premium (after purchase)
  Future<void> unlockPremium() async {
    _isPremium = true;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_keyIsPremium, true);
    await prefs.setString(_keyPurchaseDate, DateTime.now().toIso8601String());
  }

  // Restore purchase
  Future<void> restorePurchase() async {
    // TODO: Verify with Apple StoreKit + Server
  }
}
```

### 3.2 NamingScreen — Toggle + Search Logic

```dart
// ตอนกด Toggle
_buildCheckbox("เลขศาสตร์ดี", _filterSat, (v) {
  if (v == true && !PremiumManager().canUsePremiumFeature) {
    _showPaywall(context);
    return;
  }
  setState(() => _filterSat = v!);
  _search();
}),

// ตอนกดค้นหา
void _search() async {
  // เช็คว่าใช้ premium filter หรือไม่
  final usesPremium = _filterSat || _filterSha;
  
  if (usesPremium && !PremiumManager().canUsePremiumFeature) {
    _showPaywall(context);
    // ปิด toggle กลับ
    setState(() { _filterSat = false; _filterSha = false; });
    return;
  }

  // นับ trial ถ้าใช้ premium filter
  if (usesPremium && !PremiumManager().isPremium) {
    await PremiumManager().incrementTrial();
    setState(() {}); // Update UI counter
  }

  // ... proceed with search
}
```

### 3.3 Trial Counter UI (ใกล้ toggles)

```dart
// แสดงจำนวน trial คงเหลือ
if (!PremiumManager().isPremium)
  Text(
    "🎁 เหลือสิทธิ์ทดลอง ${PremiumManager().trialRemaining} ครั้ง",
    style: TextStyle(color: Colors.amber, fontSize: 12),
  ),
```

### 3.4 Paywall Dialog

```dart
void _showPaywall(BuildContext context) {
  showDialog(
    context: context,
    builder: (context) => AlertDialog(
      title: "🔒 หมดสิทธิ์ทดลอง",
      content: Column(
        children: [
          "ท่านใช้ฟีเจอร์กรองชื่อครบ 10 ครั้งแล้ว",
          "",
          "ปลดล็อกถาวร เพียง ฿159",
          "✅ กรองเลขศาสตร์ดี ไม่จำกัด",
          "✅ กรองพลังเงาดี ไม่จำกัด",
          "✅ ใช้ได้ตลอดไป",
        ],
      ),
      actions: [
        "ไว้ทีหลัง" → close,
        "🔓 ปลดล็อกเลย" → PurchaseService.buy(),
      ],
    ),
  );
}
```

---

## 4. Database Schema (Go Backend — อนาคต)

```sql
CREATE TABLE user_premium (
    id              SERIAL PRIMARY KEY,
    device_id       VARCHAR(255) NOT NULL UNIQUE,
    user_id         INT,
    is_premium      BOOLEAN DEFAULT FALSE,
    trial_used      INT DEFAULT 0,
    product_id      VARCHAR(100),           -- Apple product ID
    purchase_token  TEXT,                    -- Apple receipt
    purchased_at    TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);
```

### API Endpoints (Go Backend — อนาคต)

```
GET  /api/v1/premium/status?device_id=xxx
     → { "is_premium": false, "trial_used": 7, "trial_limit": 10 }

POST /api/v1/premium/verify
     → Body: { "device_id": "xxx", "receipt_data": "base64..." }
     → Verify with Apple → Update DB → { "is_premium": true }

POST /api/v1/premium/trial-increment
     → Body: { "device_id": "xxx" }
     → Increment trial_used → { "trial_used": 8 }
```

---

## 5. iOS In-App Purchase Setup (อนาคต)

### Apple Developer Account
1. สร้าง App ID ใน Certificates, Identifiers & Profiles
2. เปิดใช้ In-App Purchase capability

### App Store Connect
1. สร้าง In-App Purchase product:
   - Type: **Non-Consumable**
   - Product ID: `com.yourapp.premium_unlock`
   - Price: Tier 3 (฿159) หรือ Tier 4 (฿249)
   - Display Name: "ปลดล็อกพรีเมียม"
   - Description: "ปลดล็อกฟีเจอร์กรองเลขศาสตร์ดีและพลังเงาดี ใช้ได้ตลอดไป"

### Flutter Package
- ใช้ `in_app_purchase` package (official Flutter plugin)
- `flutter pub add in_app_purchase`

### PurchaseService (อนาคต)
```dart
class PurchaseService {
  static const productId = 'com.yourapp.premium_unlock';
  
  Future<void> buy() async {
    // 1. Load products from Apple
    // 2. Show purchase dialog
    // 3. Handle purchase result
    // 4. Verify receipt with server
    // 5. Unlock premium locally
  }
  
  Future<void> restore() async {
    // For users who reinstall app
    // 1. Query Apple for previous purchases
    // 2. Verify receipt with server
    // 3. Unlock premium locally
  }
}
```

---

## 6. Development Phases

### Phase 1: Local Premium Logic (ทำตอนนี้) ✅
- [x] สร้าง `PremiumManager` (SharedPreferences)
- [x] เพิ่ม trial check ที่ toggle
- [x] เพิ่ม trial count ที่ search
- [x] สร้าง Paywall Dialog UI
- [x] แสดง trial remaining counter

### Phase 2: Server Sync (ทำภายหลัง)
- [ ] สร้างตาราง `user_premium` ใน PostgreSQL
- [ ] สร้าง API endpoints ใน Go backend
- [ ] Sync trial count กับ server
- [ ] Sync premium status กับ server

### Phase 3: iOS In-App Purchase (ก่อน submit App Store)
- [ ] สมัคร Apple Developer Account ($99/year)
- [ ] ตั้งค่า IAP product ใน App Store Connect
- [ ] Implement `PurchaseService` ด้วย `in_app_purchase` package
- [ ] Server-side receipt verification
- [ ] Sandbox testing

### Phase 4: Polish
- [ ] "Restore Purchase" button ใน Settings
- [ ] Graceful handling เมื่อไม่มี internet
- [ ] Analytics: track conversion rate

---

## 7. Key Variables & Constants

| Variable | Location | Description |
|---|---|---|
| `_filterSat` | `naming_screen.dart:28` | Toggle state "เลขศาสตร์ดี" |
| `_filterSha` | `naming_screen.dart:29` | Toggle state "พลังเงาดี" |
| `trialLimit` | `premium_manager.dart` | จำนวนครั้งทดลอง (10) |
| `_keyTrialCount` | SharedPreferences | Key เก็บจำนวนครั้งที่ใช้ |
| `_keyIsPremium` | SharedPreferences | Key เก็บสถานะ premium |

---

## 8. Current Codebase Reference

| File | Path | Description |
|---|---|---|
| Main Screen | `lib/src/screens/naming_screen.dart` | หน้าจอหลัก มี toggles + search |
| API Service | `lib/src/services/api_service.dart` | เรียก API backend |
| Colors | `lib/src/utils/colors.dart` | สี theme ของ app |
| Name List Item | `lib/src/widgets/name_list_item.dart` | แสดงผลชื่อแต่ละรายการ |
| Saved Names | `lib/src/screens/saved_names_screen.dart` | หน้าแสดงชื่อที่บันทึก |
| Go Backend | `/Users/tayap/project-naming/go-naming/` | Go API server |
| Backend Models | `go-naming/models/models.go` | Go data models |
| Backend Handlers | `go-naming/handlers/` | Go API handlers |

---

## 9. Testing Checklist

- [ ] Toggle เปิดได้ 10 ครั้งก่อนแสดง paywall
- [ ] Toggle ปิดทั้งสองตัว → ค้นหาได้ไม่จำกัด (ไม่นับ trial)
- [ ] Counter แสดง "เหลือ x ครั้ง" ถูกต้อง
- [ ] Paywall แสดงถูกต้องเมื่อ trial หมด
- [ ] หลังซื้อ → toggle ใช้ได้ไม่จำกัด
- [ ] ลบ app ลงใหม่ → restore purchase ได้ (Phase 3)
- [ ] ไม่มี internet → ใช้ local cache
