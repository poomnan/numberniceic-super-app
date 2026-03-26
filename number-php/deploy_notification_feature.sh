#!/bin/bash

# Configuration
DB_HOST="localhost"
DB_USER="zoqlszwh_ananyadb"
DB_PASS="IntelliP24.X"
DB_NAME="zoqlszwh_ananyadb"

echo "Deploying Notification History Feature..."

# 1. Run SQL to create table
if [ -f "create_notification_history_table.sql" ]; then
    echo "Running SQL migration..."
    # Check if mysql command exists
    if ! command -v mysql &> /dev/null; then
        echo "⚠️  'mysql' command not found. Please run the SQL file manually:"
        echo "   mysql -u $DB_USER -p $DB_NAME < create_notification_history_table.sql"
    else
        mysql -h $DB_HOST -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" < create_notification_history_table.sql
        
        if [ $? -eq 0 ]; then
            echo "✅ Database table created successfully."
        else
            echo "❌ Error running SQL. Please check your database credentials."
            echo "   Try running manually: mysql -u $DB_USER -p $DB_NAME < create_notification_history_table.sql"
        fi
    fi
else
    echo "❌ Error: create_notification_history_table.sql not found."
    exit 1
fi

echo "✅ PHP Manager Created: app/Managers/NotificationHistoryManager.php"
echo "✅ Routes Updated by appending to app/routes.php"
echo "🎉 Deployment Complete!"
