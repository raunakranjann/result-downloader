#!/bin/bash

# 1. Navigate to the app directory dynamically
cd "$(dirname "$0")"

# --- ADDED: Playwright Path Fix ---
# Points to the folder containing 'chrome-linux'
export PLAYWRIGHT_BROWSERS_PATH="./browsers/linux"
# ----------------------------------

# 2. Mutex Check: Check if Port 2006 is already in use
if lsof -Pi :2006 -sTCP:LISTEN -t >/dev/null ; then
    echo "Academic Analytics is already running. Opening UI..."
    xdg-open http://localhost:2006/
    exit 0
fi

# 3. Run the Java app in the background
# 'nohup' prevents the app from closing when the terminal shuts down
nohup ./jre/bin/java -jar mainapplication.jar > /dev/null 2>&1 &

# 4. Wait for the server to boot
sleep 8

# 5. Open the UI as a standalone applet
# Added the bundled chrome path as the first priority
./browsers/linux/chrome-linux/chrome --app=http://localhost:2006/ || \
google-chrome --app=http://localhost:2006/ || \
microsoft-edge --app=http://localhost:2006/ || \
xdg-open http://localhost:2006/