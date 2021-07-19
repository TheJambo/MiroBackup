$Sourcefolder= "C:\MiroBackup\Daily Backup\"
$Targetfolder= "C:\MiroBackup\Weekly Backup\"
Get-ChildItem -Path $Sourcefolder -Recurse|
Where-Object {
  $_.LastWriteTime -gt [datetime]::Now.AddDays(-1)
}| Copy-Item -Destination $Targetfolder
Get-ChildItem -Path $Sourcefolder -Include *.rtb -File -Recurse | foreach { $_.Delete()}