#!/bin/bash

# Deploy script for number-php project to production server
# Server: numberniceic.online
# User: root
# Password: Lydh@58LTG

SERVER="numberniceic.online"
USER="root"
PASSWORD="Lydh@58LTG"
REMOTE_PATH="/var/www/html"
LOCAL_PATH="/Users/tayap/project-naming/number-php"
DEPLOY_SSH_HOST="${DEPLOY_SSH_HOST:-$SERVER}"

echo "🚀 Starting deployment to $SERVER (SSH: $DEPLOY_SSH_HOST)..."
echo "Local path: $LOCAL_PATH"
echo "Remote path: $REMOTE_PATH"
echo ""

# Create backup on server
echo "📦 Creating backup on server..."
sshpass -p "$PASSWORD" ssh $USER@$DEPLOY_SSH_HOST "cd $REMOTE_PATH && tar -czf backup_$(date +%Y%m%d_%H%M%S).tar.gz --exclude='backup_*.tar.gz' ."

# Sync files to server
echo "📤 Syncing files to server..."
sshpass -p "$PASSWORD" rsync -avz --delete \
  --exclude='vendor/' \
  --exclude='.git/' \
  --exclude='*.log' \
  --exclude='backup_*' \
  --exclude='cache/' \
  --exclude='node_modules/' \
  $LOCAL_PATH/ $USER@$DEPLOY_SSH_HOST:$REMOTE_PATH/

# Install/update dependencies on server
echo "📦 Installing dependencies on server..."
sshpass -p "$PASSWORD" ssh $USER@$DEPLOY_SSH_HOST "cd $REMOTE_PATH && composer install --no-dev --optimize-autoloader"

# Set proper permissions
echo "🔐 Setting permissions..."
sshpass -p "$PASSWORD" ssh $USER@$DEPLOY_SSH_HOST "cd $REMOTE_PATH && chown -R www-data:www-data . && chmod -R 755 ."

# Add address column if not exists
echo "🗄️ Adding address column if not exists..."
sshpass -p "$PASSWORD" ssh $USER@$DEPLOY_SSH_HOST "cd $REMOTE_PATH && php add_address_column.php"

# Restart web server
echo "🔄 Restarting web server..."
  sshpass -p "$PASSWORD" ssh $USER@$DEPLOY_SSH_HOST "systemctl restart nginx"

echo ""
echo "✅ Deployment completed successfully!"
echo "🌐 Your application is now available at: http://$SERVER"
echo ""
echo "📋 Summary of changes:"
echo "  - Added address column to membertb table"
echo "  - Updated UserController.php to handle address field"
echo "  - Deployed all files to production server"
echo "  - Restarted web server"
