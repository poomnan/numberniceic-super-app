#!/bin/bash

set -euo pipefail

OUTPUT_DIR="app/release"
APK_OUTPUT_DIR="app/build/outputs/apk/release"

echo "🚀 กำลังเริ่มสร้าง Android release APK สำหรับส่งทดสอบ..."

echo "📦 1/3 Clean project"
./gradlew clean

echo "📦 2/3 Assemble release APK"
./gradlew assembleRelease

APK_PATH=$(find "$APK_OUTPUT_DIR" -maxdepth 1 -type f -name '*.apk' | head -n 1)

if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
    echo "❌ ไม่พบไฟล์ APK ใน $APK_OUTPUT_DIR"
    exit 1
fi

VERSION_NAME=$(sed -nE 's/^[[:space:]]*versionName[[:space:]]+"([^"]+)"$/\1/p' app/build.gradle | head -n 1)
VERSION_CODE=$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]+([0-9]+)$/\1/p' app/build.gradle | head -n 1)
SAFE_VERSION_NAME=$(printf "%s" "$VERSION_NAME" | sed -E 's/[^A-Za-z0-9._-]+/-/g; s/-+/-/g; s/^-|-$//g')
SHARE_APK_NAME="numberniceic-test-v${SAFE_VERSION_NAME}-${VERSION_CODE}.apk"

echo "📂 3/3 คัดลอก APK ไปยัง $OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"
cp "$APK_PATH" "$OUTPUT_DIR/$SHARE_APK_NAME"

echo "✅ สร้างไฟล์สำหรับส่งให้ผู้ทดสอบเรียบร้อย"
echo "📄 APK: $OUTPUT_DIR/$SHARE_APK_NAME"
echo ""
echo "หมายเหตุ:"
echo "- ส่งไฟล์ .apk นี้ให้เพื่อนติดตั้งได้เลย"
echo "- อย่าส่งไฟล์ .aab ผ่าน LINE เพราะ Android จะติดตั้งไม่ได้"
