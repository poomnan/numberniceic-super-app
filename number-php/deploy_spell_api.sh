#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
REMOTE_DIR="/home/tayap/ananya-php"

echo "🚀 Deploying Spell API..."

# Upload files
scp -o StrictHostKeyChecking=no app/Managers/SpellAPIController.php $USER@$HOST:$REMOTE_DIR/app/Managers/
scp -o StrictHostKeyChecking=no app/routes.php $USER@$HOST:$REMOTE_DIR/app/

echo "✅ Spell API Deployed."
