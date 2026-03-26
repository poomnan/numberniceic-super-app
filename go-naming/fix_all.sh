#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"

echo "🚀 Starting FIX via SCP..."

# 1. Build
echo "📦 Building..."
mkdir -p ./.go-build ./.go-cache
export GOCACHE=$(pwd)/.go-cache
export GOTMPDIR=$(pwd)/.go-build
export GOOS=linux
export GOARCH=amd64

go build -o go-naming-app-linux main.go
go build -o update-vectors-linux debug_tools/update_vectors.go

# 2. Upload (Use scp, ignore failures slightly to proceed)
echo "📤 Uploading..."
# Try to upload to unique filenames
TS=$(date +%s)
APP_TARGET="go-naming-app.$TS"
TOOL_TARGET="update-vectors-linux.$TS"

sshpass -p "$PASS" scp -o StrictHostKeyChecking=no go-naming-app-linux $USER@$HOST:/home/tayap/$APP_TARGET
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no update-vectors-linux $USER@$HOST:/home/tayap/$TOOL_TARGET
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no -r templates $USER@$HOST:/home/tayap/

# 3. Swap & Restart
echo "🔥 Swapping & Restarting..."
COMMANDs="
   echo '$PASS' | sudo -S pkill -9 go-naming-app; 
   echo '$PASS' | sudo -S mv /home/tayap/$APP_TARGET /home/tayap/go-naming/server; 
   echo '$PASS' | sudo -S chmod +x /home/tayap/go-naming/server;
   echo '$PASS' | sudo -S systemctl restart go-naming;
   
   pkill -f update-vectors-linux;
   mv /home/tayap/$TOOL_TARGET /home/tayap/update-vectors-linux;
   chmod +x /home/tayap/update-vectors-linux;
   nohup /home/tayap/update-vectors-linux > update.log 2>&1 &
"

sshpass -p "$PASS" ssh -t -o StrictHostKeyChecking=no $USER@$HOST "$COMMANDs"

echo "✅ DONE."
