#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

echo "Installing/Starting Academic Analytics..."

# --- CLEANUP STEP (Fixes the Conflict Error) ---
echo "Cleaning up old sessions..."
sudo docker rm -f academic_db academic_app > /dev/null 2>&1
# -----------------------------------------------

# Ensure permissions
chmod +x stop_app.sh

# Start fresh
sudo docker compose up -d --build

echo "App started! Opening Browser..."
sleep 5
xdg-open http://localhost:2006