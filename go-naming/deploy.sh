#!/bin/bash
set -e

# Configuration สำหรับโดเมน ชื่อดี.com
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_IP="43.228.85.200"  # IP สำหรับโดเมน ชื่อดี.com
SERVER_USER="root"
SERVER_PASSWORD="Lydh@58LTG"
SERVICE_NAME="go-naming"
REMOTE_DIR="/home/tayap/go-naming"
LOCAL_DIR="${LOCAL_DIR:-$SCRIPT_DIR}"
DEPLOY_SSH_HOST="${DEPLOY_SSH_HOST:-$SERVER_IP}"

echo "=== Deploying go-naming backend to ชื่อดี.com ($SERVER_IP) (SSH: $DEPLOY_SSH_HOST) ==="

# Step 1: Build for Linux
echo "Building for Linux/amd64..."
cd "$LOCAL_DIR"
GOOS=linux GOARCH=amd64 go build -o server-linux main.go

# Step 2: Copy new binary to remote server as a temporary file
echo "Copying new binary to temporary path on server..."
sshpass -p "$SERVER_PASSWORD" scp -o StrictHostKeyChecking=no -o PreferredAuthentications=password \
    server-linux \
    "$SERVER_USER@$DEPLOY_SSH_HOST:$REMOTE_DIR/server-linux-tmp"

# Step 3: Replace binary and restart service immediately (Downtime: < 0.1s)
echo "Replacing binary and restarting service..."
sshpass -p "$SERVER_PASSWORD" ssh -o StrictHostKeyChecking=no -o PreferredAuthentications=password -T \
    "$SERVER_USER@$DEPLOY_SSH_HOST" "systemctl stop $SERVICE_NAME && mv -f $REMOTE_DIR/server-linux-tmp $REMOTE_DIR/server && chmod +x $REMOTE_DIR/server && systemctl start $SERVICE_NAME && sleep 2 && systemctl status $SERVICE_NAME --no-pager | head -5"

echo "Deployment completed successfully with near-zero downtime!"
