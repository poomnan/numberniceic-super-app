#!/bin/bash
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/var/www/html"

# Upload Inspector to PUBLIC folder
echo "Uploading inspect_db.php to public..."
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no inspect_db.php "$USER@$HOST:$REMOTE_DIR/public/"

# Upload NewsController
echo "Uploading NewsController.php..."
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no app/Managers/news/NewsController.php "$USER@$HOST:$REMOTE_DIR/app/Managers/news/"
