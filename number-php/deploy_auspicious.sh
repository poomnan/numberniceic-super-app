#!/bin/bash
# ⚠️ CORRECT DEPLOY SCRIPT — Uses root@43.228.85.200:/var/www/html
# ❌ ananya.in.th คือ domain เก่า ห้ามใช้
HOST="43.228.85.200"
USER="root"
PASS="Lydh@58LTG"
REMOTE_DIR="/var/www/html"

echo "🚀 Starting deployment to $REMOTE_DIR..."

export SSHPASS="$PASS"
sshpass -e scp -o StrictHostKeyChecking=no migrate_auspicious_missing.php "$USER@$HOST:$REMOTE_DIR/public/"

echo "✅ Upload complete! Running migration via curl..."
curl -s "http://$HOST:81/migrate_auspicious_missing.php"
echo -e "\nDone. API Base: http://$HOST:81"
