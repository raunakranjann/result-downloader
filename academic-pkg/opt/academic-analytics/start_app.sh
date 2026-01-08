#!/bin/bash
# 1. Navigate to the app directory
cd /opt/academic-analytics

# 2. Run the Java app in the background
./jre/bin/java -jar beu-bulk-result-downlaoder-0.0.1-SNAPSHOT.jar &

# 3. Give the server a few seconds to start
sleep 4

# 4. Open the UI as a standalone applet (using Edge or Chrome if available)
microsoft-edge --app=http://localhost:2006/ || google-chrome --app=http://localhost:2006/ || xdg-open http://localhost:2006/
