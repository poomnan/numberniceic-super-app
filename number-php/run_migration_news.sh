#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

echo "Using sshpass to upload and run migration (NEWS TABLE)..."

# Upload migration file
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no migrate_news_table_correct.php "$USER@$HOST:$REMOTE_DIR/"

# Run migration
sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "cd $REMOTE_DIR && php migrate_news_table_correct.php"
