; Script generated for Academic Analytics System
#define MyAppName "Academic Analytics"
#define MyAppVersion "1.0"
#define MyAppPublisher "Raunak Ranjan"
#define MyAppExeName "start_app.bat"

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
AppId={{A1B2C3D4-E5F6-7890-1234-567890ABCDEF}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={sd}\AcademicAnalytics
DefaultGroupName={#MyAppName}
; Only allow installation on 64-bit Windows (required for Docker)
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
; Make the installer look modern
WizardStyle=modern
OutputBaseFilename=AcademicAnalytics_Setup_v1
Compression=lzma
SolidCompression=yes
; We need admin rights to talk to Docker
PrivilegesRequired=admin
; This tells Inno Setup to put the EXE in the same folder as this script
OutputDir=..


[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
; IMPORTANT: Update the 'Source' path below to match your folder on Windows!
; We exclude Linux files (.sh), Git history, and build artifacts
Source: "*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs; Excludes: "*.sh,install.sh,.git,target,.idea,*.run,*.exe"

[Icons]
; 1. Desktop Shortcut
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\src\main\resources\static\icon.ico"
; 2. Start Menu Shortcuts
Name: "{group}\Start {#MyAppName}"; Filename: "{app}\start_app.bat"; IconFilename: "{app}\src\main\resources\static\icon.ico"
Name: "{group}\Stop {#MyAppName}"; Filename: "{app}\stop_app.bat"
Name: "{group}\Uninstall {#MyAppName}"; Filename: "{uninstallexe}"

[Run]
; Launch the app immediately after installation finishes (Optional)
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#MyAppName}}"; Flags: shellexec postinstall skipifsilent

[UninstallRun]
; CRITICAL: Stop Docker containers before removing files
; 'waituntilterminated' -> Waits for the 2-second timeout to finish so Docker closes properly.
; 'skipifdoesntexist' -> If the file is missing (deleted manually), ignore it and continue uninstalling.
Filename: "{app}\stop_app.bat"; Flags: runhidden waituntilterminated skipifdoesntexist; RunOnceId: "StopDockerApp"