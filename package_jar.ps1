$projectDir = "C:\Users\User\.gemini\antigravity\scratch\FTBLimit"
$buildClasses = Join-Path $projectDir "build\classes"
$targetDir = Join-Path $projectDir "target"
$resourcesDir = Join-Path $projectDir "src\main\resources"
$jarExe = "C:\Program Files\Java\jdk-23\bin\jar.exe"

New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

Write-Host "Копирование ресурсов..."
Get-ChildItem -Path $resourcesDir | ForEach-Object {
    Copy-Item -Path $_.FullName -Destination $buildClasses -Force
    Write-Host "Скопирован: $($_.Name)"
}

$outputJar = Join-Path $targetDir "FTBLimit-1.0.0.jar"
$outputJarLatest = Join-Path $targetDir "FTBLimit.jar"

Write-Host "Сборка JAR архива..."
& $jarExe -cf $outputJar -C $buildClasses .
Copy-Item -Path $outputJar -Destination $outputJarLatest -Force

Write-Host "======================================================="
Write-Host "Плагин успешно собран!"
Write-Host "Файл: $outputJarLatest"
Write-Host "Размер: $((Get-Item $outputJarLatest).Length) байт"
Write-Host "======================================================="
