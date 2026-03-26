#!/bin/bash

# Configuration
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_BASE="/home/tayap/ananya-php"

echo "🚀 Starting Emergency Fix Deployment..."

# 1. Upload Routes (Targeting direct path)
echo "📦 Uploading routes.php..."
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no app/routes.php "$USER@$HOST:$REMOTE_BASE/app/routes.php"

# 2. Upload Controller (Targeting direct path)
echo "📦 Uploading SacredTempleController.php..."
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no app/Managers/SacredTempleController.php "$USER@$HOST:$REMOTE_BASE/app/Managers/SacredTempleController.php"

echo "✅ Deployment of critical files complete!"
