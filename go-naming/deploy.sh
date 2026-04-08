#!/bin/bash
set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_IP="numberniceic.online"
SERVER_USER="root"
SERVER_PASSWORD="Lydh@58LTG"
SERVICE_NAME="go-naming"
REMOTE_DIR="/home/tayap/go-naming"
LOCAL_DIR="${LOCAL_DIR:-$SCRIPT_DIR}"
DEPLOY_SSH_HOST="${DEPLOY_SSH_HOST:-$SERVER_IP}"

echo "=== Deploying go-naming backend to $SERVER_IP (SSH: $DEPLOY_SSH_HOST) ==="

# Step 1: Build for Linux
echo "Building for Linux/amd64..."
cd "$LOCAL_DIR"
GOOS=linux GOARCH=amd64 go build -o server-linux main.go

# Step 2: Stop service first (to release binary lock)
echo "Stopping service..."
sshpass -p "$SERVER_PASSWORD" ssh -o StrictHostKeyChecking=no -T \
    "$SERVER_USER@$DEPLOY_SSH_HOST" "systemctl stop $SERVICE_NAME"

# Step 3: Copy new binary to server
echo "Copying new binary..."
sshpass -p "$SERVER_PASSWORD" scp -o StrictHostKeyChecking=no \
    server-linux \
    "$SERVER_USER@$DEPLOY_SSH_HOST:$REMOTE_DIR/server"

# Step 4: Start service with new binary
echo "Starting service..."
sshpass -p "$SERVER_PASSWORD" ssh -o StrictHostKeyChecking=no -T \
    "$SERVER_USER@$DEPLOY_SSH_HOST" "systemctl start $SERVICE_NAME && sleep 2 && systemctl status $SERVICE_NAME --no-pager | head -5"

echo "Deployment completed successfully!"
