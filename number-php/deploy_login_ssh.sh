#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
REMOTE_DIR="/home/tayap/ananya-php"

echo "🚀 Deploying with SSH Key..."

scp -o StrictHostKeyChecking=no app/routes.php $USER@$HOST:$REMOTE_DIR/app/routes.php
scp -o StrictHostKeyChecking=no views/web_login.php $USER@$HOST:$REMOTE_DIR/views/web_login.php
scp -o StrictHostKeyChecking=no views/web_register.php $USER@$HOST:$REMOTE_DIR/views/web_register.php
scp -o StrictHostKeyChecking=no views/web_dashboard.php $USER@$HOST:$REMOTE_DIR/views/web_dashboard.php

echo "✅ Login Fix Deployed (SSH Key)."
