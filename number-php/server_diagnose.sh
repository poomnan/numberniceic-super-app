#!/bin/bash

echo "========================================"
echo " SERVER DIAGNOSTIC TOOL"
echo "========================================"

echo "\n[1] Checking OS & Kernel..."
uname -a
cat /etc/os-release

echo "\n[2] Checking Web Server..."
if command -v nginx &> /dev/null; then
    echo "Nginx found. Version:"
    nginx -v
    echo "\nNginx Configs:"
    ls -l /etc/nginx/sites-enabled/
    echo "\nActive Site Config (First one found):"
    head -n 50 /etc/nginx/sites-enabled/* | head -n 50
elif command -v apache2 &> /dev/null; then
    echo "Apache found."
    apache2 -v
else
    echo "Web server not identified (standard paths)."
fi

echo "\n[3] Checking PID/User of Web process..."
ps aux | grep -E 'www-data|nginx|apache' | head -n 5

echo "\n[4] Checking Project Permissions..."
TARGET_DIR="/home/tayap/ananya-php"
if [ -d "$TARGET_DIR" ]; then
    echo "Project dir exists: $TARGET_DIR"
    ls -ld $TARGET_DIR
    echo "\nSub-directories:"
    ls -l $TARGET_DIR | grep "^d"
else
    echo "CRITICAL: Project directory $TARGET_DIR not found!"
fi

echo "\n[5] Checking PHP Version..."
php -v

echo "\n========================================"
echo " DIAGNOSIS COMPLETE"
echo "========================================"
