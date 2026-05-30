# Chue-Dee (ชื่อดี) System Specification & Logic
This document serves as the primary technical context for "Chue-Dee" (ชื่อดี.com), a numerology-based naming system.

## 1. Technical Stack
- **Backend:** Pure Go (Standard Best Practices, Clean Architecture)
- **UI:** Pure CSS & Vanilla JavaScript
- **Database:** PostgreSQL (User: tayap, DB: tayap)
- **Deployment:** Manual execution via Terminal (AI generates scripts for deployment).
- **Server:** Ubuntu (IP: 43.228.85.200)

---

## 2. Database Schema Reference

### 2.1 Numerology Tables
- `sat_nums` (เลขศาสตร์): `char_key` (text), `sat_value` (int)
- `sha_nums` (เลขพลังเงา): `char_key` (text), `sha_value` (int)

### 2.2 Interpretation & Rules
- `numbers`: Dictionary for pair analysis.
    - `pairnumber` (char2): Key for joining.
    - `pairtype`: (D10, D8, D5 = Good) | (R10, R7, R5 = Bad)
    - `pairpoint` (int): Ranking score.
- `kakis_day`: Inauspicious characters based on birth day.
    - `day`: Sunday, Monday... Wednesday1 (Day), Wednesday2 (Night).

### 2.3 Master Name Storage
- `names_miracle`: 300,000+ records.
    - Contains pre-calculated numerology slides and `th_name_vector` (vector 768) for semantic search.

---

## 3. Core Business Logic

### Step 1: Decoding & Summation
1. **Decode:** Split Thai string into individual characters (Runes).
2. **Lookup:** Map each character to `sat_value` and `sha_value`.
3. **Calculate:** - $Sum_{sat} = \sum sat\_values$
   - $Sum_{sha} = \sum sha\_values$

### Step 2: Sliding Window Pairing
Convert sums into string pairs:
- If Sum $\geq 100$ (e.g., 641): `["64", "41"]`
- If Sum $10-99$ (e.g., 41): `["41"]`
- If Sum $< 10$ (e.g., 1): `["01"]` (Zero-padded)

### Step 3: Predictive Analysis
- Loop through pairs and JOIN with `public.numbers` table on `pairnumber`.
- Filter by `pairtype` to determine UI color/grading:
  - **Green (Good Tone):** D10, D8, D5
  - **Red (Bad Tone):** R10, R7, R5

### Step 4: Kalakini (Inauspicious) Filtering
- Input: Birth day (Thai/English mapping).
- Process: Compare each character of the name against `kakis_day`.
- Output: Boolean array (e.g., `[false, true, false]`) to highlight restricted characters.

### Step 5: Advanced Search & Vector Matching
- **Combination:** Find names that, when added to a specific surname, result in a "Good Sum" (D10, D8, D5).
- **Ideation:** Use `th_name_vector` for semantic similarity search based on user intent (e.g., "Wealth", "Brave").

### Step 6: Phonetic Matching (Sound-Alike)
- **Problem:** Users often find a name they like, but the numerology is bad.
- **Solution:** Use Phonetic Algorithm (Thai Soundex) to find names that sound similar (or identical) but have different spellings (different numerology).
- **Implementation:** `services/phonetic_service.go` implements a custom Thai Soundex:
  - Consonants grouped by sound (1-9).
  - Vowels, Tones removed.
  - Silent characters (Karan) handling.

### Step 7: Spelling Recommendation
- **Feature:** `/recommend-spelling?name=...`
- **Logic:** Takes a bad name -> Finds names with SAME phonetic code -> Returns valid alternatives from DB.
- **Optimization:** Filters by first character + phonetic code to ensure speed.

---

## 4. Operation Protocols for AI

1. **Go Coding:** Use pure Go without heavy frameworks. Prioritize performance for large dataset (300k+ rows).
2. **PostgreSQL:** Use GIN/GiST indexes for fast text/array searching.
3. **Deployment:** Generate bash scripts for SSH/SCP operations. Use provided credentials:
   - Host: 43.228.85.200
   - User: tayap
   - Port: 22 (Default)

---
*Note: This skill document is exclusive to the "Chue-Dee" project (xn--b3cu8e7ah6h.com).*

---

## 5. Chat-Based Naming Assistant (Web)

### 5.1 Overview
- **URL:** `https://xn--b3cu8e7ah6h.com/` (หน้าแรกของเว็บ)
- **Persona:** "คุณทญา" (Khun Taya) — ที่ปรึกษาตั้งชื่อมงคลตามหลักเลขศาสตร์ชั้นสูง
- **API Endpoint:** `POST /api/v1/naming/chat`
- **Handler:** `handlers/naming_assistant_handler.go`
- **Template:** `templates/chat.html`
- **Usage:** ไม่จำกัดจำนวนข้อความ (tracking ด้วย prefix `naming_` แยกจากระบบทำนายฝัน)

### 5.2 Multi-Turn Context Memory
ระบบจำบริบทข้ามข้อความได้ผ่าน `context` object ที่ส่งไป-กลับระหว่าง frontend ↔ backend ทุก request

