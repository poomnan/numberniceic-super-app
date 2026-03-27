#!/bin/bash
HOST="numberniceic.online"
USER="tayap"
REMOTE_DIR="/var/www/html"

echo "🚀 Deploying with SSH Key..."

scp -o StrictHostKeyChecking=no app/routes.php $USER@$HOST:$REMOTE_DIR/app/routes.php
scp -o StrictHostKeyChecking=no views/web_login.php $USER@$HOST:$REMOTE_DIR/views/web_login.php
scp -o StrictHostKeyChecking=no views/web_register.php $USER@$HOST:$REMOTE_DIR/views/web_register.php
scp -o StrictHostKeyChecking=no views/web_dashboard.php $USER@$HOST:$REMOTE_DIR/views/web_dashboard.php

echo "✅ Login Fix Deployed (SSH Key)."
