[Setup]
; This tells Windows to close the app if it's running during install/uninstall
CloseApplications=yes
; Searches for the window title to identify the process
AppMutex=AcademicAnalyticsMutex
AppId={{A1B2C3D4-E5F6-G7H8-I9J0-K1L2M3N4O5P6}
AppName=Academic Analytics
AppVersion=1.0
DefaultDirName={autopf}\AcademicAnalytics
DefaultGroupName=Academic Analytics
OutputDir=.
OutputBaseFilename=AcademicAnalytics
SetupIconFile=icon.ico
Compression=lzma
SolidCompression=yes
PrivilegesRequired=admin
UninstallDisplayIcon={app}\icon.ico

[Files]
; --- ADDED LAUNCH.VBS HERE ---
Source: "launch.vbs"; DestDir: "{app}"; Flags: ignoreversion
; -----------------------------
Source: "mainapplication.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "jre\*"; DestDir: "{app}\jre"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "browsers\*"; DestDir: "{app}\browsers"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "start_app.bat"; DestDir: "{app}"; Flags: ignoreversion
Source: "icon.ico"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\Academic Analytics"; Filename: "{app}\launch.vbs"; WorkingDir: "{app}"; IconFilename: "{app}\icon.ico"
Name: "{autodesktop}\Academic Analytics"; Filename: "{app}\launch.vbs"; WorkingDir: "{app}"; IconFilename: "{app}\icon.ico"
Name: "{group}\Uninstall Academic Analytics"; Filename: "{uninstallexe}"

[Run]
Filename: "wscript.exe"; Parameters: """{app}\launch.vbs"""; Description: "Launch Academic Analytics"; Flags: nowait postinstall skipifsilent

[UninstallRun]
; Kills only the window named 'Academic Analytics'
Filename: "{cmd}"; Parameters: "/C taskkill /F /FI ""WINDOWTITLE eq Academic Analytics*"" /T"; Flags: runhidden

[UninstallDelete]
; This removes the entire folder and the database created after installation
Type: filesandordirs; Name: "{app}"