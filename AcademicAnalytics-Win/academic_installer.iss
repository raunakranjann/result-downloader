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
; Copy the JAR file
Source: "mainapplication.jar"; DestDir: "{app}"; Flags: ignoreversion
; Copy the Windows JRE folder
Source: "jre\*"; DestDir: "{app}\jre"; Flags: ignoreversion recursesubdirs createallsubdirs
; Copy the Playwright browsers
Source: "browsers\*"; DestDir: "{app}\browsers"; Flags: ignoreversion recursesubdirs createallsubdirs
; Copy the launcher
Source: "start_app.bat"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\Academic Analytics"; Filename: "{app}\launch.vbs"; IconFilename: "{app}\icon.ico"
Name: "{autodesktop}\Academic Analytics"; Filename: "{app}\launch.vbs"; IconFilename: "{app}\icon.ico"

; The new Uninstall shortcut
Name: "{group}\Uninstall Academic Analytics"; Filename: "{uninstallexe}"

[Run]
Filename: "{app}\start_app.bat"; Description: "Launch Academic Analytics"; Flags: nowait postinstall skipifsilent