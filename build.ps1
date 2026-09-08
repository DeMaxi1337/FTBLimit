[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$projectDir = 'C:\Users\User\.gemini\antigravity\scratch\FTBLimit'
$jdkBin = 'C:\Program Files\Java\jdk-23\bin'
$javac = Join-Path $jdkBin 'javac.exe'
$jar = Join-Path $jdkBin 'jar.exe'

$libDir = Join-Path $projectDir 'lib'
$buildClasses = Join-Path $projectDir 'build\classes'
$targetDir = Join-Path $projectDir 'target'
$resourcesDir = Join-Path $projectDir 'src\main\resources'

Write-Host 'Checking dependencies...'
& (Join-Path $projectDir 'download_deps.ps1')

if (Test-Path $buildClasses) {
    Remove-Item -Recurse -Force $buildClasses
}
New-Item -ItemType Directory -Force -Path $buildClasses | Out-Null
New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

$srcFiles = Get-ChildItem -Path (Join-Path $projectDir 'src\main\java') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
$sourceListFile = Join-Path $projectDir 'sources.txt'
[System.IO.File]::WriteAllLines($sourceListFile, $srcFiles, (New-Object System.Text.UTF8Encoding($false)))

$libWildcard = Join-Path $libDir '*'
Write-Host "Compiling $($srcFiles.Count) Java source files..."
& $javac --release 21 -encoding UTF-8 -cp $libWildcard -d $buildClasses "@$sourceListFile"

if ($LASTEXITCODE -ne 0) {
    Write-Error 'Javac compilation failed!'
    exit 1
}

Write-Host 'Copying resources...'
Get-ChildItem -Path $resourcesDir | ForEach-Object {
    Copy-Item -Path $_.FullName -Destination $buildClasses -Force
}

$outputJar = Join-Path $targetDir 'FTBLimit-1.0.0.jar'
$outputJarLatest = Join-Path $targetDir 'FTBLimit.jar'

Write-Host 'Packaging JAR archive...'
& $jar -cf $outputJar -C $buildClasses .
Copy-Item -Path $outputJar -Destination $outputJarLatest -Force

Remove-Item $sourceListFile -ErrorAction SilentlyContinue

$jarSize = (Get-Item $outputJarLatest).Length
Write-Host '======================================================='
Write-Host 'Plugin successfully built!'
Write-Host "JAR: $outputJarLatest"
Write-Host "Size: $jarSize bytes"
Write-Host '======================================================='
