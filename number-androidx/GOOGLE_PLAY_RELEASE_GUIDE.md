# คู่มือการอัปโหลดแอป Number Android ขึ้น Google Play Store

## ข้อมูลเวอร์ชันปัจจุบัน
- **Version Code**: 1004
- **Version Name**: 3.0.8
- **Application ID**: com.numberniceic
- **Target SDK**: 35
- **Min SDK**: 24

---

## ขั้นตอนที่ 1: เตรียมไฟล์ Release Build (AAB)

### 1.1 ตรวจสอบการตั้งค่า Signing
ไฟล์ `app/build.gradle` มีการตั้งค่า signing config แล้ว:
```gradle
signingConfigs {
    release {
        storeFile file("/Users/tayap/KeyStoreNumberniceIc.jks")
        storePassword "IntelliP24.X"
        keyAlias "key0"
        keyPassword "IntelliP24.X"
    }
}
```

### 1.2 สร้างไฟล์ AAB (Android App Bundle)
รันคำสั่งต่อไปนี้ในโฟลเดอร์ `number-android`:

```bash
./gradlew clean
./gradlew bundleRelease
```

หรือใช้สคริปต์ที่มีอยู่แล้ว:
```bash
./build_release.sh
```

### 1.3 ตำแหน่งไฟล์ AAB ที่สร้างเสร็จ
ไฟล์ AAB จะอยู่ที่:
```
/Users/tayap/project-number/number-android/app/build/outputs/bundle/release/app-release.aab
```

---

## ขั้นตอนที่ 2: เตรียมข้อมูลสำหรับ Google Play Console

### 2.1 Release Notes (บันทึกการอัปเดต)
สร้างบันทึกการอัปเดตสำหรับเวอร์ชัน 3.0.8 ในภาษาไทย:

**ตัวอย่าง Release Notes:**
```
เวอร์ชัน 3.0.8
- ปรับปรุงระบบแนะนำชื่อเล่นด้วย AI
- เพิ่มความเร็วในการค้นหาชื่อ
- แก้ไขบั๊กเล็กน้อยเพื่อประสบการณ์ที่ดีขึ้น
- ปรับปรุงประสิทธิภาพโดยรวม
```

### 2.2 ภาพหน้าจอ (Screenshots)
ตรวจสอบว่ามีภาพหน้าจอที่จำเป็น:
- ภาพหน้าจอโทรศัพท์ (อย่างน้อย 2 ภาพ, แนะนำ 8 ภาพ)
- ภาพหน้าจอแท็บเล็ต 7 นิ้ว (ถ้ามี)
- ภาพหน้าจอแท็บเล็ต 10 นิ้ว (ถ้ามี)

ขนาดที่แนะนำ:
- โทรศัพท์: 1080 x 1920 px หรือ 1080 x 2340 px
- แท็บเล็ต 7": 1200 x 1920 px
- แท็บเล็ต 10": 1600 x 2560 px

### 2.3 Feature Graphic
- ขนาด: 1024 x 500 px
- รูปแบบ: PNG หรือ JPG
- ไม่มีความโปร่งใส

### 2.4 App Icon
- ขนาด: 512 x 512 px
- รูปแบบ: PNG
- 32-bit PNG (with alpha)

---

## ขั้นตอนที่ 3: อัปโหลดไฟล์ AAB ขึ้น Google Play Console

### 3.1 เข้าสู่ Google Play Console
1. ไปที่ https://play.google.com/console
2. เข้าสู่ระบบด้วยบัญชี Google ที่ใช้จัดการแอป
3. เลือกแอป "Number" หรือ "numberniceic"

### 3.2 สร้าง Release ใหม่
1. ไปที่เมนู **Production** (การผลิต) หรือ **Testing** > **Internal testing** (ถ้าต้องการทดสอบก่อน)
2. คลิก **Create new release** (สร้างรุ่นใหม่)
3. อัปโหลดไฟล์ AAB:
   - คลิก **Upload** และเลือกไฟล์ `app-release.aab`
   - รอให้ Google ตรวจสอบไฟล์ (อาจใช้เวลา 1-2 นาที)

### 3.3 กรอกข้อมูล Release
1. **Release name**: ใส่ชื่อเวอร์ชัน เช่น "3.0.8 (1004)"
2. **Release notes**: คัดลอกจาก Release Notes ที่เตรียมไว้ในข้อ 2.1
3. ตรวจสอบ **App Bundle Explorer** ว่า Google สร้าง APK ต่างๆ ได้ถูกต้อง

### 3.4 ตรวจสอบและแก้ไขปัญหา
Google Play Console จะแสดงคำเตือนหรือข้อผิดพลาด (ถ้ามี):
- **Warnings** (คำเตือน): สามารถดำเนินการต่อได้ แต่ควรแก้ไข
- **Errors** (ข้อผิดพลาด): ต้องแก้ไขก่อนจึงจะอัปโหลดได้

ปัญหาที่พบบ่อย:
- **Permission issues**: ตรวจสอบ AndroidManifest.xml
- **Target SDK version**: ต้องเป็น API level ที่ Google กำหนด
- **App signing**: ตรวจสอบว่าใช้ Google Play App Signing

---

## ขั้นตอนที่ 4: Review และ Publish

### 4.1 Review Release
1. ตรวจสอบข้อมูลทั้งหมดอีกครั้ง:
   - Version code และ version name
   - Release notes
   - Countries/regions (ประเทศที่จะเผยแพร่)
   - Rollout percentage (เปอร์เซ็นต์การเผยแพร่)