#### Context Fields:
| Field | ค่าที่เป็นไปได้ | ใช้ทำอะไร |
|-------|---------------|----------|
| `day` | จันทร์, อังคาร, พุธกลางวัน, พุธกลางคืน, พฤหัสบดี, ศุกร์, เสาร์, อาทิตย์ | กรองอักษรกาลกิณี (`k_friday = false`) |
| `gender` | ช (ชาย), ญ (หญิง), ค (คละ) | กรองเพศ (`gender = 'ญ' OR gender = 'ค'`) |
| `keywords` | เช่น "ร่ำรวย", "ฉลาด", "เป็นที่รัก" | Vector search ความหมาย |

#### Flow:
```
ข้อความ 1: "อยากได้ชื่อลูกสาว แนวร่ำรวย"
→ AI สกัด: {gender:"ญ", keywords:"ร่ำรวย"}
→ ค้นหาชื่อหญิง + ความหมายร่ำรวย (ยังไม่กรองกาลกิณี)

ข้อความ 2: "เกิดวันศุกร์ค่ะ"  
→ AI สกัด: {day:"ศุกร์"} + merge กับ context เดิม
→ ค้นหาชื่อหญิง + ร่ำรวย + กรองกาลกิณีวันศุกร์
```

#### Architecture:
```
Frontend (chat.html)
  ├─ chatContext = { day, gender, keywords }  ← JavaScript state
  ├─ ส่ง context ไปกับทุก POST request
  ├─ รับ context ที่ update แล้วจาก response
  └─ แสดง Context Badges (📅 วันเกิด, 👧 เพศ, 🔍 แนวชื่อ)

Backend (naming_assistant_handler.go)
  ├─ รับ context จาก frontend เป็น base
  ├─ AI (GPT-4o) สกัดข้อมูลใหม่จากข้อความ + ประวัติ
  ├─ Merge: ข้อมูลใหม่ override, ข้อมูลเดิมคงไว้
  ├─ Gender filter: AND (gender = 'ญ' OR gender = 'ค')
  ├─ Kalakini filter: AND k_friday = false
  └─ Return updated context กลับ frontend
```

### 5.3 AI Extraction (Intent Parsing)
ใช้ GPT-4o สกัดข้อมูล 3 อย่างจากบทสนทนาทั้งหมด:
1. **วันเกิด** — จาก "เกิดวันศุกร์" หรือ "วันจันทร์"
2. **เพศ** — จาก "ลูกชาย", "ลูกสาว", "ผู้หญิง" + robust fallback regex
3. **Keywords** — เฉพาะแนวคิด/ความหมาย (กรอง filler เช่น "ตั้งชื่อ", "เกิด" ออก)

ข้อมูลที่เคยสกัดได้จะถูกส่งกลับมาใน prompt เพื่อให้ AI คงค่าเดิมไว้ ไม่ต้องถามซ้ำ

### 5.4 System Prompt Design (คุณทญา)
คุณทญามีความรู้เรื่อง:
- **เลขศาสตร์** — อธิบายคู่เลข D/R ได้
- **พลังเงา** — อธิบาย shadow number ได้
- **กาลกิณี** — อธิบายว่าอักษรต้องห้ามตามวันเกิดคืออะไร และระบบกรองออกให้อัตโนมัติ

กฎสำคัญ:
- ถามวันเกิดได้ โดยอธิบายว่า "ถ้าแจ้งวันเกิด จะกรองอักษรกาลกิณีให้"
- ห้ามทำนาย/วิเคราะห์เรื่องวันเกิดเอง (ไม่ใช่หมอดู)
- ห้ามถามซ้ำสิ่งที่รู้แล้ว
- ห้ามเกี่ยวข้องกับระบบทำนายฝัน (คนละแอป)

### 5.5 Separation from Dream System
| ระบบ | Persona | API | Usage Tracking | Limit |
|------|---------|-----|---------------|-------|
| **ตั้งชื่อ (Web)** | คุณทญา | `/api/v1/naming/chat` | `naming_` prefix | ไม่จำกัด |
| **ทำนายฝัน (Mobile)** | คุณนิน | `/api/v1/ninin/chat` | guest_id ตรง | Guest: 3, Member: 30 |
| **OpenClaw (Demo)** | คุณนิน | `/openclaw` | ไม่มี | ไม่มี |

### 5.6 Frontend (chat.html)
- **Design:** Dark theme, mobile-first, Kanit font
- **Quick Actions:** ปุ่มลัด 4 แบบ (ลูกชาย, ลูกสาว, เปลี่ยนชื่อ, วิเคราะห์ชื่อ)
- **Name Cards:** แสดงชื่อพร้อม badge สี (เขียว=ดี, แดง=ร้าย) สำหรับเลขศาสตร์และพลังเงา
- **Context Badges:** แถบใต้ header แสดงข้อมูลที่ระบบจำได้ (วันเกิด, เพศ, keywords)
- **Typing Indicator:** Animation dots ขณะรอ AI
- **Guest Tracking:** `localStorage` เก็บ guest_id (prefix `web_`)

