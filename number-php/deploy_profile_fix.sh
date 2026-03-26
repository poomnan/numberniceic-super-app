#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

echo "🚀 Deploying User Profile Update Fix..."

export SSHPASS="$PASS"

# Upload Zip
echo "📦 Uploading zip..."
sshpass -e scp -o StrictHostKeyChecking=no deploy_user_profile_update.zip "$USER@$HOST:$REMOTE_DIR/"

# Unzip on Server
echo "📂 Unzipping on server..."
sshpass -e ssh -o StrictHostKeyChecking=no "$USER@$HOST" "cd $REMOTE_DIR && unzip -o deploy_user_profile_update.zip"

echo "✅ Deployment complete!"
