#!/bin/bash

# Configuration
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/var/www/html"

echo "🚀 Starting Menu Update deployment..."

expect <<EOF
set timeout 300
spawn scp views/web_dashboard.php "$USER@$HOST:$REMOTE_DIR/views/"
expect "password:"
send "$PASS\r"
expect eof
EOF

echo "✅ Deployment complete!"