### 5.7 Routes
| Path | หน้า |
|------|------|
| `/` | Chat ตั้งชื่อมงคล (chat.html) |
| `/home` | หน้าเดิม (index.html) |
| `/chat` | Chat (alias) |

---

## 6. Advanced Algorithms

### 6.1 Reverse Numerology Intersection (Strategy for Vector Search)
*   **The Problem:** Searching for "Meaning" first (Vector Search) is slow because 90% of names have bad numerology. Filtering after search wastes resources.
*   **The Solution:** Calculate the "Perfect Target Sums" first, then filter the Database.

#### Logic Flow:
1.  **Input:** User Surname (e.g., Sat Sum = 20, Sha Sum = 45).
2.  **Pre-Calculation (In Memory):**
    *   Iterate through all possible Total Sums (0-200).
    *   Check if *every* pair in that sum is "Good" (D-Series).
    *   If Sum 24 -> Pairs 24 (Good) -> Keep.
    *   If Sum 115 -> Pairs 11 (Bad), 15 (Good) -> Discard.
    *   *Result:* A list of "Perfect Total Sums" (e.g., [24, 36, 41...]).
3.  **Reverse Target:**
    *   `Required Name Sat` = `Perfect Total` - `Surname Sat`.
    *   `Required Name Sha` = `Perfect Total` - `Surname Sha`.
4.  **Database Query (High Performance):**
    *   `SELECT * FROM names_miracle`
    *   `WHERE sat_sum IN (...) AND sha_sum IN (...)`
    *   `ORDER BY meaning_vector <=> $Vector LIMIT 50`
5.  **Output:** All returned names are guaranteed to be numerologically perfect AND semantically relevant.

#### Database Requirements:
*   Table `names_miracle` MUST have columns `sat_sum` (int) and `sha_sum` (int) with Indexes.
*   (Migration Script: `debug_tools/migrate_sums.go`)

### 6.2 Pre-Computed Kalakini (Inauspicious) Filtering
*   **The Problem:** Filtering 10,000 names for forbidden characters (Kalakini) in real-time is CPU-intensive and causes "Empty Result" issues if many top-ranked names are filtered out.
*   **The Solution:** Flag every name during insertion/migration for each day of the week.

#### Logic Flow:
1.  **Database Columns:** `k_sunday`, `k_monday`, ..., `k_saturday` (Boolean, Default False).
2.  **Pre-Computation:**
    *   Iterate through each name.
    *   Check against `kakis_day` rules for all 8 days.
    *   Set `k_day = true` if the name contains ANY restricted character for that day.
3.  **Zero-Cost Filtering:**
    *   Query: `AND k_sunday = FALSE` (if user born on Sunday).
    *   This uses a Partial Index or Boolean Index, which is extremely fast.
4.  **Guaranteed Result Count:**
    *   Since filtering happens in the DB, `LIMIT 50` guarantees 50 valid names (unless the total pool is < 50).
    *   Eliminates the need to over-fetch (e.g., fetching 10,000 to get 10 valid ones).

#### Database Requirements:
*   Table `names_miracle` MUST have boolean columns for all days with Indexes.
*   (Migration Script: `debug_tools/migrate_kakis.go`)

### 6.3 AI-Narrator & Celebrity Matching (The "Khun Ninin" Persona)
*   **The Problem:** Raw data from database looks robotic and lacks emotional connection.
*   **The Solution:** Use LLM (Claude/GPT) as a "Reviewer" on top of the calculated data.

#### Logic Flow:
1.  **Engine (Golang):** Use `RecommendNamesFast` to get 5 guaranteed lucky names (100% precision).
    *   *Key Addition:* Fetch `miracledesc` (Pair Description) from `numbers` table alongside the pair codes.
2.  **Context Injection (The Secret Sauce):**
    *   Send 5 names + **Exclusive Pair Meanings** to LLM.
    *   *Prompt:* "Roleplay as 'Khun Ninin'. Use these *specific meanings* from our scripture: [24: เมตตามหานิยม...]."
3.  **Proactive Matching:**
    *   **Instruction:** *"If any name sounds like a famous/successful person (Phonetic or Exact Match), mention it positively to inspire the user."*
    *   *Example:* "Thanin -> sounds like the CP Tycoon (Success in Business)."
4.  **Final Output:**
    *   The user receives a curated message that combines:
        *   **Precision:** Guaranteed numerology.
        *   **Exclusive Content:** Meanings from our own scripture.
        *   **Inspiration:** Celebrity matching.

### 6.4 Toggle-Based Search (Single Query Strategy)
*   **Concept:** Mobile/Web UI มี 3 toggle เสริม สำหรับกรองชื่อตามเงื่อนไข โดยค้นหาตามความหมาย/ความรู้สึกเป็นหลักเสมอ
*   **Toggles (Optional):**
    *   🔘 เลขศาสตร์ — `AND sat_sum IN (good_sums)`
    *   🔘 พลังเงา — `AND sha_sum IN (good_sums)`
    *   🔘 กาลกิณี — `AND k_{day} = false`

