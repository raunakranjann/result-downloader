#!/bin/bash

# 1. Get the Real Username (because we are running as sudo)
REAL_USER=$SUDO_USER
if [ -z "$REAL_USER" ]; then
  echo "Error: Please run this script using 'sudo ./install.sh'"
  exit 1
fi

# 2. Define Paths
APP_NAME="Academic Analytics"
# The folder containing your actual code (relative to this script)
SOURCE_CODE_DIR="Academic_Installer_Source"
INSTALL_DIR="/home/$REAL_USER/AcademicAnalytics_App"

# --- SAFETY CHECK: Ensure the source folder exists ---
if [ ! -d "$SOURCE_CODE_DIR" ]; then
  echo "CRITICAL ERROR: Folder '$SOURCE_CODE_DIR' not found!"
  echo "Make sure install.sh is next to the 'Academic_Installer_Source' folder."
  exit 1
fi

# Note: This assumes your icon is exactly here inside the source folder
ICON_PATH="$INSTALL_DIR/src/main/resources/static/icon.png"

echo "Installing $APP_NAME..."
echo "Source: $SOURCE_CODE_DIR"
echo "Dest  : $INSTALL_DIR"

# 3. Copy Files
mkdir -p "$INSTALL_DIR"

# Copy files from the SUBFOLDER to the Install Directory
# The trailing slash in "$SOURCE_CODE_DIR/" is CRITICAL.
# It tells rsync to copy the CONTENTS, not the folder itself.
rsync -av --progress "$SOURCE_CODE_DIR/" "$INSTALL_DIR" \
    --exclude .git \
    --exclude target \
    --exclude .idea \
    --exclude '*.bat' \
    --exclude '*.iss' \
    --exclude '*.exe' \
    --exclude '*.run' \
    --exclude 'install.sh'

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
Icon=$ICON_PATH
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
    # Stop and remove containers, networks, images, and volumes
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