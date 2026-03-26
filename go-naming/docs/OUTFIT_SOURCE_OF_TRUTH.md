# Outfit Miracle - Source of Truth

การคำนวณศรี/กาลีและการจัดชุดสีมงคลในระบบ API ของโปรเจกต์นี้ต้องอ้างอิงจากแหล่งเดียวกันเสมอ:

- `/Users/tayap/project-naming/outfit-miracle/main.go`
- `/Users/tayap/project-naming/outfit-miracle/SKILL.md`

## ขอบเขตที่ต้องอ้างอิง

- `astro/outfit_miracle.go`
- `astro/outfit_handlers.go`
- `templates/api_mobile_doc.html`
- Mobile client ที่เรียก `/api/v1/outfit-miracle/color-sets`

## Contract ที่ส่งกลับจาก API

endpoint `/api/v1/outfit-miracle/color-sets` ส่ง field อ้างอิงเพื่อให้ client ตรวจสอบได้:

- `reference_project`
- `reference_skill_doc`
- `reference_code_file`

ถ้าปรับ logic ใน source of truth ให้ปรับไฟล์ในรายการขอบเขตนี้พร้อมกันทุกครั้ง
