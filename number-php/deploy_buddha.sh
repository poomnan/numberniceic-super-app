#!/bin/bash

# Configuration
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

echo "🚀 Starting Buddha feature deployment to $HOST..."

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

echo "📦 Uploading Deployment Tarball..."
upload_file "buddha_deploy.tar.gz" "$REMOTE_DIR/"

echo "📦 Extracting files on remote server..."
run_remote "cd $REMOTE_DIR && tar -xzf buddha_deploy.tar.gz && chmod -R 755 public/uploads/buddha"

echo "🏗️ Running database migration on remote server..."
run_remote "cd $REMOTE_DIR && php migrate_buddha_full.php"

echo "✅ Buddha feature deployment complete!"
