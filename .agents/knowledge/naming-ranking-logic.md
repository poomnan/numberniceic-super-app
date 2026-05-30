# Naming & Ranking Logic (ตั้งชื่อ) - Developer Knowledge Base

> อัปเดตล่าสุด: พฤษภาคม 2569 / May 2026  
> แหล่งที่มา: AGENTS.md / Flutter Naming Client / Go Service logic

---

## 1. เกณฑ์การจัดลำดับ (Ranking)

ในการจัดลำดับชื่อที่แนะนำ ระบบต้องอาศัยปัจจัยหลายอย่างเพื่อความแม่นยำและไม่ทำให้ผู้ใช้สับสน:

### 1.1 เงื่อนไขก่อนเริ่มจัดอันดับ (Ranking Entry Conditions)
ระบบ Flutter จะยังไม่แสดง ranking list จนกว่าจะครบ 2 เงื่อนไขนี้:

1. **มีแม่แบบชื่อที่จัดอันดับได้ (`_hasRankableNameTemplate`)**
   - มาจากชื่อที่ผู้ใช้พิมพ์, ชื่อที่ระบบ detect ได้, หรือ semantic meaning search ที่ถูกใช้เป็นแม่แบบ
   - ถ้ายังไม่มีแม่แบบชื่อ ระบบควรแสดงไอเดีย/คำแนะนำชื่อแทน ไม่ควรแสดงอันดับ
2. **ผู้ใช้เปิดอย่างน้อยหนึ่งตัวคัด ranking (`_filterSat` หรือ `_filterSha`)**
   - `คัดเลขศาสตร์ดี` (`_filterSat`) ใช้ SAT เป็นเงื่อนไขผ่าน/ไม่ผ่าน
   - `คัดพลังเงาดี` (`_filterSha`) ใช้ SHA เป็นเงื่อนไขผ่าน/ไม่ผ่าน
   - เปิดทั้งสองตัวคัด: ชื่อต้องผ่านทั้ง SAT และ SHA ก่อนเข้า ranking list

### 1.2 ลำดับความสำคัญ (Order of Importance)
1. **Backend ranking score ก่อนเสมอ:** ใช้ `final_rank_score` เมื่อ backend ส่งค่ากลับมา
2. **Local fallback score:** หากไม่มี `final_rank_score` ให้ใช้ `MobileNameResult.calculateScore(showMatching: false)`
3. **เงื่อนไขตัวคัดที่เลือก:** ชื่อที่ไม่ผ่านตัวคัดที่ active ต้องไม่ถูกแสดงใน ranking list
4. **ความสัมพันธ์ของเลขคู่ (Pairing Type):** หากคะแนนอันดับเท่ากัน ให้ดู `pairType` ตามแกนที่เลือก
   - SAT mode ใช้ `satPairType` และ `satPairPoint`
   - SHA mode ใช้ `shaPairType` และ `shaPairPoint`
   - ลำดับ pairType ที่ดี: `D10` > `D8` > `D5`
5. **Pairpoint:** ถ้า pairType เท่ากัน ให้ pairpoint สูงกว่าขึ้นก่อน
6. **ระยะห่างความหมาย (Semantic Distance / Semantic Score):** ใช้เป็นตัวตัดสินท้ายสุดเมื่อคุณภาพชื่อและคู่เลขใกล้กันมาก

### 1.3 กฎการกรองตามปุ่มผู้ใช้ (Filter Rules)
- ถ้าเปิดเฉพาะ `คัดเลขศาสตร์ดี`: แสดงเฉพาะชื่อที่ `isSatGood == true`
- ถ้าเปิดเฉพาะ `คัดพลังเงาดี`: แสดงเฉพาะชื่อที่ `isShaGood == true`
- ถ้าเปิดทั้งสอง: แสดงเฉพาะชื่อที่ `isSatGood == true` และ `isShaGood == true`
- ถ้าไม่เปิดทั้งสอง: ไม่เรียก ranked result เป็น ranking list; ให้คง UX เป็น suggestions/ideas

---

## 2. การแสดงผล (UI Representation)

### 2.0 หน้าอธิบาย Ranking ในแอป
- Footer link `การจัดอันดับชื่อ` ใน `flutter-naming/lib/src/screens/naming_screen.dart` ต้องเปิด `InformationScreen(initialTabIndex: 3)`
- Tab 3 ใน `flutter-naming/lib/src/screens/info_screen.dart` ต้องอธิบายกฎปัจจุบันตามหัวข้อ 1.1-1.3
- ห้ามปล่อยข้อความเก่าที่สื่อว่า ranking เป็นเพียง bonus แบบ SAT/SHA/Double Lucky โดยไม่พูดถึง `final_rank_score`, entry conditions, และ `pairType/pairpoint`

