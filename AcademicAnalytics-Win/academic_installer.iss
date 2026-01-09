[Setup]
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
; Shortcuts now point to launch.vbs for a silent startup
Name: "{group}\Academic Analytics"; Filename: "{app}\launch.vbs"; WorkingDir: "{app}"; IconFilename: "{app}\icon.ico"
Name: "{autodesktop}\Academic Analytics"; Filename: "{app}\launch.vbs"; WorkingDir: "{app}"; IconFilename: "{app}\icon.ico"
Name: "{group}\Uninstall Academic Analytics"; Filename: "{uninstallexe}"

[Run]
; Launch via launch.vbs so the black console window doesn't appear after installation
Filename: "wscript.exe"; Parameters: """{app}\launch.vbs"""; Description: "Launch Academic Analytics"; Flags: nowait postinstall skipifsilent