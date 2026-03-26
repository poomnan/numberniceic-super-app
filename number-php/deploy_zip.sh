#!/bin/bash

# Configuration
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

echo "🚀 Starting deployment (Zip method)..."

# Upload Zip
expect <<EOF
    set timeout 300
    spawn scp "deploy_pack.zip" "$USER@$HOST:$REMOTE_DIR/"
    expect {
        "yes/no" { send "yes\r"; exp_continue }
        "password:" { send "$PASS\r" }
    }
    expect eof
EOF

# Unzip on remote
echo "📦 Unzipping on server..."
expect <<EOF
    set timeout 120
    spawn ssh "$USER@$HOST" "cd $REMOTE_DIR && unzip -o deploy_pack.zip && rm deploy_pack.zip"
    expect {
        "yes/no" { send "yes\r"; exp_continue }
        "password:" { send "$PASS\r" }
    }
    expect eof
EOF

echo "✅ Deployment complete!"
