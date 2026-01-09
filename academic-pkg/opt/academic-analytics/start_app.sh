#!/bin/bash

# 1. Navigate to the app directory dynamically and store it
APP_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$APP_DIR"

# 2. Setup Data Directory Safety
mkdir -p "$HOME/AcademicAnalytics"

# 3. Playwright Environment Configuration
export PLAYWRIGHT_BROWSERS_PATH="$APP_DIR/browsers/linux"
export PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

# 4. Mutex Check: Fix for Re-opening the App
if lsof -Pi :2006 -sTCP:LISTEN -t >/dev/null ; then
    echo "Academic Analytics is already running. Forcing Applet view..."
    # FIX: Use the bundled chrome with --app instead of xdg-open
    "$APP_DIR/browsers/linux/chrome-linux/chrome" --no-sandbox --app=http://localhost:2006/
    exit 0
fi

# 5. Run the Java app in the background
nohup "$APP_DIR/jre/bin/java" -jar "$APP_DIR/mainapplication.jar" > /dev/null 2>&1 &

# 6. Optimized Boot Wait: Check for the port, don't just sleep
echo "Starting server..."
for i in {1..30}; do
    if lsof -Pi :2006 -sTCP:LISTEN -t >/dev/null ; then
        echo "Server is UP."
        break
    fi
    sleep 1
done

# 7. Final Launch: Priority Applet Mode
# Using the absolute path to your bundled binary prevents system hijack
"$APP_DIR/browsers/linux/chrome-linux/chrome" --no-sandbox --app=http://localhost:2006/ || \
google-chrome --no-sandbox --app=http://localhost:2006/ || \
xdg-open http://localhost:2006/