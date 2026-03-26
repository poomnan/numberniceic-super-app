#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

# Upload
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no debug_news_schema.php "$USER@$HOST:$REMOTE_DIR/"

# Run with -T to avoid PTY issues and capture stdout
sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no -T "$USER@$HOST" "cd $REMOTE_DIR && php debug_news_schema.php"
