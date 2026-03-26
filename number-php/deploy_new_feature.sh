#!/bin/bash

# Configuration
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

echo "🚀 Starting deployment to $HOST..."

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

echo "📦 Uploading new views..."
upload_file "views/web_admin_tabians.php" "$REMOTE_DIR/views/"
upload_file "views/web_admin_tabian_form.php" "$REMOTE_DIR/views/"
upload_file "views/web_dashboard.php" "$REMOTE_DIR/views/"

echo "📦 Uploading updated routes and managers..."
upload_file "app/routes.php" "$REMOTE_DIR/app/"
upload_file "app/Managers/TabianController.php" "$REMOTE_DIR/app/Managers/"

echo "📦 Uploading migration script..."
upload_file "migrate_tabian.php" "$REMOTE_DIR/"

echo "🏗️ Running database migration on server..."
run_remote "cd $REMOTE_DIR && php migrate_tabian.php"

echo "✅ Deployment complete!"
