[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$libDir = 'C:\Users\User\.gemini\antigravity\scratch\FTBLimit\lib'
if (!(Test-Path $libDir)) {
    New-Item -ItemType Directory -Path $libDir -Force | Out-Null
}

$paperUrl = 'https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/1.21.1-R0.1-SNAPSHOT/paper-api-1.21.1-R0.1-20250328.161643-128.jar'
$paperJar = Join-Path $libDir 'paper-api-1.21.1.jar'

Write-Host "Downloading Paper API from $paperUrl..."
Invoke-WebRequest -Uri $paperUrl -OutFile $paperJar
Write-Host "Paper API downloaded: $((Get-Item $paperJar).Length) bytes"
