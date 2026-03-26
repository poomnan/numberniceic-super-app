# Releases Directory

โฟลเดอร์นี้เก็บไฟล์ AAB และเอกสารที่เกี่ยวข้องสำหรับแต่ละเวอร์ชันที่เผยแพร่

## โครงสร้างโฟลเดอร์

```
releases/
├── v3.0.6_1002/
│   ├── app-release-v3.0.6.aab
│   ├── app-release-v3.0.6.aab.sha256
│   └── release-notes-3.0.6.txt
├── v3.0.5_1001/
│   └── ...
└── README.md (ไฟล์นี้)
```

## รูปแบบการตั้งชื่อ

- โฟลเดอร์: `v{VERSION_NAME}_{VERSION_CODE}`
- ไฟล์ AAB: `app-release-v{VERSION_NAME}.aab`
- Checksum: `app-release-v{VERSION_NAME}.aab.sha256`
- Release Notes: `release-notes-{VERSION_NAME}.txt`

## วิธีใช้งาน

### สร้าง Release ใหม่
```bash
cd /Users/tayap/project-number/number-android
./build_google_play_release.sh
```

สคริปต์จะ:
1. Build AAB file
2. สร้างโฟลเดอร์ใหม่ใน `releases/`
3. คัดลอกไฟล์ AAB และเอกสารที่เกี่ยวข้อง
4. สร้างไฟล์ checksum (SHA-256)

### ตรวจสอบ Checksum
```bash
cd releases/v3.0.6_1002
shasum -a 256 -c app-release-v3.0.6.aab.sha256
```

## ข้อมูลสำคัญ

### เวอร์ชันปัจจุบัน
- **Version**: 3.0.6 (1002)
- **Release Date**: 23 มกราคม 2026
- **Status**: Pending Upload

### การจัดเก็บ
- เก็บไฟล์ AAB ทุกเวอร์ชันเพื่อการ rollback
- เก็บ release notes สำหรับอ้างอิง
- เก็บ checksum เพื่อตรวจสอบความถูกต้องของไฟล์

### Backup
- Backup โฟลเดอร์นี้ไปยัง cloud storage
- Backup keystore file แยกต่างหาก
- ไม่ควร commit ไฟล์ AAB ขึ้น Git (ใหญ่เกินไป)

## หมายเหตุ

- ไฟล์ AAB มีขนาดใหญ่ ไม่ควร commit ขึ้น Git
- ใช้ `.gitignore` เพื่อ ignore โฟลเดอร์ `releases/`
- เก็บ backup ไว้ใน cloud storage หรือ external drive
- ตรวจสอบ checksum ก่อนอัปโหลดทุกครั้ง

---

**อัปเดตล่าสุด**: 23 มกราคม 2026
