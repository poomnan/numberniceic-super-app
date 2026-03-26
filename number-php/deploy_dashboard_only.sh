#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php/views/"

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
