#!/bin/bash
# Script to run Go project (ananya-go)

PROJECT_PATH="../../apps-go/ananya-go"
GOROOT_BIN="/Users/tayap/goroot1.22.6/bin"

echo "Changing directory to $PROJECT_PATH..."
cd "$PROJECT_PATH" || { echo "Error: Directory not found"; exit 1; }

# Add Go to PATH
export PATH=$PATH:$GOROOT_BIN

echo "Starting Go application..."
go run main.go
