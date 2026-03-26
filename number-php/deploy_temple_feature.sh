#!/bin/bash

# Configuration
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

echo "🚀 Starting Sacred Temple feature deployment..."

# 1. Upload Files
echo "📦 Uploading Files..."
expect <<EOF
set timeout 300
spawn scp \
    app/Managers/SacredTempleController.php \
    app/routes.php \
    views/admin_temple_list.php \
    views/admin_temple_form.php \
    create_temple_table.php \
    create_temple_assign_table.php \
    "$USER@$HOST:$REMOTE_DIR/"
expect "password:"
send "$PASS\r"
expect eof
EOF

# 2. Move files to correct directories (routes and create_table are already in root/app relative properly? No, routes is in app/, controller in app/Managers)
# The scp command above puts everything in $REMOTE_DIR/root or relative?
# scp sources... dest
# If source is app/Managers/X, and dest is $REMOTE_DIR/, it puts X in $REMOTE_DIR/X usually? No, scp puts filename in dest.
# So "app/Managers/SacredTempleController.php" -> "$REMOTE_DIR/" result in "$REMOTE_DIR/SacredTempleController.php"
# I need to move them.

echo "nb Moving files to correct structure..."
expect <<EOF
set timeout 300
spawn ssh "$USER@$HOST"
expect "password:"
send "$PASS\r"
expect "$ "
send "mv $REMOTE_DIR/SacredTempleController.php $REMOTE_DIR/app/Managers/\r"
expect "$ "
send "mv $REMOTE_DIR/admin_temple_list.php $REMOTE_DIR/views/\r"
expect "$ "
send "mv $REMOTE_DIR/admin_temple_form.php $REMOTE_DIR/views/\r"
expect "$ "
send "mv $REMOTE_DIR/routes.php $REMOTE_DIR/app/\r"
expect "$ "
send "exit\r"
expect eof
EOF

# 3. Create Table
echo "🗄️ Creating Database Table..."
expect <<EOF
set timeout 300
spawn ssh "$USER@$HOST" "cd $REMOTE_DIR && php create_temple_table.php && php create_temple_assign_table.php"
expect "password:"
send "$PASS\r"
expect eof
EOF

echo "✅ Temple feature deployment complete!"
