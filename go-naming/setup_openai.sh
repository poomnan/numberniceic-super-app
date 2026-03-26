#!/bin/bash
set -e

echo "=== ตั้งค่า go-naming ให้ใช้ OpenAI เท่านั้น ==="

# 1. อัปเดต OpenAI API Key (แทนที่ด้วย key จริงของคุณ)
OPENAI_API_KEY="sk-xxxx-your-real-api-key-here"
if [[ "$OPENAI_API_KEY" == "sk-xxxx-your-real-api-key-here" ]]; then
  echo "ERROR: กรุณาแทนที่ OPENAI_API_KEY ในสคริปต์นี้ด้วย key จริงของคุณ"
  exit 1
fi

# 2. อัปเดต systemd service file
echo "อัปเดต service file..."
cat > /etc/systemd/system/go-naming.service <<EOF
[Unit]
Description=Go Naming API Service
After=network.target postgresql.service

[Service]
Type=simple
User=root
WorkingDirectory=/usr/local/bin
ExecStart=/usr/local/bin/go-naming
Restart=always
RestartSec=5
Environment="DATABASE_URL=postgres://tayap:IntelliP24.X@localhost:5432/tayap?sslmode=disable"
Environment="PORT=8095"
Environment="OPENAI_API_KEY=${OPENAI_API_KEY}"

[Install]
WantedBy=multi-user.target
EOF

# 3. อัปเดต binary
echo "อัปเดต binary..."
cp /tmp/go-naming-new /usr/local/bin/go-naming
chmod +x /usr/local/bin/go-naming

# 4. Restart service
echo "Restarting service..."
systemctl daemon-reload
systemctl restart go-naming

# 5. ตรวจสอบสถานะ
echo "ตรวจสอบสถานะ service..."
systemctl --no-pager status go-naming

echo ""
echo "✅ การตั้งค่าเสร็จสมบูรณ์"
echo "✅ โค้ดใหม่ใช้ OpenAI เท่านั้น (ไม่มี fallback ไป Ollama)"
echo ""
echo "ทดสอบการค้นหา:"
echo "  curl -X POST -H 'Content-Type: application/json' -d '{\"keyword\":\"แข็งแรง\"}' http://localhost:8095/api/v1/name-search"
echo ""
echo "ตรวจสอบ logs:"
echo "  journalctl -u go-naming -n 20 --no-pager"