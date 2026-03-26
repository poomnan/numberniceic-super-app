#!/bin/bash

# Notification System Deployment Script
# Deploys new notification system files to production server

set -e  # Exit on error

echo "🚀 Deploying Notification System..."
echo "===================================="

# Configuration (adjust these)
SERVER_USER="your_username"
SERVER_HOST="ananya.in.th"
SERVER_PATH="/path/to/number-php"
DB_NAME="your_db_name"
DB_USER="your_db_user"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Step 1: Upload new Manager files
echo -e "${YELLOW}📤 Step 1: Uploading new Manager files...${NC}"
scp app/Managers/NotificationManager.php ${SERVER_USER}@${SERVER_HOST}:${SERVER_PATH}/app/Managers/
scp app/Managers/NotificationAPIController.php ${SERVER_USER}@${SERVER_HOST}:${SERVER_PATH}/app/Managers/
echo -e "${GREEN}✅ Manager files uploaded${NC}"

# Step 2: Upload modified NotificationController
echo -e "${YELLOW}📤 Step 2: Uploading modified NotificationController...${NC}"
scp app/Managers/NotificationController.php ${SERVER_USER}@${SERVER_HOST}:${SERVER_PATH}/app/Managers/
echo -e "${GREEN}✅ NotificationController uploaded${NC}"

# Step 3: Upload routes.php
echo -e "${YELLOW}📤 Step 3: Uploading routes.php...${NC}"
scp app/routes.php ${SERVER_USER}@${SERVER_HOST}:${SERVER_PATH}/app/
echo -e "${GREEN}✅ routes.php uploaded${NC}"

# Step 4: Upload SQL migration
echo -e "${YELLOW}📤 Step 4: Uploading database migration...${NC}"
scp create_notifications_table.sql ${SERVER_USER}@${SERVER_HOST}:${SERVER_PATH}/
echo -e "${GREEN}✅ SQL file uploaded${NC}"

# Step 5: Run database migration
echo -e "${YELLOW}🗄️  Step 5: Running database migration...${NC}"
echo "Please run the following command on the server:"
echo ""
echo -e "${YELLOW}ssh ${SERVER_USER}@${SERVER_HOST}${NC}"
echo -e "${YELLOW}cd ${SERVER_PATH}${NC}"
echo -e "${YELLOW}psql -U ${DB_USER} -d ${DB_NAME} -f create_notifications_table.sql${NC}"
echo ""
read -p "Press Enter after you've run the migration on the server..."

# Step 6: Test API endpoints
echo -e "${YELLOW}🧪 Step 6: Testing API endpoints...${NC}"
echo "Testing GET /api/v2/notifications..."
curl -s "https://${SERVER_HOST}/api/v2/notifications?memberid=TEST001&type=webview_merit" | jq '.' || echo "jq not installed, showing raw response"

echo ""
echo -e "${GREEN}✅ Deployment Complete!${NC}"
echo ""
echo "Next steps:"
echo "1. Test all API endpoints manually"
echo "2. Implement Android client changes (see NOTIFICATION_SYSTEM_IMPLEMENTATION.md)"
echo "3. Deploy Android app"
echo ""
echo "API Endpoints:"
echo "  GET  /api/v2/notifications"
echo "  GET  /api/v2/notifications/by-type"
echo "  POST /api/v2/notifications/mark-read"
echo "  GET  /api/v2/notifications/unread-count"
echo "  GET  /api/v2/notifications/statistics"
echo ""
