#!/bin/bash
HOST="numberniceic.online"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/var/www/html"

sshpass -p "$PASS" scp -o StrictHostKeyChecking=no views/web_dashboard.php "$USER@$HOST:$REMOTE_DIR/views/web_dashboard.php"
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no views/web_admin_merits.php "$USER@$HOST:$REMOTE_DIR/views/web_admin_merits.php"
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no views/web_admin_merits_form.php "$USER@$HOST:$REMOTE_DIR/views/web_admin_merits_form.php"
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no views/tambon/dynamic.phtml "$USER@$HOST:$REMOTE_DIR/views/tambon/dynamic.phtml"
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no app/routes.php "$USER@$HOST:$REMOTE_DIR/app/routes.php"
sshpass -p "$PASS" scp -o StrictHostKeyChecking=no migrate_tambon_to_db.php "$USER@$HOST:$REMOTE_DIR/migrate_tambon_to_db.php"

sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "cd $REMOTE_DIR && php migrate_tambon_to_db.php"
