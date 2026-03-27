#!/bin/bash
# Configuration
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/var/www/html"

echo "🚀 Deploying Landing Page Fix..."

upload_file() {
    FILE=$1
    DEST=$2
    echo "📦 Uploading $FILE..."
    sshpass -p "$PASS" scp -o StrictHostKeyChecking=no "$FILE" "$USER@$HOST:$DEST"
}

upload_file "app/routes.php" "$REMOTE_DIR/app/"
upload_file "app/Managers/HomePageManager.php" "$REMOTE_DIR/app/Managers/"
upload_file "views/web_index.php" "$REMOTE_DIR/views/"
upload_file "views/web_menu.php" "$REMOTE_DIR/views/"

echo "✅ Deployment Attempted."
