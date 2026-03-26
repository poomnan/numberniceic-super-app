#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
export SSHPASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

# Upload SQL and PHP script
sshpass -e scp -o StrictHostKeyChecking=no restore_news.sql "$USER@$HOST:$REMOTE_DIR/"
sshpass -e scp -o StrictHostKeyChecking=no restore_remote_db.php "$USER@$HOST:$REMOTE_DIR/"
sshpass -e scp -o StrictHostKeyChecking=no app/Managers/news/NewsController.php "$USER@$HOST:$REMOTE_DIR/app/Managers/news/"

# Execute restore script (using -T to disable TTY)
sshpass -e ssh -o StrictHostKeyChecking=no -T "$USER@$HOST" "cd $REMOTE_DIR && php restore_remote_db.php"
