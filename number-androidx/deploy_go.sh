#!/bin/bash

# Configuration
PROJECT_PATH="../../apps-go/ananya-go"

echo "🚀 กำลังเริ่มกระบวนการ Deploy Go (ananya-go)..."

# ตรวจสอบว่ามีไฟล์ deploy_ubuntu.sh ในโปรเจกต์ Go หรือไม่
if [ -f "$PROJECT_PATH/deploy_ubuntu.sh" ]; then
    echo "📦 กำลังรันสคริปต์ deploy_ubuntu.sh จากโปรเจกต์ Go..."
    cd "$PROJECT_PATH" && ./deploy_ubuntu.sh
else
    echo "❌ ไม่พบสคริปต์ deploy_ubuntu.sh ใน $PROJECT_PATH"
    exit 1
fi
