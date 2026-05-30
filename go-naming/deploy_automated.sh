#!/bin/bash
# Configuration
SERVER_IP="43.228.85.200"
USER="root"
PASS="Lydh@58LTG"
REMOTE_DIR="/home/tayap/go-naming"

echo "🚀 Starting High-Speed Deployment to $SERVER_IP..."

# 0. Cross-Compile for Linux (amd64)
echo "🔨 Building binary for Linux..."
GOOS=linux GOARCH=amd64 go build -o go-naming-linux main.go
if [ $? -ne 0 ]; then
    echo "❌ Build failed! Aborting."
    exit 1
fi
echo "✅ Build successful (go-naming-linux)"

# 1. Sync Files using sshpass
echo "📂 Syncing files..."
sshpass -p "$PASS" rsync -avz --progress \
    --exclude '.git' \
    --exclude 'go-naming' \
    --exclude 'uploads' \
    --exclude '.DS_Store' \
    ./ $USER@$SERVER_IP:$REMOTE_DIR/

# 2. Remote Commands using sshpass
echo "🔧 Restarting Service on Remote..."
sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no $USER@$SERVER_IP "bash -c '
    cd $REMOTE_DIR
    
    # Target binary name
    mv go-naming-linux server
    chmod +x server

    # Attempt Restart
    if systemctl list-units --full -all | grep -Fq \"go-naming.service\"; then
        systemctl restart go-naming
        echo \"✅ Service restarted via systemd.\"
    else
        echo \"⚠️ Service not found. Manual restart...\"
        pkill -f go-naming || true
        export DATABASE_URL=\"postgres://tayap:IntelliP24.X@localhost/tayap?sslmode=disable\"
        nohup ./server > server.log 2>&1 &
        echo \"✅ Manual start triggered.\"
    fi
'"

echo "✨ Deployment Finished Successfully!"
