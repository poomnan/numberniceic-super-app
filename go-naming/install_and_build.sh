#!/bin/bash
# Script to install Go and build the application on server
# Run this on the server: bash install_and_build.sh

set -e

echo "🚀 Starting Go installation and build process..."

# 1. Install Go
echo "📦 Installing Go..."
sudo apt update
sudo apt install golang-go -y

# Verify installation
go version

# 2. Navigate to project directory
echo "📂 Navigating to project directory..."
cd ~/go-naming

# 3. Download dependencies
echo "📥 Downloading dependencies..."
go mod tidy

# 4. Build the application
echo "🔨 Building application..."
GOOS=linux GOARCH=amd64 go build -o go-naming-app-linux main.go

# 5. Make executable
chmod +x go-naming-app-linux

# 6. Restart service
echo "🔄 Restarting service..."
sudo systemctl restart go-naming

# 7. Wait a bit
sleep 3

# 8. Check status
echo "✅ Checking service status..."
sudo systemctl status go-naming --no-pager

# 9. Show logs
echo ""
echo "📝 Recent logs:"
sudo journalctl -u go-naming -n 20 --no-pager

echo ""
echo "🎉 Build and deployment completed!"
echo "📝 To view live logs: sudo journalctl -u go-naming -f"