### 4.2 เลือกประเภทการเผยแพร่
- **Staged rollout**: เผยแพร่ทีละน้อย (เช่น 10%, 25%, 50%, 100%)
- **Full rollout**: เผยแพร่เต็มรูปแบบ 100%

### 4.3 Submit for Review
1. คลิก **Review release** (ตรวจสอบรุ่น)
2. ตรวจสอบสรุปข้อมูลทั้งหมด
3. คลิก **Start rollout to Production** (เริ่มเผยแพร่สู่การผลิต)

### 4.4 รอการอนุมัติ
- Google จะตรวจสอบแอปภายใน 1-7 วัน (โดยปกติ 1-3 วัน)
- คุณจะได้รับอีเมลแจ้งเตือนเมื่อแอปได้รับการอนุมัติหรือถูกปฏิเสธ

---

## ขั้นตอนที่ 5: หลังการเผยแพร่

### 5.1 ตรวจสอบสถานะ
- ไปที่ **Dashboard** ใน Google Play Console
- ตรวจสอบ:
  - จำนวนการติดตั้ง
  - คะแนนรีวิว
  - Crash reports
  - ANR (Application Not Responding) reports

### 5.2 Monitor Crashes
- ไปที่ **Quality** > **Android vitals**
- ตรวจสอบ crash rate และ ANR rate
- แก้ไขปัญหาที่พบ

### 5.3 User Reviews
- ตอบกลับรีวิวของผู้ใช้
- รวบรวม feedback สำหรับการพัฒนาในอนาคต

---

## Checklist ก่อนอัปโหลด

- [ ] เพิ่ม versionCode และ versionName ใน build.gradle
- [ ] ทดสอบแอปบนอุปกรณ์จริง
- [ ] ตรวจสอบ signing configuration
- [ ] สร้างไฟล์ AAB สำเร็จ
- [ ] เตรียม Release Notes (ภาษาไทยและภาษาอังกฤษ)
- [ ] เตรียมภาพหน้าจอ (ถ้าต้องการอัปเดต)
- [ ] ตรวจสอบ permissions ใน AndroidManifest.xml
- [ ] ทดสอบ ProGuard/R8 (ถ้าเปิดใช้งาน minifyEnabled)
- [ ] ตรวจสอบ app size และ download size
- [ ] Backup ไฟล์ AAB และ keystore

---

## คำสั่งที่ใช้บ่อย

### สร้าง AAB
```bash
cd /Users/tayap/project-number/number-android
./gradlew clean bundleRelease
```

### ตรวจสอบ AAB ที่สร้างแล้ว
```bash
ls -lh app/build/outputs/bundle/release/
```

### ดูข้อมูล AAB
```bash
bundletool build-apks --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=app-release.apks \
  --mode=universal
```

### ทดสอบ AAB บนอุปกรณ์
```bash
bundletool install-apks --apks=app-release.apks
```

---

## ข้อมูลสำคัญ

### Keystore Information
- **Location**: `/Users/tayap/KeyStoreNumberniceIc.jks`
- **Store Password**: IntelliP24.X
- **Key Alias**: key0
- **Key Password**: IntelliP24.X

⚠️ **สำคัญ**: เก็บไฟล์ keystore ไว้อย่างปลอดภัย! หากสูญหาย จะไม่สามารถอัปเดตแอปได้

### Google Play App Signing
ถ้าใช้ Google Play App Signing:
- Google จะจัดการ signing key ให้
- คุณใช้ upload key สำหรับอัปโหลด AAB
- ปลอดภัยกว่าและแนะนำให้ใช้

---

## ปัญหาที่พบบ่อยและวิธีแก้ไข

### 1. "You uploaded an APK or Android App Bundle that was signed in debug mode"
**วิธีแก้**: ตรวจสอบว่าใช้ `bundleRelease` ไม่ใช่ `bundleDebug`

### 2. "Version code X has already been used"
**วิธีแก้**: เพิ่ม versionCode ใน build.gradle

### 3. "You need to use a different package name"
**วิธีแก้**: ตรวจสอบ applicationId ใน build.gradle ว่าตรงกับที่ลงทะเบียนไว้

### 4. "Upload failed: You uploaded an APK with an invalid signature"
**วิธีแก้**: ตรวจสอบ keystore path และ password

### 5. "This release is not compliant with the Google Play 64-bit requirement"
**วิธีแก้**: ใช้ AAB แทน APK (AAB จะสร้าง APK ทั้ง 32-bit และ 64-bit อัตโนมัติ)

---

## ทรัพยากรเพิ่มเติม

- [Google Play Console](https://play.google.com/console)
- [Android Developer Documentation](https://developer.android.com/studio/publish)
- [App Bundle Documentation](https://developer.android.com/guide/app-bundle)
- [Release Checklist](https://developer.android.com/distribute/best-practices/launch/launch-checklist)

---

## หมายเหตุ

- แอปจะต้องผ่านการตรวจสอบของ Google ก่อนเผยแพร่
- ระยะเวลาการตรวจสอบโดยเฉลี่ย: 1-3 วัน
- ควรทดสอบแอปอย่างละเอียดก่อนอัปโหลด
- เตรียม rollback plan ในกรณีที่พบปัญหาหลังเผยแพร่

---

**อัปเดตล่าสุด**: 23 มกราคม 2026
**เวอร์ชันเอกสาร**: 1.0
