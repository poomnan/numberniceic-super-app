#!/bin/bash
# Configuration
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

# Use sshpass WITHOUT strict host checking and WITHOUT allocating a pty (-T not needed for scp, but for ssh we might need simply to avoid it)

echo "🚀 Deploying without PTY allocation..."

upload_file() {
    FILE=$1
    DEST=$2
    echo "📦 Uploading $FILE..."
    # sshpass works by intercepting the tty, but here we run without tty.
    # We will try a different approach: standard ssh with password in command line is tricky.
    # But we have sshpass installed.
    sshpass -p "$PASS" scp -o StrictHostKeyChecking=no "$FILE" "$USER@$HOST:$DEST"
}

# 1. Upload Routes & Controllers
upload_file "app/routes.php" "$REMOTE_DIR/app/"
upload_file "app/Managers/BuddhaPangController.php" "$REMOTE_DIR/app/Managers/"
upload_file "app/Managers/SacredTempleController.php" "$REMOTE_DIR/app/Managers/"
upload_file "check_user_data.php" "$REMOTE_DIR/"
upload_file "app/Managers/news/NewsController.php" "$REMOTE_DIR/app/Managers/news/"
upload_file "debug_news_count.php" "$REMOTE_DIR/"
upload_file "debug_buddha_18.php" "$REMOTE_DIR/"
upload_file "app/Managers/AdminSpellController.php" "$REMOTE_DIR/app/Managers/"
upload_file "views/web_admin_spell_list.php" "$REMOTE_DIR/views/"
upload_file "views/web_admin_spell_form.php" "$REMOTE_DIR/views/"
upload_file "views/web_menu.php" "$REMOTE_DIR/views/"
upload_file "migrate_spells.php" "$REMOTE_DIR/"

echo "✅ Scripts Uploaded. Executing Check & Migration..."

# 2. Execute Check & Migration
sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "php $REMOTE_DIR/check_user_data.php && php $REMOTE_DIR/migrate_spells.php"
