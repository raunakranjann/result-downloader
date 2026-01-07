#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

echo "Stopping Academic Analytics System..."

# Stop containers and remove networks (Data stays safe)
sudo docker compose down

echo "System Stopped Successfully."
sleep 2