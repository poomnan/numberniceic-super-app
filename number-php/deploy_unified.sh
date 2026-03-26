#!/bin/bash

# Configuration
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

echo "🚀 Starting Buddha feature deployment (Unified)..."

expect <<EOF
set timeout 300
spawn ssh "$USER@$HOST"
expect "password:"
send "$PASS\r"
expect "$ "
send "mkdir -p $REMOTE_DIR/app/Managers $REMOTE_DIR/views $REMOTE_DIR/public/uploads/buddha\r"
expect "$ "
send "exit\r"
expect eof
EOF

# Use scp for multiple files at once if possible
echo "📦 Uploading Files..."
expect <<EOF
set timeout 300
spawn scp \
    app/Managers/BuddhaPangController.php \
    app/routes.php \
    migrate_buddha_full.php \
    "$USER@$HOST:$REMOTE_DIR/"
expect "password:"
send "$PASS\r"
expect eof
EOF

expect <<EOF
set timeout 300
spawn scp \
    views/admin_buddha_list.php \
    views/admin_buddha_form.php \
    views/web_dashboard.php \
    "$USER@$HOST:$REMOTE_DIR/views/"
expect "password:"
send "$PASS\r"
expect eof
EOF

expect <<EOF
set timeout 300
spawn scp \
    public/uploads/buddha/mon.png \
    public/uploads/buddha/tue.png \
    "$USER@$HOST:$REMOTE_DIR/public/uploads/buddha/"
expect "password:"
send "$PASS\r"
expect eof
EOF

echo "🏗️ Moving Controller..."
expect <<EOF
set timeout 300
spawn ssh "$USER@$HOST" "mv $REMOTE_DIR/BuddhaPangController.php $REMOTE_DIR/app/Managers/"
expect "password:"
send "$PASS\r"
expect eof
EOF

echo "🏗️ Running database migration..."
expect <<EOF
set timeout 300
spawn ssh "$USER@$HOST" "cd $REMOTE_DIR && php migrate_buddha_full.php"
expect "password:"
send "$PASS\r"
expect eof
EOF

echo "✅ Buddha feature deployment complete!"
