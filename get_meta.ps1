$meta = Invoke-RestMethod -Uri 'https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/1.21.1-R0.1-SNAPSHOT/maven-metadata.xml'
$meta.metadata.versioning.snapshotVersions.snapshotVersion | Out-String | Write-Host
