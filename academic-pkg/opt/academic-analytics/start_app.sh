#!/bin/bash

# 1. Navigate to the app directory dynamically
cd "$(dirname "$0")"

# 2. Mutex Check: Check if Port 2006 is already in use
# This prevents launching multiple instances and crashing the port
if lsof -Pi :2006 -sTCP:LISTEN -t >/dev/null ; then
    echo "Academic Analytics is already running. Opening UI..."
    xdg-open http://localhost:2006/
    exit 0
fi

# 3. Run the Java app in the background
# Using nohup ensures the app keeps running if the terminal is closed
nohup ./jre/bin/java -jar mainapplication.jar > /dev/null 2>&1 &

# 4. Wait for the server to boot
# Increased to 7 seconds to ensure the Spring Context is ready
sleep 7

# 5. Open the UI as a standalone applet
# xdg-open is the safest cross-distro command for Linux
google-chrome --app=http://localhost:2006/ || microsoft-edge --app=http://localhost:2006/ || xdg-open http://localhost:2006/