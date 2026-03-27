#!/bin/bash
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/var/www/html"

echo "Using sshpass to upload fixed NewsController (Using 'news' table)..."
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no app/Managers/news/NewsController.php "$USER@$HOST:$REMOTE_DIR/app/Managers/news/"
