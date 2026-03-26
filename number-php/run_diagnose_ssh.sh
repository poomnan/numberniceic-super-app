#!/bin/bash
HOST="43.228.85.200"
USER="tayap"
# SSH Key is assumed to be setup based on previous success

echo "🚀 Sending Diagnostic Script to Server..."
scp -o StrictHostKeyChecking=no server_diagnose.sh $USER@$HOST:/home/tayap/server_diagnose.sh

echo "🚀 Running Diagnostic Script..."
ssh -o StrictHostKeyChecking=no $USER@$HOST "chmod +x /home/tayap/server_diagnose.sh && /home/tayap/server_diagnose.sh"

echo "✅ Done."
