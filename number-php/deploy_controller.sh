#!/bin/bash
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_BASE="/var/www/html"

echo "Deploying SacredTempleController..."
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no app/Managers/SacredTempleController.php "$USER@$HOST:$REMOTE_BASE/app/Managers/SacredTempleController.php"

echo "✅ Controller Deployed!"
