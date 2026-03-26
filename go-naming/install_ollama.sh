#!/bin/bash
set -e

echo "Installing Ollama..."
curl -fsSL https://ollama.com/install.sh | sh

echo "Starting Ollama service..."
systemctl enable ollama
systemctl start ollama

echo "Pulling nomic-embed-text model (this may take a few minutes)..."
ollama pull nomic-embed-text

echo "Ollama installed and model ready."
echo "To use Ollama for embeddings, set OLLAMA_URL=http://localhost:11434 in go-naming service."
