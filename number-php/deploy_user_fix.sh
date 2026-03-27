#!/bin/bash
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_BASE="/var/www/html"

echo "🚀 Deploying UserController.php to Server..."
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no app/Managers/UserController.php "$USER@$HOST:$REMOTE_BASE/app/Managers/UserController.php"

if [ $? -eq 0 ]; then
    echo "✅ UserController.php Deployed Successfully!"
else
    echo "❌ Deployment Failed!"
fi
