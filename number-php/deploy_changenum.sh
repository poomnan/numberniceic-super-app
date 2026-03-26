#!/bin/bash
# Configuration
REMOTE_USER="root"
REMOTE_HOST="43.228.85.200"
REMOTE_PASS="Lydh@58LTG"
REMOTE_PATH="/var/www/html"

# Files to upload
FILES=(
    "app/routes.php"
    "views/web_dashboard.php"
    "views/web_admin_changenum.php"
    "views/web_admin_changenum_form.php"
    "migrate_changenum_to_db.php"
)

for file in "${FILES[@]}"; do
    echo "Uploading $file..."
    sshpass -p "$REMOTE_PASS" scp -o StrictHostKeyChecking=no "$file" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_PATH/$file"
done

# Run migration
echo "Running migration on server..."
sshpass -p "$REMOTE_PASS" ssh -o StrictHostKeyChecking=no "$REMOTE_USER@$REMOTE_HOST" "php $REMOTE_PATH/migrate_changenum_to_db.php"

# Clear Opcache
echo "Clearing Opcache..."
curl -s https://numberniceic.online/api/opcache/clear

echo "Deployment of Change Procedures completed!"
