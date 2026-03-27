#!/bin/bash
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_TEMP_DIR="/var/www/html"
REMOTE_PROD_DIR="/var/www/html"

echo "1. Uploading modified files to temp directory..."
sshpass -p "$PASS" rsync -av -e "ssh -o StrictHostKeyChecking=no" app/Managers/InauspiciousController.php "$USER@$HOST:$REMOTE_TEMP_DIR/app/Managers/"
sshpass -p "$PASS" rsync -av -e "ssh -o StrictHostKeyChecking=no" app/Managers/AuspiciousController.php "$USER@$HOST:$REMOTE_TEMP_DIR/app/Managers/"
sshpass -p "$PASS" rsync -av -e "ssh -o StrictHostKeyChecking=no" app/Managers/UserController.php "$USER@$HOST:$REMOTE_TEMP_DIR/app/Managers/"
sshpass -p "$PASS" rsync -av -e "ssh -o StrictHostKeyChecking=no" app/Managers/NotificationController.php "$USER@$HOST:$REMOTE_TEMP_DIR/app/Managers/"
sshpass -p "$PASS" rsync -av -e "ssh -o StrictHostKeyChecking=no" app/routes.php "$USER@$HOST:$REMOTE_TEMP_DIR/app/"

echo "2. Copying files to production directory (/var/www/html) using sudo..."
sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no "$USER@$HOST" << EOF
echo "$PASS" | sudo -S cp $REMOTE_TEMP_DIR/app/Managers/InauspiciousController.php $REMOTE_PROD_DIR/app/Managers/
echo "$PASS" | sudo -S cp $REMOTE_TEMP_DIR/app/Managers/AuspiciousController.php $REMOTE_PROD_DIR/app/Managers/
echo "$PASS" | sudo -S cp $REMOTE_TEMP_DIR/app/Managers/UserController.php $REMOTE_PROD_DIR/app/Managers/
echo "$PASS" | sudo -S cp $REMOTE_TEMP_DIR/app/Managers/NotificationController.php $REMOTE_PROD_DIR/app/Managers/
echo "$PASS" | sudo -S cp $REMOTE_TEMP_DIR/app/routes.php $REMOTE_PROD_DIR/app/
echo "$PASS" | sudo -S chown www-data:www-data $REMOTE_PROD_DIR/app/Managers/InauspiciousController.php
echo "$PASS" | sudo -S chown www-data:www-data $REMOTE_PROD_DIR/app/Managers/AuspiciousController.php
echo "$PASS" | sudo -S chown www-data:www-data $REMOTE_PROD_DIR/app/Managers/UserController.php
echo "$PASS" | sudo -S chown www-data:www-data $REMOTE_PROD_DIR/app/Managers/NotificationController.php
echo "$PASS" | sudo -S chown www-data:www-data $REMOTE_PROD_DIR/app/routes.php
EOF

echo "3. Clearing OPCache..."
curl -s "https://numberniceic.online/api/opcache/clear"

echo "✅ Production Deployment Complete!"
