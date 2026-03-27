#!/bin/bash
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/var/www/html"

upload_file() {
    FILE=$1
    DEST=$2
    echo "📦 Uploading $FILE..."
    sshpass -p "$PASS" scp -o StrictHostKeyChecking=no "$FILE" "$USER@$HOST:$DEST"
}

upload_file "app/routes.php" "$REMOTE_DIR/app/"
upload_file "views/web_login.php" "$REMOTE_DIR/views/"
upload_file "views/web_register.php" "$REMOTE_DIR/views/"

echo "✅ Login Fix Deployed."
