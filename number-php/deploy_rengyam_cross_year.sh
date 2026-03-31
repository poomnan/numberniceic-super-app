#!/bin/bash
set -euo pipefail

HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE="/var/www/html"
SSH_OPTS=(-o ConnectTimeout=10 -o StrictHostKeyChecking=no)
TS="$(date +%Y%m%d_%H%M%S)"
BACKUP="$REMOTE/_codex_backup/${TS}_rengyam_cross_year"
STAGING="/home/$USER/codex_rengyam_cross_year_${TS}"

remote() {
    sshpass -p "$PASS" ssh "${SSH_OPTS[@]}" "$USER@$HOST" "$1"
}

remote_root() {
    local cmd="$1"
    local quoted_cmd
    quoted_cmd=$(printf '%q' "$cmd")
    sshpass -p "$PASS" ssh "${SSH_OPTS[@]}" "$USER@$HOST" "printf '%s\n' '$PASS' | sudo -S bash -lc $quoted_cmd"
}

upload() {
    local source_file="$1"
    local dest_file="$2"
    sshpass -p "$PASS" scp "${SSH_OPTS[@]}" "$source_file" "$USER@$HOST:$dest_file"
}

echo "Creating remote backup at $BACKUP"
remote_root "set -e; \
    mkdir -p '$BACKUP/app/Managers/userx'; \
    cp '$REMOTE/app/Managers/ThaiCalendarHelper.php' '$BACKUP/app/Managers/ThaiCalendarHelper.php'; \
    cp '$REMOTE/app/Managers/UserController.php' '$BACKUP/app/Managers/UserController.php'; \
    cp '$REMOTE/app/Managers/userx/UserController.php' '$BACKUP/app/Managers/userx/UserController.php'; \
    if [ -f '$REMOTE/populate_auspicious.php' ]; then cp '$REMOTE/populate_auspicious.php' '$BACKUP/populate_auspicious.php'; fi; \
    if [ -f '$REMOTE/import_kating_days.php' ]; then cp '$REMOTE/import_kating_days.php' '$BACKUP/import_kating_days.php'; fi; \
    if [ -f '$REMOTE/audit_rengyam_consistency.php' ]; then cp '$REMOTE/audit_rengyam_consistency.php' '$BACKUP/audit_rengyam_consistency.php'; fi"

echo "Uploading backend files"
remote "mkdir -p '$STAGING/app/Managers/userx'"
upload "number-php/app/Managers/ThaiCalendarHelper.php" "$STAGING/app/Managers/ThaiCalendarHelper.php"
upload "number-php/app/Managers/UserController.php" "$STAGING/app/Managers/UserController.php"
upload "number-php/app/Managers/userx/UserController.php" "$STAGING/app/Managers/userx/UserController.php"
upload "number-php/populate_auspicious.php" "$STAGING/populate_auspicious.php"
upload "number-php/import_kating_days.php" "$STAGING/import_kating_days.php"
upload "number-php/audit_rengyam_consistency.php" "$STAGING/audit_rengyam_consistency.php"

echo "Installing files into $REMOTE"
remote_root "set -e; \
    cp '$STAGING/app/Managers/ThaiCalendarHelper.php' '$REMOTE/app/Managers/ThaiCalendarHelper.php'; \
    cp '$STAGING/app/Managers/UserController.php' '$REMOTE/app/Managers/UserController.php'; \
    cp '$STAGING/app/Managers/userx/UserController.php' '$REMOTE/app/Managers/userx/UserController.php'; \
    cp '$STAGING/populate_auspicious.php' '$REMOTE/populate_auspicious.php'; \
    cp '$STAGING/import_kating_days.php' '$REMOTE/import_kating_days.php'; \
    cp '$STAGING/audit_rengyam_consistency.php' '$REMOTE/audit_rengyam_consistency.php'"

echo "Running remote sync and audit"
remote_root "set -e; cd '$REMOTE'; php populate_auspicious.php; php import_kating_days.php; php audit_rengyam_consistency.php"

echo "Clearing opcache"
curl -s "https://numberniceic.online/api/opcache/clear"
echo

echo "Checking live API for 2027 dates"
curl -s "https://numberniceic.online/member/lengyam" | grep -Eo '2027-09-10|2027-12-31' || true

echo "Cleaning staging files"
remote "rm -rf '$STAGING'"
