#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
REMOTE_DIR="/home/tayap/ananya-php"

echo "🚀 Starting Interactive Deployment..."
echo "You will be asked for the password for each file. This is normal."

scp -o StrictHostKeyChecking=no app/routes.php $USER@$HOST:$REMOTE_DIR/app/routes.php
scp -o StrictHostKeyChecking=no views/web_login.php $USER@$HOST:$REMOTE_DIR/views/web_login.php
scp -o StrictHostKeyChecking=no views/web_register.php $USER@$HOST:$REMOTE_DIR/views/web_register.php

echo "✅ Deployment Complete!"
