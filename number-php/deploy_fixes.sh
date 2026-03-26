#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_BASE="/home/tayap/ananya-php"

echo "🚀 Deploying Fixes for Merit, Temple, and Buddha..."

FILES=(
    "app/Managers/BuddhaPangController.php"
    "app/Managers/SacredTempleController.php"
    "app/Managers/MeritController.php"
    "app/routes.php"
)

for FILE in "${FILES[@]}"; do
    if [ -f "$FILE" ]; then
        echo "📦 Uploading $FILE..."
        sshpass -p "$PASS" scp -o StrictHostKeyChecking=no "$FILE" "$USER@$HOST:$REMOTE_BASE/$FILE"
    else
        echo "⚠️ File not found: $FILE"
    fi
done

echo "⚙️ Running Migration for Merit Table..."
# Trigger the migration route via curl on the server (locally on server) or via public URL
# We assume the server is accessible via public URL or we run php command.
# Let's try running a PHP one-liner on the server to trigger DB exec?
# Or just rely on the route.
# Here we just curl the route if domain is known, but using SSH to run PHP is safer.

sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "cd $REMOTE_BASE && php -r \"require 'vendor/autoload.php'; \$settings = require 'configs/config.php'; \$db = new PDO('mysql:host='.\$settings['db']['host'].';dbname='.\$settings['db']['dbname'], \$settings['db']['user'], \$settings['db']['pass']); \$db->exec('CREATE TABLE IF NOT EXISTS user_merit_assign (id INT AUTO_INCREMENT PRIMARY KEY, memberid VARCHAR(50) NOT NULL, merit_type VARCHAR(50) DEFAULT \'webview_merit\', title VARCHAR(255), body TEXT, url TEXT, assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, KEY (memberid)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;'); echo 'Migration executed.';\""

echo "✅ Deployment and Migration Complete!"
