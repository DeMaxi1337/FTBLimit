[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$libDir = "C:\Users\User\.gemini\antigravity\scratch\FTBLimit\lib"

$urls = @(
    "https://repo1.maven.org/maven2/net/kyori/adventure-api/4.17.0/adventure-api-4.17.0.jar",
    "https://repo1.maven.org/maven2/net/kyori/adventure-key/4.17.0/adventure-key-4.17.0.jar",
    "https://repo1.maven.org/maven2/net/kyori/examination-api/1.3.0/examination-api-1.3.0.jar",
    "https://repo1.maven.org/maven2/net/kyori/examination-string/1.3.0/examination-string-1.3.0.jar",
    "https://repo1.maven.org/maven2/net/md-5/bungeecord-chat/1.20-R0.2/bungeecord-chat-1.20-R0.2.jar",
    "https://repo1.maven.org/maven2/com/google/guava/guava/33.2.1-jre/guava-33.2.1-jre.jar"
)

foreach ($u in $urls) {
    $fname = Split-Path -Leaf $u
    $dest = Join-Path $libDir $fname
    if (!(Test-Path $dest)) {
        Write-Host "Downloading $fname..."
        Invoke-WebRequest -Uri $u -OutFile $dest
    }
}

Get-ChildItem $libDir | Select-Object Name, Length
