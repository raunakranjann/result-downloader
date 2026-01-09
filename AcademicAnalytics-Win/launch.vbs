Set WshShell = CreateObject("WScript.Shell")
Set FSO = CreateObject("Scripting.FileSystemObject")

' Get the folder where THIS .vbs script is actually sitting
strPath = FSO.GetParentFolderName(WScript.ScriptFullName)

' Change the working directory to that folder
WshShell.CurrentDirectory = strPath

' Run the batch file hidden (0 = hide window)
' We use Chr(34) to add double quotes in case the path has spaces
WshShell.Run Chr(34) & strPath & "\start_app.bat" & Chr(34), 0

Set WshShell = Nothing
Set FSO = Nothing