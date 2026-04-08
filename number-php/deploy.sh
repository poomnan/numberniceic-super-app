#!/bin/bash

# Deploy script for number-php project to production server
# Server IP: 43.228.85.200
# User: root
# Password: Lydh@58LTG

SERVER="43.228.85.200"
USER="root"
PASSWORD="Lydh@58LTG"
REMOTE_PATH="/var/www/html"
LOCAL_PATH="/Users/tayap/Numberniceic-Super-Apps/number-php"
DEPLOY_SSH_HOST="${DEPLOY_SSH_HOST:-$SERVER}"

echo "Starting deployment to $SERVER (SSH: $DEPLOY_SSH_HOST)..."
echo "Local path: $LOCAL_PATH"
echo "Remote path: $REMOTE_PATH"
echo ""

# Sync files to server
echo "Syncing files to server..."
sshpass -p "$PASSWORD" rsync -avz --delete \
  --exclude='vendor/' \
  --exclude='.git/' \
  --exclude='*.log' \
  --exclude='backup_*' \
  --exclude='cache/' \
  --exclude='node_modules/' \
  $LOCAL_PATH/ $USER@$DEPLOY_SSH_HOST:$REMOTE_PATH/

# Restart web server
echo "Restarting web server..."
sshpass -p "$PASSWORD" ssh $USER@$DEPLOY_SSH_HOST "systemctl restart nginx"

echo ""
echo "Deployment process finished."
