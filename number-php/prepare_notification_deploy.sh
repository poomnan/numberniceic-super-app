#!/bin/bash

echo "📦 Preparing Notification System Deployment Package..."
echo "======================================================"

# Create deployment directory
DEPLOY_DIR="notification_system_deploy"
rm -rf $DEPLOY_DIR
mkdir -p $DEPLOY_DIR/app/Managers

# Copy files
echo "📋 Copying files..."
cp app/Managers/NotificationManager.php $DEPLOY_DIR/app/Managers/
cp app/Managers/NotificationAPIController.php $DEPLOY_DIR/app/Managers/
cp app/Managers/NotificationController.php $DEPLOY_DIR/app/Managers/
cp app/routes.php $DEPLOY_DIR/app/
cp create_notifications_table.sql $DEPLOY_DIR/

# Create README for deployment
cat > $DEPLOY_DIR/README.txt << 'READMEEOF'
🚀 Notification System Deployment Package
==========================================

📁 Files Included:
------------------
1. app/Managers/NotificationManager.php (NEW)
2. app/Managers/NotificationAPIController.php (NEW)
3. app/Managers/NotificationController.php (UPDATED)
4. app/routes.php (UPDATED)
5. create_notifications_table.sql (DATABASE MIGRATION)

📝 Deployment Steps:
--------------------
1. Extract this zip to your hosting root directory
   - This will overwrite existing files (backup first!)

2. Run database migration:
   - Login to your database (phpMyAdmin or command line)
   - Execute: create_notifications_table.sql
   
   OR via command line:
   psql -U your_db_user -d your_db_name -f create_notifications_table.sql

3. Test API endpoints:
   https://ananya.in.th/api/v2/notifications?memberid=TEST001&type=webview_merit

4. Check logs if there are errors:
   - Check error_log in root directory
   - Check fcm_log.txt for FCM issues

✅ New API Endpoints:
---------------------
GET  /api/v2/notifications
GET  /api/v2/notifications/by-type
POST /api/v2/notifications/mark-read
GET  /api/v2/notifications/unread-count
GET  /api/v2/notifications/statistics
POST /api/v2/notifications/save

🔧 What Changed:
----------------
- NotificationController now saves to database when sending FCM
- New NotificationManager handles database operations
- New API endpoints for Android app to fetch notifications
- Database table 'notifications' stores all notifications

⚠️ Important Notes:
-------------------
- Backup your database before running migration!
- Old SharedPreferences system still works (backward compatible)
- Test on staging first if possible

📚 Documentation:
-----------------
See NOTIFICATION_SYSTEM_IMPLEMENTATION.md for full details
See NOTIFICATION_API_DOCS.md for API documentation
READMEEOF

# Create deployment instructions in Thai
cat > $DEPLOY_DIR/DEPLOY_TH.txt << 'THEOF'
🚀 คู่มือการติดตั้งระบบแจ้งเตือนแบบใหม่
=========================================

📝 ขั้นตอนการติดตั้ง:
--------------------

1. สำรองข้อมูล (BACKUP):
   ✅ สำรอง database ก่อน
   ✅ สำรองไฟล์ app/routes.php
   ✅ สำรองไฟล์ app/Managers/NotificationController.php

2. อัปโหลดไฟล์:
   ✅ แตก zip นี้ไปที่ root directory ของ hosting
   ✅ ไฟล์จะถูก overwrite (ทับไฟล์เดิม)

3. รัน Database Migration:
   ✅ เข้า phpMyAdmin หรือ command line
   ✅ เลือก database ของคุณ
   ✅ รันไฟล์ create_notifications_table.sql
   
   หรือใช้คำสั่ง:
   psql -U ชื่อuser -d ชื่อdatabase -f create_notifications_table.sql

4. ทดสอบ:
   ✅ เปิด: https://ananya.in.th/api/v2/notifications?memberid=TEST001&type=webview_merit
   ✅ ควรเห็น JSON response
   ✅ ถ้าเห็น error ให้เช็ค error_log

✨ ฟีเจอร์ใหม่:
--------------
- แจ้งเตือนจะถูกบันทึกลง database อัตโนมัติ
- รองรับ multi-device (login ที่ไหนก็เห็นแจ้งเตือนเดียวกัน)
- ติดตามว่าผู้ใช้อ่านแล้วหรือยัง
- Admin สามารถจัดการแจ้งเตือนจาก server ได้

🔧 ไฟล์ที่เปลี่ยนแปลง:
---------------------
✅ app/Managers/NotificationManager.php (ใหม่)
✅ app/Managers/NotificationAPIController.php (ใหม่)
✅ app/Managers/NotificationController.php (แก้ไข)
✅ app/routes.php (เพิ่ม API endpoints)
✅ create_notifications_table.sql (database migration)

⚠️ หมายเหตุสำคัญ:
-----------------
- ระบบเก่ายังใช้งานได้ (backward compatible)
- ควรทดสอบบน staging ก่อนถ้าเป็นไปได้
- ถ้ามีปัญหาให้ restore จาก backup
THEOF

echo "✅ Files copied to $DEPLOY_DIR/"

# Create zip file
ZIP_NAME="notification_system_$(date +%Y%m%d_%H%M%S).zip"
echo "📦 Creating zip file: $ZIP_NAME"
cd $DEPLOY_DIR
zip -r ../$ZIP_NAME . -x "*.DS_Store"
cd ..

echo ""
echo "✅ Deployment package ready!"
echo "📦 File: $ZIP_NAME"
echo "📏 Size: $(du -h $ZIP_NAME | cut -f1)"
echo ""
echo "Next steps:"
echo "1. Upload $ZIP_NAME to your hosting"
echo "2. Extract to root directory"
echo "3. Run database migration (see README.txt)"
echo "4. Test API endpoints"
echo ""
