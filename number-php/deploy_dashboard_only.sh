#!/bin/bash
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/var/www/html/views/"

echo "Uploading web_dashboard.php..."
expect <<EOF
set timeout 60
spawn scp "views/web_dashboard.php" "$USER@$HOST:$REMOTE_DIR"
expect {
  "yes/no" { send "yes\r"; exp_continue }
  "password:" { send "$PASS\r" }
}
expect eof
EOF
