#!/bin/bash

# Configuration
PROJECT_PATH="../chuedee-flutter"

echo "🚀 กำลังเริ่มกระบวนการ Build Flutter Release (chuedee-flutter)..."

# 1. เข้าไปยังไดเรกทอรีโปรเจกต์
cd "$PROJECT_PATH" || { echo "❌ ไม่พบไดเรกทอรีโปรเจกต์"; exit 1; }

# 2. ทำความสะอาดและดึง dependencies
echo "🧹 1/3 กำลังทำความสะอาดและดึง dependencies..."
flutter clean
flutter pub get

# 3. Build Release
echo "📦 2/3 กำลัง Build APK และ App Bundle (Release)..."
flutter build apk --release
flutter build appbundle --release

if [ $? -eq 0 ]; then
    echo "✅ Build สำเร็จ!"
else
    echo "❌ Build ไม่สำเร็จ"
    exit 1
fi

# 4. แสดงตำแหน่งไฟล์ผลลัพธ์
echo "📂 3/3 ไฟล์ผลลัพธ์อยู่ที่:"
echo "APK: build/app/outputs/flutter-apk/app-release.apk"
echo "AAB: build/app/outputs/bundle/release/app-release.aab"

echo "🎉 กระบวนการ Build Flutter เสร็จสิ้น!"
