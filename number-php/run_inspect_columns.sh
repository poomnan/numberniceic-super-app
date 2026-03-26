#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

# Upload and execute
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no inspect_columns.php "$USER@$HOST:$REMOTE_DIR/"
sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "cd $REMOTE_DIR && php inspect_columns.php"
