$Sourcefolder= "C:\MiroBackup\5Min\"
$Targetfolder= "C:\MiroBackup\Daily Backup\"
Get-ChildItem -Path $Sourcefolder -Recurse|
Where-Object {
  $_.LastWriteTime -gt [datetime]::Now.AddMinutes(-40)
}| Copy-Item -Destination $Targetfolder
Get-ChildItem -Path $Sourcefolder -Include *.rtb -File -Recurse | foreach { $_.Delete()}