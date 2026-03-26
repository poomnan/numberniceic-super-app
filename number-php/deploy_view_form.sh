#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

echo "Using sshpass to upload updated view..."

# Upload migration file
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no views/web_admin_article_form.php "$USER@$HOST:$REMOTE_DIR/views/"
