#!/bin/bash
# Sync local log to server every 10 seconds
while true; do
    scp /Users/tayap/project-naming/go-naming/debug_tools/classify_gender_ai.log tayap@43.228.85.200:~/go-naming/debug_tools/classify_gender_ai.log > /dev/null 2>&1
    sleep 10
done
