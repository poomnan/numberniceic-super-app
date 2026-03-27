#!/bin/bash
HOST="numberniceic.online"
USER="tayap"
REMOTE_DIR="/var/www/html"

echo "🚀 Deploying Spell API..."

# Upload files
scp -o StrictHostKeyChecking=no app/Managers/SpellAPIController.php $USER@$HOST:$REMOTE_DIR/app/Managers/
scp -o StrictHostKeyChecking=no app/routes.php $USER@$HOST:$REMOTE_DIR/app/

echo "✅ Spell API Deployed."
