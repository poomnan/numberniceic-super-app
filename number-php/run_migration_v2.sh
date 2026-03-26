#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

echo "Using sshpass to upload and run migration V2..."
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no migrate_news_table_v2.php "$USER@$HOST:$REMOTE_DIR/"
sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "cd $REMOTE_DIR && php migrate_news_table_v2.php"
