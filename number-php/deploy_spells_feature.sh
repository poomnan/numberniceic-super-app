#!/bin/bash
HOST="numberniceic.online"
USER="tayap"
REMOTE_DIR="/var/www/html"

echo "🚀 Deploying Spells Feature..."

# Upload files
scp -o StrictHostKeyChecking=no app/Managers/AdminSpellController.php $USER@$HOST:$REMOTE_DIR/app/Managers/
scp -o StrictHostKeyChecking=no views/web_admin_spell_form.php $USER@$HOST:$REMOTE_DIR/views/
scp -o StrictHostKeyChecking=no views/web_admin_spell_list.php $USER@$HOST:$REMOTE_DIR/views/
scp -o StrictHostKeyChecking=no migrate_spells_photo.php $USER@$HOST:$REMOTE_DIR/

echo "📦 Files Uploaded."

# Run Migration and Setup Dir
echo "⚙️ Running Migration and Setup..."
ssh -o StrictHostKeyChecking=no $USER@$HOST "cd $REMOTE_DIR && php migrate_spells_photo.php && mkdir -p public/uploads/spells && chmod 777 public/uploads/spells"

echo "✅ Spells Feature Deployed."
