#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

echo "========================================="
echo "   ACADEMIC ANALYTICS - LINUX LAUNCHER"
echo "========================================="

# 1. CHECK IF DOCKER IS RUNNING
# We try 'docker info'. If it fails, Docker is off or user has no permissions.
if ! sudo docker info > /dev/null 2>&1; then
  echo "CRITICAL ERROR: Docker is not running or not installed."
  echo "Please start Docker and try again."
  echo "-----------------------------------------------------"
  read -p "Press Enter to exit..."
  exit 1
fi

echo "Docker is running. Starting setup..."

# 2. CLEANUP OLD SESSIONS (Fixes Conflict Error)
echo "Cleaning up old sessions..."
sudo docker rm -f academic_db academic_app > /dev/null 2>&1

# 3. ENSURE PERMISSIONS
chmod +x stop_app.sh

# 4. START FRESH
echo "Building and Starting Containers..."
sudo docker compose up -d --build

# 5. OPEN BROWSER
echo "App started! Opening Browser..."
sleep 5
xdg-open http://localhost:2006