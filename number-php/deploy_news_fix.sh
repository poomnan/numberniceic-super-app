#!/bin/bash
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/var/www/html"

echo "Deploying NewsController..."
export SSHPASS=$PASS
sshpass -e scp -o StrictHostKeyChecking=no app/Managers/news/NewsController.php $USER@$HOST:$REMOTE_DIR/app/Managers/news/NewsController.php
echo "Done."
