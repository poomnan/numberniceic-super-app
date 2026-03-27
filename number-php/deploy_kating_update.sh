#!/bin/bash
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/var/www/html"

echo "🚀 Deploying Kating Updates..."

upload_file() {
    FILE=$1
    DEST=$2
    echo "📦 Uploading $FILE..."
    sshpass -p "$PASS" scp -o StrictHostKeyChecking=no "$FILE" "$USER@$HOST:$DEST"
}

# 1. Update UserController (contains is_kating logic)
upload_file "app/Managers/UserController.php" "$REMOTE_DIR/app/Managers/"

# 2. Upload Helper Scripts
upload_file "add_kating_day.php" "$REMOTE_DIR/"
upload_file "check_latest_kating.php" "$REMOTE_DIR/"

echo "✅ Deployment Complete."
echo "You can now SSH into the server and run 'php add_kating_day.php YYYY-MM-DD' to update data."