#### Why Single Query Works:
*   `sat_sum`, `sha_sum` = Pre-computed integer → ใช้ btree index กรองได้ทันที (ไม่ต้อง decode ชื่อซ้ำ)
*   `k_{day}` = Pre-computed boolean → ใช้ partial index กรองได้ทันที
*   `meaning_vector` = Pre-computed 768-dim vector → ใช้ HNSW index เรียงลำดับความหมาย
*   **ทุกเงื่อนไขเป็น WHERE clause** → DB วางแผน query plan เอง ใช้ BitmapAnd รวม index

#### Dynamic Query Building (Go):
```go
where := "WHERE char_length(meaning) >= 25"
if toggleSat  { where += " AND sat_sum IN (" + goodSumsSQL + ")" }
if toggleSha  { where += " AND sha_sum IN (" + goodSumsSQL + ")" }
if toggleKaki { where += " AND k_" + dayColumn + " = false" }
query := "SELECT ... FROM names_miracle " + where +
    " ORDER BY meaning_vector <=> $1 LIMIT 5"
```

#### Example Scenarios:
| Toggle เปิด | Query ที่สร้าง |
|-------------|---------------|
| ไม่เปิดเลย | `ORDER BY meaning_vector <=> $v LIMIT 5` (ค้นตามความหมายอย่างเดียว) |
| เลขศาสตร์ | `WHERE sat_sum IN (...) ORDER BY meaning_vector <=> $v` |
| ทั้ง 3 ตัว | `WHERE sat_sum IN (...) AND sha_sum IN (...) AND k_friday = false ORDER BY meaning_vector <=> $v` |

#### Performance:
*   ทุก toggle ใช้ index ที่สร้างไว้ → Execution Time ~4ms สำหรับ 300K+ rows
*   DB กรองก่อน → Vector search เฉพาะชื่อที่ผ่านเงื่อนไข → LIMIT 20
*   ไม่ต้องแยก query หลายตัว ไม่ต้องให้ code กรองทีหลัง

#### Fallback Strategy:
หากการค้นหาแบบเข้มงวด (Strict) ไม่พบผลลัพธ์ ระบบจะคลายฟิลเตอร์โดยอัตโนมัติ:
1. **Strict:** กรองตามทุก Toggle ที่เปิด (Sat AND Sha AND Kaki)
2. **Relaxed:** เปลี่ยนจาก Sat AND Sha เป็น Sat OR Sha (กรณีเปิดทั้งคู่)
3. **Desperate:** หากยังไม่พบ จะพยายามหาชื่อที่ "ใกล้เคียงความหมายที่สุด" โดยคงเงื่อนไขกาลกิณีไว้แต่ลดหย่อนฟิลเตอร์เลขศาสตร์อื่นลง

---

## 6.6 Matching Mode (รวมชื่อ (Matching))

### Concept
ช่วยให้ผู้ใช้หาชื่อที่มีความหมายหรือ "สไตล์" ใกล้เคียงกับชื่อที่ระบุ (เช่น ชื่อพี่น้อง หรือชื่อเดิม) และคำนวณเลขศาสตร์รวมกัน

### Logic
1. **Search Context Construction:**
   - หากเปิด `similar_mode`: `searchContext = Keyword + " " + Lastname` (ใช้ชื่อที่ระบุมาช่วยดึงแนวทางความหมายจาก Vector)
   - หากปิด: ใช้ `Keyword` ปกติ
2. **Dynamic UI Rendering:**
   - **Matching Mode (Checked):** หากระบุชื่อในช่อง "Matching" ระบบจะคำนวณผลรวมใหม่ (Name + Matching Name) และแสดงคะแนนรวม (Sat/Sha) พร้อมคำแปลตามตำราที่ด้านล่างของบัตรชื่อ (วงกลมสีแดง)
   - **No Matching (Unchecked):** ระบบจะ **ซ่อน** ส่วน "ผลรวม Matching" ออกจากหน้าจอ เพื่อให้ผู้ใช้โฟกัสที่ชื่อเพียงอย่างเดียว แต่ยังคงแสดงเลขศาสตร์เฉพาะตัวชื่อ (วงกลมสีเขียวด้านบน) ไว้
3. **Dynamic Labeling:**
   - หัวข้อส่วน Matching จะเปลี่ยนไปตามชื่อที่ระบุ เช่น "ผลรวม Matching กับ [ชื่อที่พิมพ์]"

---

## 6.5 Kalakini Red Highlighting on Frontend (Canvas Rendering)

### The Problem
ผลการค้นหาชื่อต้องแสดง **สระ/อักษรกาลกิณี** เป็นสีแดง (`#ef4444`) บนหน้าเว็บ (`templates/index.html`) เพื่อให้ผู้ใช้เห็นว่าชื่อมีตัวอักษรต้องห้ามตรงไหน

#### อักษรกาลกิณีวันจันทร์ (ตาม `kakis_day` table):
สระทั้งหมด + ตัวการันต์ (์) + วรรณยุกต์บางตัว:
```
ะ ั า ำ ิ ี ึ ื ุ ู เ แ โ ใ ไ ็ ่ ้ ๊ ์ อ
```

