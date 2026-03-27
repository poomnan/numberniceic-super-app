#!/bin/bash
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/var/www/html"

echo "Using sshpass + rsync to upload NewsController..."
sshpass -p "$PASS" rsync -av -e "ssh -o StrictHostKeyChecking=no" app/Managers/news/NewsController.php "$USER@$HOST:$REMOTE_DIR/app/Managers/news/"
