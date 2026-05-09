# Configuration สำหรับโดเมน ชื่อดี.com
SERVER_IP="43.228.85.200"  # IP สำหรับโดเมน ชื่อดี.com
USER="tayap"
REMOTE_DIR="/home/tayap/go-naming" # Updated to correct service path

echo "🚀 Starting Deployment to ชื่อดี.com ($SERVER_IP)..."

# 0. Cross-Compile for Linux (amd64)
echo "🔨 Building binary for Linux..."
GOOS=linux GOARCH=amd64 go build -o go-naming-linux main.go
if [ $? -ne 0 ]; then
    echo "❌ Build failed! Aborting."
    exit 1
fi
echo "✅ Build successful (go-naming-linux)"

# 1. Sync Files
echo "📂 Syncing files..."
export SSHPASS="IntelliP24.X"

sshpass -e rsync -avz --progress \
    --exclude '.git' \
    --exclude 'go-naming' \
    --exclude 'uploads' \
    --exclude '.DS_Store' \
    ./ $USER@$SERVER_IP:$REMOTE_DIR/

# 2. Remote Commands to Restart
echo "🔧 Restarting Service on Remote..."
sshpass -e ssh -t $USER@$SERVER_IP "bash -c '
    cd $REMOTE_DIR
    
    # Rename uploaded binary to target name
    # Systemd expects the binary to be named 'server' based on error logs
    mv go-naming-linux server
    chmod +x server

    # Attempt Restart (Systemd or Manual)
    if systemctl list-units --full -all | grep -Fq \"go-naming.service\"; then
        # Try sudo if needed, but for now try direct
        echo \"IntelliP24.X\" | sudo -S systemctl restart go-naming
        echo \"✅ Service restarted via systemd.\"
    else
        echo \"⚠️ Service not found in systemd. Restarting manually...\"
        pkill -f go-naming || true
        pkill -f server || true
        # Start with ENV from file if exists, or basic defaults.
        export DATABASE_URL=\"postgres://tayap:IntelliP24.X@localhost/tayap?sslmode=disable\"
        nohup ./server > server.log 2>&1 &
        echo \"✅ Manual start triggered (PID: \$!).\"
    fi
'"

echo "✨ Deployment Finished!"
