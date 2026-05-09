# Thai Name Euphony V2

ไฟล์ชุดนี้ใช้สำหรับเริ่มรอบใหม่ของการประเมินชื่อภาษาไทย โดยแยกให้ชัดว่าอ่านลื่น กลาง หรือสะดุด/ค่อนข้างแข็ง

## Files
- `THAI_NAME_EUPHONY_V2_PROMPT.txt`
- `THAI_NAME_EUPHONY_V2_TEST_SET.json`
- `THAI_NAME_EUPHONY_V2_BENCHMARK_OUTPUTS.json`

## Suggested rollout
1. หยุดงานเก่าที่ใช้ prompt เดิมใน Typoon
2. สร้าง prompt ใหม่จาก `THAI_NAME_EUPHONY_V2_PROMPT.txt`
3. ทดสอบกับ `THAI_NAME_EUPHONY_V2_TEST_SET.json`
4. เทียบผลกับ `THAI_NAME_EUPHONY_V2_BENCHMARK_OUTPUTS.json`
5. ถ้า JSON valid ทุกเคสและแนวโน้มถูกต้อง ค่อยเปิด production

## Fast validation
- output ต้อง parse JSON ได้
- labels ต้องอยู่ในชุดที่กำหนดเท่านั้น
- score ทุกช่องต้องเป็น integer 0-100
- เคสสะดุด/แข็งต้องมี `เสียงค่อนข้างแข็ง` หรือ `จังหวะสะดุดเล็กน้อย` อย่างน้อย 1 ค่า
- เคสลื่นไม่ควรติด 2 label ข้างต้น
