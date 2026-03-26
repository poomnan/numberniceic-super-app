#!/bin/bash

# --- การตั้งค่าโปรเจกต์ ---
PACKAGE_NAME="com.numberniceic"
LAUNCH_ACTIVITY=".ui.SplashActivity"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

echo "🚀 กำลังเริ่มกระบวนการรัน Android App (NumberniceIc)..."

# 1. ทำความสะอาดและ Build APK
echo "📦 1/3 กำลัง Build Debug APK..."
./gradlew -Dgradle.user.home=$(pwd)/.gradle_home assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Build สำเร็จ!"
else
    echo "❌ Build ไม่สำเร็จ กรุณาตรวจสอบข้อผิดพลาดด้านบน"
    exit 1
fi

# 2. ค้นหาอุปกรณ์ทั้งหมดที่เชื่อมต่อ
echo "📲 2/3 กำลังค้นหาอุปกรณ์..."

DEVICES=$(adb devices | grep -v "List" | grep "device$" | awk '{print $1}')

if [ -z "$DEVICES" ]; then
    echo "❌ ไม่พบอุปกรณ์ที่เชื่อมต่อ (กรุณาเปิด Emulator หรือเสียบสาย USB)"
    exit 1
fi

echo "🔹 พบอุปกรณ์ดังนี้:"
echo "$DEVICES"

# 3. วนลูปติดตั้งและรันทีละเครื่อง
for DEVICE_ID in $DEVICES
do
    echo "--------------------------------------------------"
    echo "📲 กำลังจัดการเครื่อง: $DEVICE_ID"
    echo "🧹 ลบแอปเดิมเพื่อแก้ปัญหา Signature mismatch..."
    adb -s "$DEVICE_ID" uninstall "$PACKAGE_NAME"
    
    echo "📦 กำลังติดตั้ง APK..."
    adb -s "$DEVICE_ID" install -r $APK_PATH
    
    if [ $? -eq 0 ]; then
        echo "✅ ติดตั้งสำเร็จ ($DEVICE_ID)"
        echo "🏃 กำลังเปิดแอปพลิเคชัน ($DEVICE_ID)..."
        adb -s "$DEVICE_ID" shell am start -n "$PACKAGE_NAME/$LAUNCH_ACTIVITY"
    else
        echo "❌ ติดตั้งไม่สำเร็จ ($DEVICE_ID)"
    fi
done

echo "--------------------------------------------------"
echo "🎉 เสร็จสิ้นขั้นตอนสำหรับทุกอุปกรณ์!"
