#!/bin/bash
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/var/www/html"

echo "Using sshpass to upload NEWS management files..."

# Upload Controller
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no app/Managers/AdminNewsController.php "$USER@$HOST:$REMOTE_DIR/app/Managers/"

# Upload Views
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no views/web_admin_news_list.php "$USER@$HOST:$REMOTE_DIR/views/"
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no views/web_admin_news_form.php "$USER@$HOST:$REMOTE_DIR/views/"
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no views/web_dashboard.php "$USER@$HOST:$REMOTE_DIR/views/"

# Upload Routes
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no app/routes.php "$USER@$HOST:$REMOTE_DIR/app/"