### 2.1 ข้อมูลบนการ์ด (Ranking Card)
- **Compact Proof:** บนฝาหลังของการ์ดลำดับชื่อ (Ranking Card) ควรแสดงข้อมูลพิสูจน์คะแนนแบบกะทัดรัด (Compact)
- ตัวอย่าง: `เลขศาสตร์` / `พลังเงา` แสดงเป็น `24 (D10/80)` แทนการสร้าง Debug Box แยกที่ซ้ำซ้อน

### 2.1.1 การแสดงอักษรกาลกิณีในชื่อภาษาไทย
- Flutter ใช้ `_ThaiHighlightPainter` ใน `flutter-naming/lib/src/widgets/name_list_item.dart` และมี painter สำเนาใน `flutter-naming/lib/src/screens/naming_screen.dart` สำหรับแสดง `kaki_highlight`
- ห้ามใช้ `TextSpan` สลับสีรายอักขระแบบตรง ๆ กับภาษาไทยเมื่อมีสระ/วรรณยุกต์ เพราะ shaping ของสระ เช่น `ำ`, `ู`, `ไ` อาจเลื่อนหรือกินพื้นที่พยัญชนะข้างเคียง
- ห้ามใช้กฎ `cluster.any(isKaki) ? red : default` เพราะถ้าสระหรือวรรณยุกต์ตัวเดียวเป็นกาลกิณี จะทำให้ทั้งพยางค์แดง เช่น `ลำ` ทำให้ `ล` แดงผิดไปด้วย
- วิธีที่ถูกต้อง:
  1. จัดกลุ่ม Thai cluster โดยให้ preposed vowels (`เ แ โ ใ ไ`) และ combining marks (`ั ำ ิ ี ึ ื ุ ู ็ ่ ้ ๊ ๋ ์ ...`) เกาะกับพยัญชนะฐาน
  2. ถ้า cluster มีสระ/วรรณยุกต์กาลกิณี ให้วาด cluster ทั้งก้อนเป็นสีแดงก่อน เพื่อให้สระอยู่ตำแหน่งถูกต้อง
  3. วาดพยัญชนะหรืออักขระใน cluster ที่ไม่ใช่กาลกิณีทับกลับเป็นสีปกติ
  4. ถ้าเป็นกาลกิณีเฉพาะพยัญชนะ ให้วาด cluster สีปกติก่อน แล้ว overlay เฉพาะพยัญชนะนั้นเป็นสีแดง
- ตัวอย่างเคสสำคัญ: คนเกิดวันจันทร์ห้ามสระ ชื่อ `ลำพูน` ควรแสดง `ำ` และ `ู` เป็นสีแดง แต่ `ล`, `พ`, `น` ต้องยังเป็นสีปกติ
- หลังแก้ painter ตัวใดตัวหนึ่ง ให้ sync logic ทั้งใน `name_list_item.dart` และ `naming_screen.dart` เพื่อไม่ให้ผลลัพธ์ต่างกันระหว่าง ranking card กับส่วนแสดงชื่ออื่น

### 2.2 โหมดการค้นหา (Similar Mode)
- เมื่อใช้โหมด **`similarMode` (`รวมให้เป็น "ชื่อดี"`)**:
  - ต้องแสดงทั้งคะแนน **Base-name scores** และคะแนน **Combined Total scores** พร้อมกันบนการ์ด
  - **Filter Logic:** เมื่อผู้ใช้กรองข้อมูล (เช่น `เลขศาสตร์ดี` / `พลังเงาดี`) สิ่งที่แสดงบนการ์ดต้องสอดคล้องกับฟิลเตอร์ที่เลือกโดยตรง เพื่อลดความสับสน (Confusion) ว่า "ฟิลเตอร์แล้วแต่ทำไมยังแดงอยู่"

---

## 3. กฎความสอดคล้อง (Consistency Rules)

1. **ห้ามขัดแย้งกันเอง (No Self-Contradiction):** หาก 2 ชื่อมีพื้นฐานความหมายเดียวกัน (effectively the same meaning) ลำดับการจัดกลุ่มต้องไม่ทำให้ค่า Numerology ที่ผู้เห็นขัดแย้งกับลำดับที่ปรากฏ (เช่น ชื่อที่แย่กว่าไม่ควรอยู่หน้าชื่อที่ดีกว่า)
2. **Backward-Compatibility:** การแก้ไขตรรกะ Ranking ต้องรักษาความเข้ากันได้กับข้อมูลชื่อเดิมที่มีอยู่ในระบบ (Legacy names)

---

## 4. โครงสร้างไฟล์ (Reference Files)

| ระบบ | ไฟล์ / เส้นทาง |
|------|---------------|
| Flutter ranking/search UI | `flutter-naming/lib/src/screens/naming_screen.dart` |
| Flutter ranking explanation | `flutter-naming/lib/src/screens/info_screen.dart` |
| Flutter result model/score fallback | `flutter-naming/lib/src/models/name_model.dart` |
| Backend ranked search | `go-naming/handlers/mobile_search_handler.go` |
| Backend ranking pipeline | `go-naming/services/meaning_search_pipeline.go` |
| Knowledge Area | `flutter-app` skill |
