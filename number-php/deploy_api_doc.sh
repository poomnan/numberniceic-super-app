#!/bin/bash

# Configuration
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/var/www/html"

echo "🚀 Starting deployment of API Docs..."

# Function to run scp with expect
upload_file() {
    local LOCAL_FILE=$1
    local REMOTE_PATH=$2
    
    expect <<EOF
    set timeout 120
    spawn scp "$LOCAL_FILE" "$USER@$HOST:$REMOTE_PATH"
    expect {
        "yes/no" { send "yes\r"; exp_continue }
        "password:" { send "$PASS\r" }
    }
    expect eof
EOF
}

# Function to run remote commands
run_remote() {
    local CMD=$1
    
    expect <<EOF
    set timeout 120
    spawn ssh "$USER@$HOST" "$CMD"
    expect {
        "yes/no" { send "yes\r"; exp_continue }
        "password:" { send "$PASS\r" }
    }
    expect eof
EOF
}

# Ensure directory exists
echo "📂 Checking remote directories..."
run_remote "mkdir -p $REMOTE_DIR/app/Managers/news"

echo "📦 Uploading API Documentation view..."
upload_file "views/web_api_doc_news.php" "$REMOTE_DIR/views/"

echo "📦 Uploading updated Dashboard..."
upload_file "views/web_dashboard.php" "$REMOTE_DIR/views/"

echo "📦 Uploading updated Routes..."
upload_file "app/routes.php" "$REMOTE_DIR/app/"

echo "📦 Uploading updated NewsController..."
upload_file "app/Managers/news/NewsController.php" "$REMOTE_DIR/app/Managers/news/"

echo "✅ Deployment complete!"
