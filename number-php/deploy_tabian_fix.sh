#!/bin/bash

# Configuration
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

echo "🚀 Starting Tabian Fix deployment..."

# Function to upload
upload_file() {
    local LOCAL_FILE=$1
    local REMOTE_PATH=$2
    echo "📦 Uploading $LOCAL_FILE to $REMOTE_PATH..."
    sshpass -p "$PASS" scp -o StrictHostKeyChecking=no "$LOCAL_FILE" "$USER@$HOST:$REMOTE_PATH"
}

# Upload files
upload_file "app/routes.php" "$REMOTE_DIR/app/"
upload_file "app/Managers/AdminTabianController.php" "$REMOTE_DIR/app/Managers/"

echo "✅ Deployment complete!"
