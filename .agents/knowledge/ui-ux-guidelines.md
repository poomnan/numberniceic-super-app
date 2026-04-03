# UI/UX & Aesthetic Guidelines — Developer Knowledge Base

> อัปเดตล่าสุด: เมษายน 2569 / April 2026

---

## 1. ปรัชญาการออกแบบ (Design Philosophy)

"Rich Aesthetics & Premium Feel" (ความสวยฉ่ำระดับพรีเมียม) คือหัวใจหลักของประสบการณ์ผู้ใช้ (UX) ของแอปในเครือ Numberniceic

### 1.1 หลักการออกแบบพื้นฐาน
1. **ไม่ใช่แค่ MVP (More than MVP):** หลีกเลี่ยงการสร้าง UI เบื้องต้นหรือรูปแบบเรียบง่าย ต้องสร้างความประทับใจตั้งแต่แรกจำ (Stunning First Impression)
2. **สีสันและอารมณ์ (Colors & Mood):**
   - หลีกเลี่ยงสีพื้นฐาน (Pure Red, Pure Blue)
   - ใช้พาเลทสีที่ได้รับการคัดมาแล้ว (Curated Palettes), HSL tailored colors
   - ใช้ **Vibrant Colors**, **Dark Mode**, และ **Glassmorphism**
   - ใช้ **Gradients** ที่นุ่มนวล (Smooth Gradients) แทนสีทึบบางตำแหน่ง
3. **Typography:** ใช้ฟอนต์สมัยใหม่จาก Google Fonts (เช่น *Inter*, *Roboto*, หรือ *Outfit*) แทนฟอนต์พื้นฐานของเบราว์เซอร์หรือ OS

---

## 2. การเคลื่อนไหวและการโต้ตอบ (Dynamics)

- **Micro-animations:** ใส่การเคลื่อนไหวเล็กๆ น้อยๆ เมื่อมีการโต้ตอบ (Hover effects, Button scaling, Subtle transitions)
- **Responsive & Alive:** อินเทอร์เฟซต้องทำให้ผู้ใช้รู้สึกว่าเข้าถึงและมีชีวิตชีวา (Reactive UI)

### 2.1 ตัวอย่างที่นำไปใช้แล้ว
- **RengYam Buttons:** การแทนที่แอนิเมชั่นพื้นฐานด้วยดีไซน์ที่ดูพรีเมียมและเป็นมืออาชีพมากขึ้น
- **Ranking Cards:** การจัดวางข้อมูลบนการ์ดที่ดูกระชับและน่าเชื่อถือ
- **Marquee Badge:** การใช้แบดจ์แบบ Marquee (วิ่งวน) หรือพื้นหลังสีเขียวอ่อน (`#E8F5E9`) สำหรับวันที่ดีที่สุด (Best Day)

---

## 3. กฎทางเทคนิคสำหรับ UI (Technical Constraints)

- **Don't use Placeholders:** ห้ามใช้ภาพ Placeholder ให้ใช้วิธี Generate ภาพจริงที่มีคุณภาพสูง
- **Data Binding Context:** ใน Android (number-androidx) ต้องระวังเรื่อง `<layout>` tag หากมีการใช้ `<include>` ต้องหุ้มด้วย `<layout>` เสมอเพื่อไม่ให้ Data Binding พัง

---

## 4. แหล่งข้อมูลสำหรับดีไซน์

- **Google Fonts:** Inter, Outfit, Roboto
- **Icons:** Premium 3D Icons (เช่น ชุดไอคอนหมวดหมู่มงคลที่สร้างขึ้นใหม่)
- **Color Palette Reference:**
  - `forbiddenColor`: `#D32F2F` (แดงเข้มขรึม)
  - `auspiciousColor`: `#F57C00` (ส้มทอง)
  - `safeDayColor`: `#E8F5E9` (เขียวอ่อนสะอาด)
