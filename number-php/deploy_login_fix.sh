#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

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
