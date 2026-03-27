#!/bin/bash

# Auto Deploy Go Naming Server Script with sshpass
# Usage: ./deploy_auto.sh

set -e

PASS="Lydh@58LTG"
SERVER="root@numberniceic.online"
LOCAL_DIR="/Users/tayap/project-naming/go-naming"

echo "🚀 Starting automated deployment..."

# Step 1: Build for Linux
echo "📦 Building for Linux..."
cd "$LOCAL_DIR"
mkdir -p build_tmp 
export GOTMPDIR="$LOCAL_DIR/build_tmp"
export GOCACHE="$LOCAL_DIR/build_cache"
mkdir -p "$GOCACHE"
GOOS=linux GOARCH=amd64 go build -o go-naming-app-linux main.go
rm -rf build_tmp

# Step 2: Copy to server
echo "📤 Uploading binary..."
sshpass -p "$PASS" scp -O -o StrictHostKeyChecking=no go-naming-app-linux "$SERVER":/home/tayap/
echo "📤 Uploading templates..."
sshpass -p "$PASS" scp -r -o StrictHostKeyChecking=no templates "$SERVER":/home/tayap/go-naming/

# Step 3: Deploy on server
echo "🔧 Executing remote deployment commands..."
sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no "$SERVER" <<'ENDSSH'
    # Stop any version if running on port 8095
    lsof -ti:8095 2>/dev/null | xargs kill -9 2>/dev/null || true
    
    # Update binary
    mv /home/tayap/go-naming-app-linux /home/tayap/go-naming/server
    chmod +x /home/tayap/go-naming/server
    
    echo "▶️ Restarting go-naming service..."
    sudo systemctl restart go-naming
    
    sleep 3
    
    if systemctl is-active --quiet go-naming; then
        echo "✅ Service is running!"
        sudo systemctl status go-naming --no-pager
    else
        echo "❌ Service failed to start."
        sudo journalctl -u go-naming -n 20 --no-pager
        exit 1
    fi
ENDSSH

echo "🎉 Automated deployment completed successfully!"
