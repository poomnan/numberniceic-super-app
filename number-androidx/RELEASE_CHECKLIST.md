# Checklist สำหรับการอัปโหลดแอปขึ้น Google Play Store

## ก่อนการ Build

### ข้อมูลเวอร์ชัน
- [ ] เพิ่ม `versionCode` ใน `app/build.gradle` (ปัจจุบัน: 1004)
- [ ] อัปเดต `versionName` ใน `app/build.gradle` (ปัจจุบัน: 3.0.8)
- [ ] ตรวจสอบว่า versionCode สูงกว่าเวอร์ชันก่อนหน้า

### การทดสอบ
- [ ] ทดสอบแอปบนอุปกรณ์จริงอย่างน้อย 2 เครื่อง
- [ ] ทดสอบบน Android เวอร์ชันต่างๆ (API 24-35)
- [ ] ทดสอบฟีเจอร์ใหม่ทั้งหมด
- [ ] ทดสอบการ login/logout
- [ ] ทดสอบการซื้อ VIP (ถ้ามี)
- [ ] ทดสอบ Firebase notifications
- [ ] ทดสอบการอัปโหลดรูปภาพ
- [ ] ทดสอบการค้นหาและกรองข้อมูล

### การตั้งค่า
- [ ] ตรวจสอบ `applicationId` ใน build.gradle (com.numberniceic)
- [ ] ตรวจสอบ `targetSdk` (ควรเป็น API 34 หรือสูงกว่า)
- [ ] ตรวจสอบ `minSdk` (API 24)
- [ ] ตรวจสอบ signing configuration
- [ ] ตรวจสอบว่า keystore file มีอยู่และใช้งานได้

### Permissions
- [ ] ตรวจสอบ permissions ใน `AndroidManifest.xml`
- [ ] ลบ permissions ที่ไม่จำเป็น
- [ ] เพิ่ม permission declarations สำหรับ Android 12+
- [ ] ตรวจสอบ runtime permissions

### ProGuard/R8
- [ ] ตรวจสอบ `minifyEnabled` (ปัจจุบัน: false)
- [ ] ถ้าเปิด minify ต้องทดสอบ release build อย่างละเอียด
- [ ] ตรวจสอบ proguard-rules.pro

---

## การ Build

### Build Process
- [ ] รัน `./gradlew clean`
- [ ] รัน `./gradlew bundleRelease` หรือ `./build_google_play_release.sh`
- [ ] ตรวจสอบว่าไม่มี build errors
- [ ] ตรวจสอบว่าไม่มี warnings สำคัญ

### ตรวจสอบไฟล์ AAB
- [ ] ตรวจสอบว่าไฟล์ AAB ถูกสร้างที่ `app/build/outputs/bundle/release/`
- [ ] ตรวจสอบขนาดไฟล์ (ไม่ควรใหญ่เกินไป)
- [ ] ตรวจสอบว่าไฟล์ signed ด้วย release keystore

---

## เตรียมข้อมูลสำหรับ Google Play Console

### Release Notes
- [ ] เขียน Release Notes ภาษาไทย
- [ ] เขียน Release Notes ภาษาอังกฤษ (ถ้าต้องการ)
- [ ] ความยาวไม่เกิน 500 ตัวอักษร
- [ ] ระบุฟีเจอร์ใหม่
- [ ] ระบุการแก้ไขบั๊ก
- [ ] ระบุการปรับปรุง

### ภาพหน้าจอ (Screenshots)
- [ ] เตรียมภาพหน้าจอโทรศัพท์ (อย่างน้อย 2 ภาพ, แนะนำ 8 ภาพ)
  - [ ] หน้าหลัก
  - [ ] หน้าค้นหาชื่อ
  - [ ] หน้าผลลัพธ์
  - [ ] หน้าชื่อเล่น
  - [ ] หน้าบทความ
  - [ ] หน้า VIP (ถ้ามี)
- [ ] ภาพขนาด 1080 x 1920 px หรือ 1080 x 2340 px
- [ ] รูปแบบ PNG หรือ JPG
- [ ] ไม่มี alpha channel

### Feature Graphic (ถ้าต้องการอัปเดต)
- [ ] ขนาด 1024 x 500 px
- [ ] รูปแบบ PNG หรือ JPG
- [ ] ไม่มีความโปร่งใส
- [ ] ดูดีบน Google Play Store

