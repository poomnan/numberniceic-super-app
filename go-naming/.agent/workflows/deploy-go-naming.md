---
description: วิธี build และ deploy go-naming backend ขึ้น production server
---

# Deploy go-naming Backend

## ข้อมูล Server

| Key | Value |
|-----|-------|
| IP | 43.228.85.200 |
| User | root |
| Password | Lydh@58LTG |
| Service | go-naming |
| Remote Dir | /home/tayap/go-naming |
| Port | 8095 |

## ⚠️ ข้อสำคัญ: ปัญหา go build บนเครื่อง Mac

เครื่อง Mac ของ user มี **sandbox permission** ที่ทำให้ `go build` ล้มเหลวด้วย error:
```
go: creating work dir: mkdir /var/folders/j_/.../T/go-build...: operation not permitted
```

**วิธีแก้**: กำหนด `GOTMPDIR` และ `GOCACHE` ให้ชี้ไปที่ **directory ใน project** แทน:

```bash
cd /Users/tayap/project-naming/go-naming

GOTMPDIR=/Users/tayap/project-naming/go-naming/tmp-build \
GOCACHE=/Users/tayap/project-naming/go-naming/tmp-build/cache \
GOOS=linux GOARCH=amd64 \
go build -o server-linux main.go && echo "✅ BUILD OK"
```

> `tmp-build/` จะถูกสร้างอัตโนมัติครั้งแรก หลังจากนั้น reuse ได้เลย

---

## Steps การ Deploy

### Step 1 — Build binary สำหรับ Linux

```bash
cd /Users/tayap/project-naming/go-naming

GOTMPDIR=/Users/tayap/project-naming/go-naming/tmp-build \
GOCACHE=/Users/tayap/project-naming/go-naming/tmp-build/cache \
GOOS=linux GOARCH=amd64 \
go build -o server-linux main.go && echo "✅ BUILD OK"
```

### Step 2 — Stop service บน server

```bash
sshpass -p 'Lydh@58LTG' ssh -o StrictHostKeyChecking=no root@43.228.85.200 \
  "systemctl stop go-naming"
```

### Step 3 — Upload binary ขึ้น server

```bash
sshpass -p 'Lydh@58LTG' scp -o StrictHostKeyChecking=no \
  /Users/tayap/project-naming/go-naming/server-linux \
  root@43.228.85.200:/home/tayap/go-naming/server
```

### Step 4 — Start service และตรวจสอบ

```bash
sshpass -p 'Lydh@58LTG' ssh -o StrictHostKeyChecking=no root@43.228.85.200 \
  "systemctl start go-naming && sleep 3 && systemctl is-active go-naming"
```

ถ้าขึ้น `active` = สำเร็จ

### Step 5 — ตรวจสอบ API (optional)

```bash
curl -s http://43.228.85.200:8095/api/v1/health
```

หรือทดสอบ endpoint ที่ต้องการ

---

## Troubleshooting

### Service ขึ้น "activating" ไม่ใช่ "active"
รอสัก 3-5 วินาทีแล้ว check อีกครั้ง:
```bash
sshpass -p 'Lydh@58LTG' ssh -o StrictHostKeyChecking=no root@43.228.85.200 \
  "systemctl is-active go-naming"
```

### ดู log error
```bash
sshpass -p 'Lydh@58LTG' ssh -o StrictHostKeyChecking=no root@43.228.85.200 \
  "journalctl -u go-naming -n 30 --no-pager"
```

### ตรวจสอบ status ละเอียด
```bash
sshpass -p 'Lydh@58LTG' ssh -o StrictHostKeyChecking=no root@43.228.85.200 \
  "systemctl status go-naming --no-pager | head -20"
```

### sshpass ล้มเหลวด้วย "Failed to get a pseudo terminal"
> ปัญหานี้เกิดใน AI Agent environment เท่านั้น (ไม่มี pty)
> ให้ **user รันคำสั่งเองใน Terminal ของ Mac** แทน
> AI จะ build ไม่ได้แต่ user รันปกติได้

---

## ทำทุกอย่างใน 1 คำสั่ง (All-in-one)

```bash
cd /Users/tayap/project-naming/go-naming && \
GOTMPDIR=/Users/tayap/project-naming/go-naming/tmp-build \
GOCACHE=/Users/tayap/project-naming/go-naming/tmp-build/cache \
GOOS=linux GOARCH=amd64 go build -o server-linux main.go && \
echo "Build OK" && \
sshpass -p 'Lydh@58LTG' ssh -o StrictHostKeyChecking=no root@43.228.85.200 "systemctl stop go-naming" && \
sshpass -p 'Lydh@58LTG' scp -o StrictHostKeyChecking=no server-linux root@43.228.85.200:/home/tayap/go-naming/server && \
sshpass -p 'Lydh@58LTG' ssh -o StrictHostKeyChecking=no root@43.228.85.200 "systemctl start go-naming && sleep 3 && systemctl is-active go-naming" && \
echo "✅ DEPLOY COMPLETE"
```
