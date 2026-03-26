#!/bin/bash
set -e

HOST="43.228.85.200"
USER="root"
PASS="Lydh@58LTG"
BINARY="dist/go-naming-linux-amd64"
SERVICE="go-naming.service"
REMOTE_BIN="/usr/local/bin/go-naming"
REMOTE_SERVICE="/etc/systemd/system/go-naming.service"

echo "Deploying to $USER@$HOST..."

# 0. Stop service (ignore errors if not running)
echo "Stopping service..."
sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "systemctl stop go-naming || true"

# 1. Upload Binary
echo "Uploading binary..."
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no "$BINARY" "$USER@$HOST:$REMOTE_BIN"

# 2. Make executable
echo "Setting executable permissions..."
sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "chmod +x $REMOTE_BIN"

# 3. Upload Service File
echo "Uploading service file..."
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no "$SERVICE" "$USER@$HOST:$REMOTE_SERVICE"

# 4. Reload and Restart
echo "Reloading systemd and restarting service..."
sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "systemctl daemon-reload && systemctl enable go-naming && systemctl restart go-naming"

# 5. Check Status
echo "Checking service status..."
sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "systemctl --no-pager status go-naming"

echo "Deployment complete!"
