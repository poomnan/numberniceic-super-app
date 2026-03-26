#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_BASE="/home/tayap/ananya-php"

echo "🚀 Deploying UserController.php to Server..."
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no app/Managers/UserController.php "$USER@$HOST:$REMOTE_BASE/app/Managers/UserController.php"

if [ $? -eq 0 ]; then
    echo "✅ UserController.php Deployed Successfully!"
else
    echo "❌ Deployment Failed!"
fi
