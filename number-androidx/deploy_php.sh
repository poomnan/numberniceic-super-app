#!/bin/bash

# ✅ CORRECT Deploy script for number-php → /var/www/html (Nginx root, port 81)
# ❌ ananya.in.th คือ domain เก่า ห้ามใช้
# ❌ /home/tayap/ananya-php คือ path เก่า nginx ไม่ได้ serve จากที่นี้

SERVER_IP="43.228.85.200"
SERVER_USER="root"
SERVER_PASS="Lydh@58LTG"
REMOTE_DIR="/var/www/html"
PROJECT_PATH="../number-php"

echo "🚀 กำลังเริ่มกระบวนการ Deploy PHP → $REMOTE_DIR บน $SERVER_IP..."

# 1. เข้าไปยังไดเรกทอรีโปรเจกต์
cd "$PROJECT_PATH" || { echo "❌ ไม่พบไดเรกทอรีโปรเจกต์"; exit 1; }

# 2. ส่งไฟล์ไปยังเซิร์ฟเวอร์ด้วย sshpass + rsync
echo "📦 กำลังส่งไฟล์ไปยัง $SERVER_USER@$SERVER_IP:$REMOTE_DIR ..."
export SSHPASS="$SERVER_PASS"
sshpass -e rsync -avz --exclude='.git' \
      --exclude='vendor' \
      --exclude='cache' \
      --exclude='*.log' \
      --exclude='.env' \
      --exclude='backup_*.tar.gz' \
      -e "ssh -o StrictHostKeyChecking=no" \
      ./ $SERVER_USER@$SERVER_IP:$REMOTE_DIR

if [ $? -eq 0 ]; then
    echo "✅ ส่งไฟล์สำเร็จ!"
else
    echo "❌ ส่งไฟล์ไม่สำเร็จ"
    exit 1
fi

# 3. ตั้งค่า permission บนเซิร์ฟเวอร์
echo "⚙️ กำลังตั้งค่า permission..."
sshpass -e ssh -o StrictHostKeyChecking=no $SERVER_USER@$SERVER_IP \
    "cd $REMOTE_DIR && chown -R www-data:www-data . && chmod -R 755 . 2>/dev/null || true"

echo ""
echo "🎉 Deploy PHP เสร็จสิ้น!"
echo "🌐 API URL: http://$SERVER_IP:81/member/lengyam"