### App Icon (ถ้าต้องการอัปเดต)
- [ ] ขนาด 512 x 512 px
- [ ] รูปแบบ PNG
- [ ] 32-bit PNG (with alpha)

---

## Google Play Console

### เข้าสู่ระบบ
- [ ] เข้า https://play.google.com/console
- [ ] เลือกแอป "Number" หรือ "numberniceic"
- [ ] ตรวจสอบสถานะแอปปัจจุบัน

### สร้าง Release
- [ ] ไปที่ Production > Create new release
- [ ] อัปโหลดไฟล์ AAB
- [ ] รอให้ Google ตรวจสอบไฟล์ (1-2 นาที)
- [ ] ตรวจสอบ App Bundle Explorer

### กรอกข้อมูล
- [ ] Release name: "3.0.8 (1004)"
- [ ] Release notes: คัดลอกจากไฟล์ที่เตรียมไว้
- [ ] ตรวจสอบ countries/regions
- [ ] เลือก rollout percentage (แนะนำเริ่มที่ 10-20%)

### ตรวจสอบและแก้ไข
- [ ] ตรวจสอบ Warnings (ถ้ามี)
- [ ] แก้ไข Errors (ถ้ามี)
- [ ] ตรวจสอบ App signing
- [ ] ตรวจสอบ Target API level

---

## Review และ Publish

### Review Release
- [ ] ตรวจสอบข้อมูลทั้งหมดอีกครั้ง
- [ ] ตรวจสอบ version code และ version name
- [ ] ตรวจสอบ release notes
- [ ] ตรวจสอบ rollout percentage

### Submit
- [ ] คลิก "Review release"
- [ ] อ่านสรุปข้อมูลทั้งหมด
- [ ] คลิก "Start rollout to Production"
- [ ] ยืนยันการเผยแพร่

### รอการอนุมัติ
- [ ] รอ email จาก Google (1-7 วัน)
- [ ] ตรวจสอบสถานะใน Play Console
- [ ] เตรียมพร้อมแก้ไขถ้า Google ปฏิเสธ

---

## หลังการเผยแพร่

### Monitor
- [ ] ตรวจสอบ Dashboard ใน Play Console
- [ ] ตรวจสอบจำนวนการติดตั้ง
- [ ] ตรวจสอบ crash reports
- [ ] ตรวจสอบ ANR reports
- [ ] ตรวจสอบ user reviews

### Staged Rollout (ถ้าใช้)
- [ ] เริ่มที่ 10-20%
- [ ] รอ 1-2 วัน ตรวจสอบ crashes
- [ ] เพิ่มเป็น 50%
- [ ] รอ 1-2 วัน ตรวจสอบ feedback
- [ ] เพิ่มเป็น 100%

### Backup
- [ ] Backup ไฟล์ AAB
- [ ] Backup keystore file
- [ ] Backup release notes
- [ ] Backup screenshots

---

## ในกรณีฉุกเฉิน

### Rollback Plan
- [ ] เตรียม rollback plan ก่อนเผยแพร่
- [ ] รู้วิธีหยุด rollout
- [ ] รู้วิธี rollback ไปเวอร์ชันก่อนหน้า
- [ ] เตรียมข้อความแจ้งผู้ใช้

### Contact Information
- [ ] มีข้อมูลติดต่อ Google Play Support
- [ ] มีข้อมูลติดต่อทีมพัฒนา
- [ ] มีข้อมูลติดต่อผู้ดูแลระบบ

---

## ข้อมูลสำคัญ

### Keystore
- **Location**: `/Users/tayap/KeyStoreNumberniceIc.jks`
- **Store Password**: IntelliP24.X
- **Key Alias**: key0
- **Key Password**: IntelliP24.X

⚠️ **คำเตือน**: อย่าแชร์ข้อมูล keystore กับใคร!

### Application Info
- **Package Name**: com.numberniceic
- **Current Version Code**: 1004
- **Current Version Name**: 3.0.8
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)

---

## หมายเหตุ

- ✅ = เสร็จสิ้น
- ⏳ = กำลังดำเนินการ
- ❌ = ยังไม่ได้ทำ
- ⚠️ = ต้องระวัง

**วันที่สร้าง**: 23 มกราคม 2026
**สำหรับเวอร์ชัน**: 3.0.8 (1004)
