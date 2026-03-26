#!/bin/bash

# Configuration
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

echo "🚀 Starting Menu Update deployment..."

expect <<EOF
set timeout 300
spawn scp views/web_dashboard.php "$USER@$HOST:$REMOTE_DIR/views/"
expect "password:"
send "$PASS\r"
expect eof
EOF

echo "✅ Deployment complete!"
