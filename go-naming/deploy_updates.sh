#!/bin/bash
set -e

echo "🔨 Building binary for Linux..."
GOOS=linux GOARCH=amd64 go build -o go-naming-linux main.go

echo "📂 Uploading binary..."
sshpass -p 'Lydh@58LTG' scp -o StrictHostKeyChecking=no go-naming-linux root@43.228.85.200:/home/tayap/go-naming/server_new

echo "🔧 Update and Restart on Remote..."
sshpass -p 'Lydh@58LTG' ssh -o StrictHostKeyChecking=no root@43.228.85.200 "bash -c '
    cd /home/tayap/go-naming
    mv server_new server
    chown tayap:tayap server
    chmod +x server
    
    # Restart service
    echo \"Restarting go-naming service...\"
    systemctl restart go-naming
    
    # Check status
    systemctl is-active --quiet go-naming && echo \"✅ Service is running\" || echo \"❌ Service failed to start\"
'"
echo "✨ Deployment Finished!"
