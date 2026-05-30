#!/bin/bash
# Naming App Deployment Script v2
# Usage: ./deploy_v2.sh

set -e

SERVER_IP="43.228.85.200"
USER="root"
PASS="Lydh@58LTG"
REMOTE_DIR="/home/tayap/go-naming"

echo "🚀 Starting Deployment to $SERVER_IP as $USER..."

# 1. Build for Linux
echo "📦 Building for Linux (amd64)..."
GOOS=linux GOARCH=amd64 go build -o server-linux main.go
echo "✅ Build successful: server-linux"

# 2. Upload to Server
echo "📤 Uploading binary and templates..."
export SSHPASS=$PASS
# Upload binary to temp location
sshpass -e scp -o StrictHostKeyChecking=no server-linux $USER@$SERVER_IP:/tmp/server-linux-new
# Sync templates
sshpass -e rsync -avz -e "ssh -o StrictHostKeyChecking=no" --progress templates/ $USER@$SERVER_IP:$REMOTE_DIR/templates/

# 3. Finalize on Server
echo "🔧 Setting permissions and restarting service..."
sshpass -e ssh -o StrictHostKeyChecking=no $USER@$SERVER_IP "bash -c '
    # Stop service first
    systemctl stop go-naming || true
    
    # Move binary from temp
    mv /tmp/server-linux-new $REMOTE_DIR/server
    chmod +x $REMOTE_DIR/server
    chown -R tayap:tayap $REMOTE_DIR
    
    echo \"▶️ Restarting go-naming service...\"
    systemctl restart go-naming
    
    sleep 2
    if systemctl is-active --quiet go-naming; then
        echo \"✅ Service is running!\"
        systemctl status go-naming --no-pager
    else
        echo \"❌ Service failed to start.\"
        journalctl -u go-naming -n 20 --no-pager
        exit 1
    fi
'"

echo "🎉 Deployment completed successfully!"
