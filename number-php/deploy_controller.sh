#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_BASE="/home/tayap/ananya-php"

echo "Deploying SacredTempleController..."
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no app/Managers/SacredTempleController.php "$USER@$HOST:$REMOTE_BASE/app/Managers/SacredTempleController.php"

echo "✅ Controller Deployed!"
