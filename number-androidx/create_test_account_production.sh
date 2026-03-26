#!/bin/bash

# สคริปต์สำหรับสร้างบัญชีทดสอบบน Production Server
# Create Test Account on Production Server for Google Play Review

SERVER_IP="43.228.85.200"
SERVER_USER="tayap"
REMOTE_DIR="/home/tayap/ananya-php"

echo "========================================="
echo "  สร้างบัญชีทดสอบบน Production Server"
echo "========================================="
echo ""

# 1. อัปโหลดสคริปต์ไปยัง server
echo "📤 กำลังอัปโหลดสคริปต์ไปยัง server..."
scp ../number-php/create_test_account.php $SERVER_USER@$SERVER_IP:$REMOTE_DIR/

if [ $? -ne 0 ]; then
    echo "❌ อัปโหลดสคริปต์ไม่สำเร็จ"
    exit 1
fi

echo "✓ อัปโหลดสคริปต์สำเร็จ"
echo ""

# 2. รันสคริปต์บน server
echo "🚀 กำลังสร้างบัญชีทดสอบบน production server..."
echo ""

ssh $SERVER_USER@$SERVER_IP "cd $REMOTE_DIR && php create_test_account.php"

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================="
    echo "✅ สร้างบัญชีทดสอบสำเร็จ!"
    echo "========================================="
    echo ""
    echo "ข้อมูลสำหรับ Google Play Console:"
    echo ""
    echo "Username: test@numberniceic.com"
    echo "Password: Test2026"
    echo ""
    echo "========================================="
else
    echo ""
    echo "❌ เกิดข้อผิดพลาดในการสร้างบัญชี"
    exit 1
fi
