#!/bin/bash

# 1. Get the Real Username (because we are running as sudo)
REAL_USER=$SUDO_USER
if [ -z "$REAL_USER" ]; then
  echo "Error: Please run this script using 'sudo ./install.sh'"
  exit 1
fi

# 2. Define Paths (CHANGED to use User's Home Directory)
APP_NAME="Academic Analytics"
INSTALL_DIR="/home/$REAL_USER/AcademicAnalytics_App"
ICON_SOURCE="./src/main/resources/static/icon.png"

echo "Installing $APP_NAME to $INSTALL_DIR..."

# 3. Copy Files
mkdir -p "$INSTALL_DIR"
# Copy files excluding junk
rsync -av --progress . "$INSTALL_DIR" --exclude .git --exclude target --exclude .idea

# 4. FIX PERMISSIONS (Crucial Step!)
# We give ownership of the folder to the real user so Docker can write to it
chown -R $REAL_USER:$REAL_USER "$INSTALL_DIR"
chmod +x "$INSTALL_DIR/start_app.sh"
chmod +x "$INSTALL_DIR/stop_app.sh"

# 5. Create the 'Start App' Menu Shortcut
echo "Creating Menu Shortcut..."
cat > /usr/share/applications/academic-analytics.desktop <<EOL
[Desktop Entry]
Version=1.0
Type=Application
Name=$APP_NAME
Comment=Start the Academic Analytics System
Exec=$INSTALL_DIR/start_app.sh
Icon=$INSTALL_DIR/src/main/resources/static/icon.png
Terminal=true
Categories=Education;Development;
EOL

# 6. Create the 'Uninstall' Menu Shortcut
cat > /usr/share/applications/academic-uninstall.desktop <<EOL
[Desktop Entry]
Version=1.0
Type=Application
Name=Uninstall $APP_NAME
Comment=Remove Academic Analytics and Data
Exec=$INSTALL_DIR/uninstall.sh
Icon=user-trash
Terminal=true
Categories=System;
EOL

# 7. Create the Uninstall Script
cat > "$INSTALL_DIR/uninstall.sh" <<EOL
#!/bin/bash
echo "Are you sure you want to uninstall Academic Analytics?"
echo "This will DELETE ALL DATABASE DATA in $INSTALL_DIR."
read -p "Type 'yes' to confirm: " confirm

if [ "\$confirm" = "yes" ]; then
    echo "Stopping containers..."
    cd "$INSTALL_DIR"
    sudo docker compose down --rmi all -v
    
    echo "Removing files..."
    sudo rm /usr/share/applications/academic-analytics.desktop
    sudo rm /usr/share/applications/academic-uninstall.desktop
    sudo rm -rf "$INSTALL_DIR"
    
    echo "Uninstallation Complete."
    sleep 3
else
    echo "Cancelled."
    sleep 2
fi
EOL

# Make uninstaller executable
chmod +x "$INSTALL_DIR/uninstall.sh"

echo "========================================="
echo "   Installation Complete!"
echo "   You can now find '$APP_NAME' in your menu."
echo "========================================="