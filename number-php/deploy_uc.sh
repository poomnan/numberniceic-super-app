#!/bin/bash
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/var/www/html"

echo "🚀 Deploying UserController Logic Fix..."

export SSHPASS="$PASS"

# Upload Zip
echo "📦 Uploading zip..."
sshpass -e scp -o StrictHostKeyChecking=no deploy_user_controller.zip "$USER@$HOST:$REMOTE_DIR/"

# Unzip on Server
echo "📂 Unzipping on server..."
sshpass -e ssh -o StrictHostKeyChecking=no "$USER@$HOST" "cd $REMOTE_DIR && unzip -o deploy_user_controller.zip"

echo "✅ Deployment complete!"
