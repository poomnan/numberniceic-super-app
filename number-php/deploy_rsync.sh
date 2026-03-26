#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

echo "Using sshpass + rsync to upload NewsController..."
sshpass -p "$PASS" rsync -av -e "ssh -o StrictHostKeyChecking=no" app/Managers/news/NewsController.php "$USER@$HOST:$REMOTE_DIR/app/Managers/news/"
