#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

echo "Deploying NewsController..."
export SSHPASS=$PASS
sshpass -e scp -o StrictHostKeyChecking=no app/Managers/news/NewsController.php $USER@$HOST:$REMOTE_DIR/app/Managers/news/NewsController.php
echo "Done."
