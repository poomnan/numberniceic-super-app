#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

# Upload and Run, save output to file on server, then read file
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no inspect_db.php "$USER@$HOST:$REMOTE_DIR/"
sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "cd $REMOTE_DIR && php inspect_db.php > db_info.txt"
sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "cat $REMOTE_DIR/db_info.txt"
