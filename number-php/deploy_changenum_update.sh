#!/bin/bash

# Configuration
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

echo "🚀 Starting deployment for changenum views..."

# Check if sshpass is installed
if ! command -v sshpass &> /dev/null; then
    echo "Error: sshpass is not installed."
    exit 1
fi

export SSHPASS="IntelliP24.X"

# Files to upload
FILES=(
    "views/changenum/home.phtml"
    "views/changenum/namenick.phtml"
    "views/changenum/namesur.phtml"
    "views/changenum/tabian.phtml"
    "views/changenum/phone.phtml"
)

for FILE in "${FILES[@]}"; do
    if [ -f "$FILE" ]; then
        echo "📦 Uploading $FILE..."
        sshpass -e scp -o StrictHostKeyChecking=no "$FILE" "$USER@$HOST:$REMOTE_DIR/views/changenum/"
    else
        echo "⚠️ File not found: $FILE"
    fi
done

echo "✅ Deployment for changenum views complete!"
