#!/bin/bash

# Configuration
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/var/www/html"

echo "🚀 Starting Sacred Temple feature deployment via sshpass..."

# Check if sshpass is installed
if ! command -v sshpass &> /dev/null; then
    echo "Error: sshpass is not installed."
    exit 1
fi

# 1. Upload Files
echo "📦 Uploading Files..."
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no \
    app/Managers/SacredTempleController.php \
    app/routes.php \
    views/admin_temple_list.php \
    views/admin_temple_form.php \
    create_temple_table.php \
    create_temple_assign_table.php \
    "$USER@$HOST:$REMOTE_DIR/"

# 2. Move files to correct directories
echo "nb Moving files..."
sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no "$USER@$HOST" <<EOF
    mv $REMOTE_DIR/SacredTempleController.php $REMOTE_DIR/app/Managers/
    mv $REMOTE_DIR/admin_temple_list.php $REMOTE_DIR/views/
    mv $REMOTE_DIR/admin_temple_form.php $REMOTE_DIR/views/
    mv $REMOTE_DIR/routes.php $REMOTE_DIR/app/
EOF

# 3. Create Table
echo "🗄️ Creating Database Table..."
sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "cd $REMOTE_DIR && php create_temple_table.php && php create_temple_assign_table.php"

echo "✅ Temple feature deployment complete!"
