#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

# Upload
export SSHPASS="$PASS"
sshpass -e scp -o StrictHostKeyChecking=no check_april_db.php "$USER@$HOST:$REMOTE_DIR/public/"

# Check
curl -s "https://ananya.in.th/check_april_db.php"
