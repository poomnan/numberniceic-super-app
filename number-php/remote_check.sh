#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

echo "Checking remote data..."
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no server_check_data.php $USER@$HOST:$REMOTE_DIR/server_check_data.php
sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no $USER@$HOST "php $REMOTE_DIR/server_check_data.php"
