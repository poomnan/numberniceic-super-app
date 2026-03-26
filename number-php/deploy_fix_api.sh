#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

echo "Using sshpass to upload fixed NewsController (Using 'news' table)..."
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no app/Managers/news/NewsController.php "$USER@$HOST:$REMOTE_DIR/app/Managers/news/"
