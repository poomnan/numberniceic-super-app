#!/bin/bash
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/var/www/html"

echo "Using sshpass to upload updated view..."

# Upload migration file
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no views/web_admin_article_form.php "$USER@$HOST:$REMOTE_DIR/views/"
