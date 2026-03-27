#!/bin/bash

# Configuration
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/var/www/html"

echo "🚀 Starting deployment (Direct file upload)..."

# Helper function to upload a single file
upload_file() {
    local LOCAL_FILE=$1
    local REMOTE_PATH=$2
    
    echo "📦 Uploading $LOCAL_FILE..."
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

# 1. API Doc View
upload_file "views/web_api_doc_news.php" "$REMOTE_DIR/views/"

# 2. Dashboard View
upload_file "views/web_dashboard.php" "$REMOTE_DIR/views/"

# 3. Routes
upload_file "app/routes.php" "$REMOTE_DIR/app/"

# 4. News Controller
# First ensure directory exists (using php to be safe as mkdir might fail if dir exists, but -p is safer, let's just try upload)
# We can try to create dir via ssh, but let's assume standard structure or try to create it.
echo "📂 Ensuring remote directory exists..."
expect <<EOF
    set timeout 30
    spawn ssh "$USER@$HOST" "mkdir -p $REMOTE_DIR/app/Managers/news"
    expect {
        "yes/no" { send "yes\r"; exp_continue }
        "password:" { send "$PASS\r" }
    }
    expect eof
EOF

upload_file "app/Managers/news/NewsController.php" "$REMOTE_DIR/app/Managers/news/"

echo "✅ Deployment complete!"
