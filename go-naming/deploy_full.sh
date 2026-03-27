#!/bin/bash
set -e

REMOTE_HOST="numberniceic.online"
REMOTE_USER="root"
REMOTE_PASS='Lydh@58LTG'
REMOTE_DIR="/home/tayap/go-naming"

echo "🔨 Building binary for Linux..."
GOOS=linux GOARCH=amd64 go build -o go-naming-linux main.go

echo "📦 Packaging assets..."
mkdir -p deploy_pkg/templates
cp go-naming-linux deploy_pkg/server_new
cp /Users/tayap/project-naming/naming-landing/index.html deploy_pkg/templates/
cp /Users/tayap/project-naming/naming-landing/styles.css deploy_pkg/templates/
cp /Users/tayap/project-naming/naming-landing/app_screen.png deploy_pkg/templates/
tar -czf deploy_pkg.tar.gz -C deploy_pkg .

echo "📂 Uploading package..."
sshpass -p "$REMOTE_PASS" scp -o StrictHostKeyChecking=no deploy_pkg.tar.gz "$REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR/"
sshpass -p "$REMOTE_PASS" scp -o StrictHostKeyChecking=no /Users/tayap/project-naming/go-naming/go-naming.service.remote "$REMOTE_USER@$REMOTE_HOST:/etc/systemd/system/go-naming.service"

echo "🔧 Extracting and Restarting on Remote..."
sshpass -p "$REMOTE_PASS" ssh -o StrictHostKeyChecking=no "$REMOTE_USER@$REMOTE_HOST" "bash -c '
    cd $REMOTE_DIR
    tar -xzf deploy_pkg.tar.gz
    
    # Backup old server inside project dir if exists, then replace
    [ -f server ] && mv server server.bak
    mv server_new server
    
    # Set permissions
    chown tayap:tayap server
    chmod +x server
    chown -R tayap:tayap templates/
    rm deploy_pkg.tar.gz
    
    # Reload and restart service
    echo \"Reloading systemd and restarting go-naming service...\"
    systemctl daemon-reload
    systemctl restart go-naming
    
    # Check status
    systemctl is-active --quiet go-naming && echo \"✅ Service is running\" || echo \"❌ Service failed to start\"
    systemctl status go-naming --no-pager | grep \"ExecStart\"
'"

# Cleanup
rm -rf deploy_pkg deploy_pkg.tar.gz go-naming-linux

echo "✨ Deployment Finished!"
