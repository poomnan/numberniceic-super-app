#!/bin/bash

# Configuration
HOST="43.228.85.200"
USER="tayap"
PASS="IntelliP24.X"
REMOTE_DIR="/home/tayap/ananya-php"

echo "🚀 Deploying Check Script..."

# Function to upload
upload_file() {
    local LOCAL_FILE=$1
    local REMOTE_PATH=$2
    echo "📦 Uploading $LOCAL_FILE..."
    expect <<EOF
    set timeout 120
    spawn scp "$LOCAL_FILE" "$USER@$HOST:$REMOTE_PATH"
    expect {
        "yes/no" { send "yes\r"; exp_continue }
        "password:" { send "$PASS\r" }
    }
    expect eof
EOF
}

# 1. Reset check_user_data.php to use localhost (for server execution)
echo "<?php
// DB Credentials
\$host = \"localhost\";
\$user = \"zoqlszwh_ananyadb\";
\$pass = \"IntelliP24.X\";
\$dbname = \"zoqlszwh_ananyadb\";

try {
    \$pdo = new PDO(\"mysql:host=\$host;dbname=\$dbname;charset=utf8\", \$user, \$pass);
    \$pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    \$pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_OBJ);
} catch (PDOException \$e) {
    die(\"Connection failed: \" . \$e->getMessage());
}

// Search for users
\$stmt = \$pdo->prepare(\"SELECT * FROM membertb WHERE realname LIKE '%น้ำหอม%' OR username LIKE '%admin%'\");
\$stmt->execute();
\$users = \$stmt->fetchAll();

echo \"Found \" . count(\$users) . \" users:\\n\";

foreach (\$users as \$u) {
    echo \"--------------------------------------------------\\n\";
    echo \"ID: \" . \$u->id . \"\\n\";
    echo \"MemberID: \" . \$u->memberid . \"\\n\";
    echo \"RealName: \" . \$u->realname . \"\\n\";
    
    // Check Buddha Assign
    \$stmtB = \$pdo->prepare(\"SELECT b.*, a.assignment_type FROM buddha_pang_tb b 
                           JOIN user_buddha_assign a ON b.id = a.buddha_id 
                           WHERE a.memberid = :mid\");
    \$stmtB->execute(['mid' => \$u->memberid]);
    \$buddhas = \$stmtB->fetchAll();
    echo \"Buddha Assignments: \" . count(\$buddhas) . \"\\n\";
    foreach (\$buddhas as \$b) {
        echo \" - Type: \" . \$b->assignment_type . \", Name: \" . \$b->pang_name . \"\\n\";
    }

    // Check Temple Assign
    \$stmtT = \$pdo->prepare(\"SELECT t.* FROM sacred_temple_tb t 
                           JOIN user_temple_assign a ON t.id = a.temple_id 
                           WHERE a.memberid = :mid\");
    \$stmtT->execute(['mid' => \$u->memberid]);
    \$temples = \$stmtT->fetchAll();
    echo \"Temple Assignments: \" . count(\$temples) . \"\\n\";
    foreach (\$temples as \$t) {
        echo \" - Name: \" . \$t->temple_name . \"\\n\";
    }
}
echo \"--------------------------------------------------\\n\";
?>" > check_user_data.php

# 2. Upload Assignment Controllers (Fix Logic)
upload_file "app/Managers/BuddhaPangController.php" "$REMOTE_DIR/app/Managers/"
upload_file "app/Managers/SacredTempleController.php" "$REMOTE_DIR/app/Managers/"

# 3. Upload Check Script
upload_file "check_user_data.php" "$REMOTE_DIR/"

echo "✅ Scripts Uploaded!"

# 4. Execute Check Script
echo "Running check script on server..."
expect <<EOF
    set timeout 120
    spawn ssh "$USER@$HOST" "php $REMOTE_DIR/check_user_data.php"
    expect {
        "yes/no" { send "yes\r"; exp_continue }
        "password:" { send "$PASS\r" }
    }
    expect eof
EOF