#### ปัญหาหลัก: Thai Combining Marks
ตัวอักษรไทยแบ่งเป็น 2 ประเภท:
1. **Standalone vowels** (า ะ ำ เ แ โ ใ ไ) — อยู่คนเดียวได้ เปลี่ยนสีง่าย
2. **Combining marks** (ิ ี ึ ื ุ ู ั ็ ่ ้ ๊ ์) — ต้องเกาะพยัญชนะ เปลี่ยนสีแยกจากพยัญชนะไม่ได้

เมื่อ combining mark อยู่คนละ `<span>` กับพยัญชนะ:
```html
<!-- ❌ ไม่ทำงาน: browser ประกอบ glyph แล้วใช้สีของพยัญชนะ -->
<span style="color:white">ก</span><span style="color:red">ิ</span>
```
Browser จะ compose ก+ิ เป็น glyph เดียวแล้วใช้สีของ base character (ขาว) ทำให้ combining mark ไม่เปลี่ยนเป็นสีแดง

### Approaches ที่ทดลองแล้วไม่สำเร็จ

| # | วิธี | ปัญหา |
|---|------|-------|
| 1 | `<span style="color:red">` ครอบแค่สระ | Combining marks ไม่ติดสี — browser compose glyph แล้วใช้สี base |
| 2 | CSS Grid Overlay (2 layers) | ปัญหาเดียวกัน — overlay layer ยังใช้ `<span>` แยก |
| 3 | Grapheme Cluster ทั้ง cluster แดง | ใช้ได้ แต่พยัญชนะก็กลายเป็นแดงด้วย (ไม่ต้องการ) |
| 4 | CSS Custom Highlight API (`::highlight()`) | ไม่ทำงานบน browser ของผู้ใช้ |
| 5 | Per-char `<span>` ใน cluster | Combining marks ไม่ติดสี (เหมือน #1) |

### Solution: HTML5 Canvas Single-Pass Rendering ✅

ใช้ `<canvas>` วาดชื่อทีละตัวอักษรด้วยสีที่ถูกต้อง — Canvas วาด glyph แยกกันจริง ไม่มี glyph composition ข้าม fillText calls

#### Algorithm:
```
1. วัดความกว้างชื่อทั้งหมดด้วย measureText()
2. สร้าง <canvas> ขนาดพอดี (คูณ devicePixelRatio เพื่อความคม)
3. วน loop ทีละตัวอักษร (for...of):
   - ถ้าเป็นอักษรกาลกิณี → fillStyle = '#ef4444' (แดง)
   - ถ้าไม่ใช่ → fillStyle = '#f8fafc' (ขาว)
   - fillText(char, x, y)
   - x += measureText(char).width
4. Combining marks มี width = 0 → วาดที่ตำแหน่ง x เดิม (ทับบนพยัญชนะ)
   แต่ด้วยสีที่กำหนดเอง → ติดสีแดงได้
```

#### ทำไม Canvas ถึงทำงาน:
- Canvas `fillText()` วาดแต่ละ call แยกกัน ไม่มี glyph composition ข้าม calls
- Combining mark ที่วาดแยกจะใช้ glyph metrics ของ font (ตำแหน่ง offset) วางตัวเองถูกที่
- ไม่มีปัญหา CSS color inheritance/composition

#### สิ่งสำคัญ — Single-Pass (ไม่ใช่ Overdraw):
```javascript
// ❌ 2-pass: วาดขาวก่อน แล้ววาดแดงทับ → combining mark ติดขาวปน
ctx.fillStyle = '#f8fafc';
ctx.fillText(name, 0, y);     // Pass 1: ทั้งชื่อขาว
ctx.fillStyle = '#ef4444';
ctx.fillText('์', x, y);      // Pass 2: ทับแดง → ยังเห็นขาวรอบขอบ

// ✅ Single-pass: วาดทีละตัวด้วยสีที่ถูกต้องตั้งแต่แรก
for (const ch of name) {
    ctx.fillStyle = regex.test(ch) ? '#ef4444' : '#f8fafc';
    ctx.fillText(ch, x, y);   // วาดครั้งเดียว สีถูกต้อง ไม่มีขาวปน
    x += measureText(ch).width;
}
```

#### Monday Kaki Regex:
```javascript
// ครอบคลุมสระ + ตัวการันต์ตามที่ kakis_day table กำหนด
/[\u0E30-\u0E39\u0E40-\u0E45\u0E47\u0E4C\u0E4D\u0E24\u0E26]/

// Breakdown:
// \u0E30-\u0E39 = ะ ั า ำ ิ ี ึ ื ุ ู
// \u0E40-\u0E45 = เ แ โ ใ ไ ๅ
// \u0E47       = ็ (mai taikhu)
// \u0E4C       = ์ (thanthakat/karan) ← ไม่ใช่สระแต่เป็นกาลกิณีตาม DB
// \u0E4D       = ◌ํ (nikhahit)
// \u0E24       = ฤ
// \u0E26       = ฦ
```

#### วันอื่น (ไม่ใช่จันทร์):
กาลกิณีเป็น **พยัญชนะ** ไม่ใช่ combining marks → ใช้ `<span style="color:red">` ธรรมดาได้ไม่มีปัญหา:
```javascript
const chars = kakiMap[day].replace(/[^ก-ฮ]/g, '');
kakiRegex = new RegExp(`[${chars}]`);
```

### Implementation Files
| File | หน้าที่ |
|------|---------|
| `templates/index.html` | `renderNameCanvas()` function — Canvas rendering + kaki regex |
| `handlers/demo_search_handler.go` | `/api/demo/search` — ส่ง `filter_kaki` ไป backend |
| `services/numerology_service.go` | `DecodeName()` — Backend kaki check จาก DB (case-insensitive query) |

### Bug Fixes ที่เกี่ยวข้อง
1. **`filter_kaki` ไม่ถูกส่งไป backend** — เพิ่ม `filter_kaki: toggleKaki` ใน JSON body
2. **Day case mismatch** — DB เก็บ `monday` (lowercase) แต่ Go ส่ง `Monday` → เพิ่ม `LOWER()` ใน SQL query
3. **Cache-Control** — เพิ่ม `Cache-Control: no-store, no-cache, must-revalidate` header ใน Go handler เพื่อป้องกัน browser cache HTML เก่า

### §6.5.1 Implementation จริงใน Lab Results (`/api/v1/name-keywords`)

#### Backend: `HybridItem.KakiChars` field

เมื่อ user เลือกวันเกิด (แม้ไม่ได้ tick filter_kaki) backend จะ mark ตัวกาลกิณีในชื่อแต่ละรายการ:

```go
// handlers/name_keywords_handler.go
type HybridItem struct {
    Name      string   `json:"name"`
    // ... other fields
    KakiChars []string `json:"kaki_chars"` // chars in name that are kaki for selected day
}

// After DB scan, if day != "":
kakiSet := loadKakiSet(day)  // query kakis_day table → map[string]bool
for i := range similar {
    for _, r := range []rune(similar[i].Name) {
        ch := string(r)
        if kakiSet[ch] { similar[i].KakiChars = append(..., ch) }
    }
}
```

`loadKakiSet(day)` query: `SELECT kakis FROM kakis_day WHERE LOWER(day) = LOWER($1) OR day_th = $2`

#### Frontend: Canvas rendering ใน lab-similar-grid

Canvas ต้องเข้าถึง DOM element จริง (ไม่ใช่ HTML string) ใช้ placeholder `<div>` แล้ว call หลัง `appendChild`:

```javascript
// 1. placeholder ใน innerHTML ของ card
`<div class="name-canvas-wrap" 
      data-name="${item.name}" 
      data-kaki='${JSON.stringify(item.kaki_chars||[])}'>
</div>`

// 2. หลัง grid.appendChild(card)
const wrap = card.querySelector('.name-canvas-wrap');
if (wrap) {
    renderNameCanvas(wrap, wrap.dataset.name, JSON.parse(wrap.dataset.kaki || '[]'));
}

// 3. renderNameCanvas: Single-Pass draw
const renderNameCanvas = (container, name, kakiChars) => {
    const kakiSet = new Set(kakiChars || []);
    const ctx = canvas.getContext('2d');
    ctx.font = '700 18px Prompt, sans-serif';
    let x = 0;
    for (const ch of name) {
        ctx.fillStyle = kakiSet.has(ch) ? '#ef4444' : '#f8fafc';
        ctx.fillText(ch, x, y);
        x += ctx.measureText(ch).width; // combining marks (~0px) → overdraw on consonant
    }
};
```

#### พฤติกรรมตามเงื่อนไข

| เงื่อนไข | พฤติกรรม |
|---|---|
| ไม่เลือกวันเกิด | ชื่อแสดงปกติ ไม่มี highlight |
| เลือกวันเกิด + ☐ ไม่ tick "ไร้กาลกิณี" | ชื่อทั้งหมดแสดง + **ตัวกาลกิณีแดง** (canvas) |
| เลือกวันเกิด + ☑ tick "ไร้กาลกิณี" | กรองชื่อที่มีกาลกิณีออกจาก DB ก่อน → ไม่มี highlight |

---


## 9. Hybrid Search & Scoring System (คำอธิบาย % ในผลลัพธ์)

### 9.1 หลักการคำนวณ % ทั้งหมด

ระบบใช้ **Hybrid Search** ผสมผสาน 2 เทคนิคเพื่อหาชื่อที่ตรงกับคำค้นหามากที่สุด:

#### 1. Trigram Similarity (Roots) 🔤
- **เทคโนโลยี:** PostgreSQL `pg_trgm` extension
- **วิธีคิด:** แบ่งข้อความเป็นชุด 3 ตัวอักษร แล้วเปรียบเทียบความคล้ายกัน
- **SQL Function:** `word_similarity(ai_roots, name)`
- **ช่วงคะแนน:** 0-1 (แปลงเป็น % ด้วย `* 100`)
- **ตัวอย่าง:** "ณเดชน์" vs "เดชน้อย" → 71% (เพราะมี "เดช" ร่วมกัน)

#### 2. Vector Similarity (Semantic) 🧠
- **เทคโนโลยี:** PostgreSQL `pgvector` extension + OpenAI embedding
- **วิธีคิด:** แปลงความหมายเป็น vector 1536 มิติ แล้วคำนวณ cosine distance
- **SQL Function:** `(1 - (meaning_vector <=> target_vector))`
- **ช่วงคะแนน:** 0-1 (แปลงเป็น % ด้วย `* 100`)
- **ตัวอย่าง:** "ผู้มีอำนาจ" vs "อำนาจเล็กน้อย" → 28% (ความหมายต่างกัน)

#### 3. Final Score (⚡) - คะแนนรวม
- **สูตร:** `Final = (Roots × 0.4) + (Semantic × 0.6)`
- **ถ่วงน้ำหนัก:** ให้ความหมายสำคัญกว่ารากศัพท์ (60% vs 40%)
- **ตัวอย่าง:** `(71% × 0.4) + (28% × 0.6) = 45%`

### 9.2 Workflow การค้นหาแบบละเอียด

#### Step 1: AI ถอดรากศัพท์ (Etymology)
```
Input: "ณเดชน์"
AI Output JSON:
{
  "roots": "ณ เดช เดชน์",
  "meaning_summary": "ผู้มีอำนาจแห่งความรู้",
  "keywords": ["อำนาจ", "ความรู้", "ผู้นำ"]
}
```

#### Step 2: Hybrid Search Query
```sql
WITH ai_output AS (
    SELECT 'ณ เดช' AS ai_roots,
           'ผู้มีอำนาจแห่งความรู้' AS ai_meaning
),
target_vector AS (
    SELECT '[0.11, -0.22, 0.33, ...]'::vector AS vec 
)
SELECT 
    name,
    word_similarity(ai_roots, name) AS root_score,      -- Trigram
    (1 - (meaning_vector <=> vec)) AS semantic_score,   -- Vector
    ((word_similarity(ai_roots, name) * 0.4) + 
     ((1 - (meaning_vector <=> vec)) * 0.6)) AS final_rank
FROM names_miracle 
WHERE ai_roots %> name  -- pg_trgm fast filter
   OR meaning_vector <=> vec < 0.4  -- Vector threshold
ORDER BY final_rank DESC
LIMIT 15;
```

#### Step 3: Frontend แสดงผล
```javascript
// แปลงค่าเป็น % อย่างปลอดภัย
const safePercent = (val) => 
    (typeof val === 'number' && !isNaN(val) && isFinite(val)) 
    ? (val * 100).toFixed(0) : '0';

// แสดงผลใน UI
<span>🔤 Roots ${safePercent(item.root_score)}%</span>
<span>🧠 Semantic ${safePercent(item.semantic_score)}%</span>
<span>⚡ Final ${safePercent(item.final_rank)}%</span>
```

### 9.3 ความหมายของ % ในแต่ละระดับ

| % Range | ความหมาย | ตัวอย่าง |
|---------|-----------|----------|
| **80-100%** | ตรงกันมาก | "ณเดชน์" → "ณเดชา" (Roots 95%, Semantic 85%) |
| **60-79%** | ค่อนข้างตรง | "ณเดชน์" → "เดชน้อย" (Roots 71%, Semantic 28%) |
| **40-59%** | ปานกลาง | "ณเดชน์" → "เดชาพล" (Roots 55%, Semantic 45%) |
| **20-39%** | ตรงกันน้อย | "ณเดชน์" → "ธีรเดช" (Roots 35%, Semantic 25%) |
| **0-19%** | แทบไม่ตรงกัน | "ณเดชน์" → "สมชาย" (Roots 10%, Semantic 5%) |

### 9.4 Edge Cases & Fallbacks

#### 1. ถ้าค่าเป็น undefined/null
```javascript
// ปัญหา: item.distance = undefined
// ผลลัพธ์: NaN% (ก่อนแก้ไข)

// แก้ไข: เช็คค่าก่อนคำนวณ
if (typeof displayDist === 'number' && !isNaN(displayDist) && isFinite(displayDist)) {
    simPercent = Math.max(0, Math.min(100, (1 - displayDist) * 100));
} else {
    simPercent = 0; // แสดง 0% แทน NaN%
}
```

#### 2. ถ้าไม่มี Vector Data
```sql
-- Fallback ใช้ค่า default distance = 0.5 (ความคล้ายปานกลาง)
COALESCE((meaning_vector <=> $1), 0.5) as distance
```

#### 3. ถ้า Trigram ไม่ตรงเลย
```sql
-- ใช้ similarity > 0.1 เป็น filter ขั้นต่ำ
WHERE word_similarity(ai_roots, name) > 0.1
```

### 9.5 Performance Optimization

#### Index Strategy
```sql
-- Trigram index (สำหรับ word_similarity)
CREATE INDEX idx_names_trgm ON names_miracle USING gin (name gin_trgm_ops);

-- Vector index (สำหรับ <=> operator)
CREATE INDEX idx_names_vector ON names_miracle USING hnsw (meaning_vector vector_cosine_ops);
```

#### Query Plan
```
1. Bitmap Heap Scan (pg_trgm index) → กรองชื่อที่คล้ายกัน
2. Index Scan (vector index) → เรียงลำดับความหมาย
3. Limit 15 → ตัดทิ้งที่เกิน
Execution Time: ~4ms สำหรับ 300K+ rows
```

### 9.6 ตัวอย่างจริงจากผลลัพธ์

```
1 เดชน้อย ⚡ 45%
├── 🔤 Roots 71% (Trigram: "ณ เดช" มี "เดช" ร่วมกัน)
├── 🧠 Semantic 28% (Vector: "อำนาจ" vs "อำนาจเล็กน้อย")
└── ⚡ Final 45% = (71% × 0.4) + (28% × 0.6)

เลขศาสตร์ · พลังเงา
26 (เลขศาสตร์) | 52 (พลังเงา)
ความหมาย: อำนาจ, พลัง, ความร้อน, แสงสว่าง
```

### 9.7 สรุป
- **% สูง** = ชื่อตรงกับคำค้นหาทั้งรากศัพท์และความหมาย
- **% ต่ำ** = ชื่อตรงกับคำค้นหาเพียงเล็กน้อย
- **ระบบจะเรียงลำดับ** ตาม Final Score จากมากไปน้อย
- **Frontend ปลอดภัย** จากการแสดง NaN% ด้วย `safePercent()` function

---

## 8. Deployment Procedures
We use a streamlined deployment script `deploy.sh` for production updates.

### Workflow:
1.  **Sync Code:** `rsync` local changes to production server.
2.  **Remote Dependencies:** Run `go mod tidy` on server.
3.  **Build Binary:** Compile `main.go` on server.
4.  **Restart Service:** Restart `systemd` service `go-naming`.

### Command:
Run the following from your local `go-naming` directory:
```bash
./deploy.sh
```

### Server Configuration Requirement:
Ensure the production server has the environment variables set (e.g., in `/etc/systemd/system/go-naming.service` or `.env` file):
*   `DATABASE_URL` (PostgreSQL Connection String)
*   `OPENAI_API_KEY` (For AI Fallback / Review)
*   `CLAUDE_API_KEY` (Where applicable)

---

## 10. Troubleshooting API & UI Freezes (Post-Mortem)

### 10.1 Database Query Hanging (33+ Seconds)
- **Symptom:** The `/api/v1/name-keywords` API takes ~30 seconds to return, causing 524 Cloudflare Timeouts and UI freezes.
- **Root Cause:** A logic error where the `meaning_vector` in the DB was cross-checked (cosine distance) directly against the embedding of a single word (the 'name') instead of the semantic 'meaning summary'. Because the cosine similarity mapping completely separated, the `meaning_vector <=> vec < 0.4` condition returned FALSE for all 300,000 strings. This completely bypassed the HNSW vector index selectivity, forcing PostgreSQL into a brutal Sequential Scan across the entire table for the `OR ai_roots %> thname` fallback option.
- **Solution:** 
  1. ALWAYS wait for the `meaning_summary` generated by the LLM and fetch its embedding, rather than attempting to bypass/parallelize it using merely the physical name. The vectors must align.
  2. The SQL `OR` condition between a Trigram Index (`%>`) and a Vector Index (`<=>`) can fatally compromise PostgreSQL's execution planners if thresholds fail. Re-write such Hybrid Search architectures using a `UNION` instead of `OR`:
     ```sql
     WITH top_semantic AS (SELECT ... ORDER BY <=> LIMIT 100),
          top_trigram AS (SELECT ... ORDER BY <-> LIMIT 100)
     SELECT * FROM top_semantic UNION SELECT * FROM top_trigram ORDER BY final_rank DESC LIMIT 15;
     ```
  3. This completely bounds query paths to O(1) latency (typically 10-20ms).

### 10.2 JavaScript Runtime Crash & Infinite Loading Bug
- **Symptom:** After an API triggers, the UI remains permanently stuck on "กำลังวิเคราะห์รากศัพท์... กรุณารอสักครู่", even if the backend replies instantly.
- **Root Cause:** In vanilla JS, an un-handled `TypeError` completely halts function execution, stranding UI progress bars. E.g., `document.getElementById('invalid-id').disabled = true;` inside synchronous or un-caught `await` code blocks.
- **Solution:** Remove references to invalid legacy DOM IDs, strictly encapsulate UI alterations inside `try/catch/finally` blocks, and ensure `finally` clears loading states.

### 10.3 Autocomplete Race Conditions
- **Symptom:** When a user types fast ("ณเดช" then "น์"), the dropdown suggests items regarding "ณเดช" despite the input showing "ณเดชน์".
- **Root Cause:** Backend latency fluctuations. The older `fetch` query ("ณเดช") resolved later than the newest `fetch` query, overwriting the DOM.
- **Solution:** Implement a JS `currentRequestID` ticker logic:
  ```js
  const reqID = ++currentRequestID;
  const data = await fetch(url);
  if (reqID !== currentRequestID) return; // Drop stale responses!
  ```