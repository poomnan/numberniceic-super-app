# Chuedee Mobile Development Skill Guide

คู่มือสำหรับการพัฒนา Mobile Application (iOS/Android/Flutter) เชื่อมต่อกับ Chuedee API

## 1. Architecture Overview
Mobile App จะทำหน้าที่เป็น Client ที่เรียกใช้ RESTful API จาก Go Backend โดยตรง
- **Base URL:** `https://ชื่อดี.com` (หรือ `https://xn--b3cu8e7ah6h.com`)
- **Authentication:** Public Access (ไม่ต้องใช้ Token สำหรับเวอร์ชั่นปัจจุบัน)

## 2. Integration Flows

### 2.1 Name Search Flow (ค้นหาชื่อ)
1. User กรอกคำค้นหา (Keyword) และเลือกวันเกิด (Day)
2. App เรียก `POST /api/v1/name-search`
3. Backend คืนรายการชื่อพร้อม `kaki_highlight`
4. **UI:** แสดงรายการชื่อ โดยใช้ `kaki_highlight` เพื่อทำสีตัวอักษรกาลกิณีเป็นสีแดง
   - **Android:** ใช้ `SpannableString`
   - **iOS:** ใช้ `NSAttributedString`
   - **Flutter:** ใช้ `RichText` + `TextSpan`

### 2.2 Root Word Flow (ดูรากศัพท์)
1. User กดปุ่ม "รากศัพท์" ที่รายการชื่อ
2. App เรียก `GET /api/v1/name-root?name=...&meaning=...`
3. **UI:** แสดง Loading Spinner
4. เมื่อได้ข้อมูล (`root_word`): แสดง Modal หรือ Bottom Sheet พร้อมข้อความ

**Best Practice:**
- ควรส่ง `meaning` ไปด้วยเพื่อให้ AI วิเคราะห์ได้แม่นยำขึ้น
- ข้อมูลนี้สร้างจาก AI (OpenAI) อาจใช้เวลา 2-5 วินาที ควรมี Timeout handling ที่เหมาะสม (เช่น 10s)

### 2.3 Number Meaning Flow (ดูคำทำนายเลข)
1. User กดที่ Badge คะแนนตัวเลข (วงกลมสีเขียว/แดง)
2. App เรียก `GET /api/v1/number-meaning?number=...`
3. **UI:** แสดง Modal หรือ Bottom Sheet
4. แสดง `description` (หัวข้อ) และ `detail` (เนื้อหา)

**Unique Data:**
- ข้อมูลคำทำนายมาจากตาราง `numbers` ใน Database
- ฟิลด์ที่ใช้คือ `miracledesc` และ `miracledetail` (Backend จัดการ mapping ให้แล้ว)

## 3. Implementation Details

### 3.1 Error Handling
API จะคืน JSON เสมอ แม้แต่ในกรณี Error 500
```json
{
  "error": "Database error: ..."
}
```
Mobile App ควรตรวจสอบ HTTP Status Code:
- **200 OK:** Parse JSON ตามปกติ
- **4xx/5xx:** อ่าน Body เพื่อหา field `error` หรือแสดงข้อความ Generic Error

### 3.2 Caching Strategy
- **Number Meanings:** ข้อมูลนี้ **Static** มาก (ความหมายเลข 0-99 ไม่ค่อยเปลี่ยน)
  - แนะนำให้ **Cache** ไว้ใน Local Storage / SQLite ของ App
  - ถ้ามี Cache แล้ว ไม่ต้องเรียก API ซ้ำ
- **Name Search:** ไม่ควร Cache นาน (User เปลี่ยน Keyword บ่อย)

### 3.3 Text Rendering (Kaki Highlight)
การแสดงผลภาษาไทยที่มีวรรณยุกต์ (Combining Marks) ให้เปลี่ยนสีเฉพาะวรรณยุกต์
- **Web:** ทำยาก (ต้องใช้ Canvas หรือ hack span)
- **Mobile (Native):** ทำได้ง่ายมาก Native Text Rendering รองรับการเปลี่ยนสี Diacritics แยกจาก Base Character ได้ถูกต้อง 100%

## 4. API Reference
ดูเอกสารฉบับเต็มได้ที่:
`https://ชื่อดี.com/api-mobile-doc`
(หรือเปิดไฟล์ `templates/api_mobile_doc.html` ใน Source Code)

## 5. Deployment / Server Info (For Devs)
- **Backend:** Go (Golang)
- **Database:** PostgreSQL (User Data), MySQL (Name Data)
- **Server:** Ubuntu Linux Systemd Service (`go-naming.service`)
- **Logs:** `journalctl -u go-naming -f`
