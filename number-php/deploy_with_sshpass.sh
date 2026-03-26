#!/bin/bash

# Configuration
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

echo "🚀 Starting deployment using sshpass..."

# Function to upload
upload_file() {
    local LOCAL_FILE=$1
    local REMOTE_PATH=$2
    echo "📦 Uploading $LOCAL_FILE..."
    sshpass -p "$PASS" scp -o StrictHostKeyChecking=no "$LOCAL_FILE" "$USER@$HOST:$REMOTE_PATH"
}

# Function to run command
run_remote() {
    local CMD=$1
    echo "🔧 Running remote: $CMD"
    sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$CMD"
}

# Ensure directory
run_remote "mkdir -p $REMOTE_DIR/app/Managers/news"

# Upload files
upload_file "views/web_api_doc_news.php" "$REMOTE_DIR/views/"
upload_file "views/web_dashboard.php" "$REMOTE_DIR/views/"
upload_file "app/routes.php" "$REMOTE_DIR/app/"
upload_file "app/Managers/news/NewsController.php" "$REMOTE_DIR/app/Managers/news/"

echo "✅ Deployment complete!"
