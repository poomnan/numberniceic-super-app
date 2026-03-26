# คำสั่งสำหรับตั้งค่า OpenAI API Key และใช้งาน go-naming ด้วย OpenAI เท่านั้น

## 1. อัปเดต OpenAI API Key ใน systemd service
```bash
sudo tee /etc/systemd/system/go-naming.service <<'EOF'
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
Environment="OPENAI_API_KEY=sk-xxxx-your-real-api-key-here"

[Install]
WantedBy=multi-user.target
EOF
```

**แทนที่ `sk-xxxx-your-real-api-key-here` ด้วย OpenAI API Key จริงของคุณ**

## 2. อัปเดต binary (โค้ดใหม่ลบ Ollama fallback แล้ว)
```bash
sudo cp /tmp/go-naming-new /usr/local/bin/go-naming
sudo chmod +x /usr/local/bin/go-naming
```

## 3. Restart service
```bash
sudo systemctl daemon-reload
sudo systemctl restart go-naming
sudo systemctl --no-pager status go-naming
```

## 4. ทดสอบ
```bash
curl -X POST -H "Content-Type: application/json" -d '{"keyword":"แข็งแรง"}' http://localhost:8095/api/v1/name-search | jq .
```

## หมายเหตุ
- โค้ดใหม่ใช้ **OpenAI เท่านั้น** ไม่ fallback ไป Ollama แล้ว
- ตรวจสอบ logs ด้วย `sudo journalctl -u go-naming -n 20 --no-pager`
- ถ้า API Key ถูกต้อง การค้นหาจะได้ผลลัพธ์ชื่อตามความหมาย