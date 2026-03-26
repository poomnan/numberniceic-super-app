#!/bin/bash

# --- การตั้งค่า ---
OUTPUT_DIR="app/release"
AAB_PATH="app/build/outputs/bundle/release/app-release.aab"
APK_PATH="app/build/outputs/apk/release/app-release.apk"

echo "🚀 กำลังเริ่มกระบวนการ Build Android Release..."

# 1. ทำความสะอาดและ Build
echo "📦 1/2 กำลัง Build Bundle (AAB) และ APK..."
./gradlew clean bundleRelease assembleRelease

if [ $? -eq 0 ]; then
    echo "✅ Build สำเร็จ!"
else
    echo "❌ Build ไม่สำเร็จ กรุณาตรวจสอบข้อผิดพลาด"
    exit 1
fi

# 2. จัดเตรียมไฟล์ผลลัพธ์
echo "📂 2/2 กำลังคัดเลือกไฟล์ไปยังโฟลเดอร์ $OUTPUT_DIR..."
mkdir -p $OUTPUT_DIR

if [ -f "$AAB_PATH" ]; then
    cp "$AAB_PATH" "$OUTPUT_DIR/numberniceic-release.aab"
    echo "📄 AAB: $OUTPUT_DIR/numberniceic-release.aab"
fi

if [ -f "$APK_PATH" ]; then
    cp "$APK_PATH" "$OUTPUT_DIR/numberniceic-release.apk"
    echo "📄 APK: $OUTPUT_DIR/numberniceic-release.apk"
fi

echo "🎉 กระบวนการ Build Release เสร็จสิ้น!"
echo "คุณสามารถนำไฟล์ในโฟลเดอร์ $OUTPUT_DIR ไป Deploy ต่อได้"
