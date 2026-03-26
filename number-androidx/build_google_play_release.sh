#!/bin/bash

# สคริปต์สำหรับสร้าง AAB และเตรียมไฟล์สำหรับอัปโหลดขึ้น Google Play Store
# Google Play Release Builder Script

set -e  # หยุดทำงานทันทีเมื่อเจอ error

# สี ANSI สำหรับการแสดงผล
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Google Play Release Builder${NC}"
echo -e "${BLUE}  Number Android App${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# ตรวจสอบว่าอยู่ในโฟลเดอร์ที่ถูกต้อง
if [ ! -f "build.gradle" ]; then
    echo -e "${RED}❌ Error: ไม่พบไฟล์ build.gradle${NC}"
    echo -e "${RED}   กรุณารันสคริปต์นี้ในโฟลเดอร์ number-android${NC}"
    exit 1
fi

# ตรวจสอบว่ามี keystore file
KEYSTORE_PATH="/Users/tayap/KeyStoreNumberniceIc.jks"
if [ ! -f "$KEYSTORE_PATH" ]; then
    echo -e "${RED}❌ Error: ไม่พบไฟล์ keystore ที่ $KEYSTORE_PATH${NC}"
    exit 1
fi

echo -e "${GREEN}✓ พบไฟล์ keystore${NC}"
echo ""

# อ่าน version จาก build.gradle
VERSION_CODE=$(grep "versionCode" app/build.gradle | awk '{print $2}')
VERSION_NAME=$(grep "versionName" app/build.gradle | awk '{print $2}' | tr -d '"')

echo -e "${BLUE}ข้อมูลเวอร์ชัน:${NC}"
echo -e "  Version Code: ${GREEN}$VERSION_CODE${NC}"
echo -e "  Version Name: ${GREEN}$VERSION_NAME${NC}"
echo ""

# ถามผู้ใช้ว่าต้องการดำเนินการต่อหรือไม่
read -p "ต้องการสร้าง AAB สำหรับเวอร์ชันนี้หรือไม่? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}ยกเลิกการสร้าง AAB${NC}"
    exit 0
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  ขั้นตอนที่ 1: Clean Project${NC}"
echo -e "${BLUE}========================================${NC}"
./gradlew clean
echo -e "${GREEN}✓ Clean เสร็จสิ้น${NC}"
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  ขั้นตอนที่ 2: Build Release AAB${NC}"
echo -e "${BLUE}========================================${NC}"
./gradlew bundleRelease
echo -e "${GREEN}✓ Build AAB เสร็จสิ้น${NC}"
echo ""

# ตรวจสอบว่าไฟล์ AAB ถูกสร้างสำเร็จ
AAB_PATH="app/build/outputs/bundle/release/app-release.aab"
if [ ! -f "$AAB_PATH" ]; then
    echo -e "${RED}❌ Error: ไม่พบไฟล์ AAB ที่ $AAB_PATH${NC}"
    exit 1
fi

# แสดงข้อมูลไฟล์ AAB
AAB_SIZE=$(du -h "$AAB_PATH" | cut -f1)
echo -e "${GREEN}✓ สร้างไฟล์ AAB สำเร็จ${NC}"
echo -e "  ตำแหน่ง: ${BLUE}$AAB_PATH${NC}"
echo -e "  ขนาดไฟล์: ${BLUE}$AAB_SIZE${NC}"
echo ""

# สร้างโฟลเดอร์สำหรับเก็บไฟล์ release
RELEASE_DIR="releases/v${VERSION_NAME}_${VERSION_CODE}"
mkdir -p "$RELEASE_DIR"

# คัดลอกไฟล์ AAB ไปยังโฟลเดอร์ release
cp "$AAB_PATH" "$RELEASE_DIR/app-release-v${VERSION_NAME}.aab"
echo -e "${GREEN}✓ คัดลอกไฟล์ AAB ไปยัง: ${BLUE}$RELEASE_DIR${NC}"

# คัดลอก release notes
if [ -f "release-notes-${VERSION_NAME}.txt" ]; then
    cp "release-notes-${VERSION_NAME}.txt" "$RELEASE_DIR/"
    echo -e "${GREEN}✓ คัดลอก Release Notes${NC}"
fi

# สร้างไฟล์ checksum
cd "$RELEASE_DIR"
shasum -a 256 "app-release-v${VERSION_NAME}.aab" > "app-release-v${VERSION_NAME}.aab.sha256"
echo -e "${GREEN}✓ สร้างไฟล์ checksum (SHA-256)${NC}"
cd - > /dev/null

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  สรุปผลการสร้าง AAB${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}✓ สร้าง AAB สำเร็จแล้ว!${NC}"
echo ""
echo -e "ไฟล์ที่สร้าง:"
echo -e "  1. ${BLUE}$RELEASE_DIR/app-release-v${VERSION_NAME}.aab${NC}"
echo -e "  2. ${BLUE}$RELEASE_DIR/app-release-v${VERSION_NAME}.aab.sha256${NC}"
if [ -f "$RELEASE_DIR/release-notes-${VERSION_NAME}.txt" ]; then
    echo -e "  3. ${BLUE}$RELEASE_DIR/release-notes-${VERSION_NAME}.txt${NC}"
fi
echo ""

echo -e "${YELLOW}📋 ขั้นตอนถัดไป:${NC}"
echo -e "  1. ไปที่ Google Play Console: ${BLUE}https://play.google.com/console${NC}"
echo -e "  2. เลือกแอป 'Number' หรือ 'numberniceic'"
echo -e "  3. ไปที่ Production > Create new release"
echo -e "  4. อัปโหลดไฟล์: ${BLUE}$RELEASE_DIR/app-release-v${VERSION_NAME}.aab${NC}"
echo -e "  5. กรอก Release Notes จากไฟล์: ${BLUE}release-notes-${VERSION_NAME}.txt${NC}"
echo -e "  6. Review และ Submit for review"
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  เสร็จสิ้น!${NC}"
echo -e "${GREEN}========================================${NC}"
