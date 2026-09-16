param(
    [Parameter(Mandatory = $true)]
    [string] $Server,
    [string] $User = "root",
    [string] $RemoteDir = "/opt/linkedknowledge"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Front = Join-Path (Split-Path -Parent $Root) "mindmap-ui"
$HostSpec = "${User}@${Server}"
$Key = Join-Path $env:USERPROFILE ".ssh\id_ed25519_lk"
$Ssh = @("-i", $Key, "-o", "IdentitiesOnly=yes")
$Stage = Join-Path $env:TEMP "lk-deploy"
$Bundle = Join-Path $env:TEMP "lk-deploy.tgz"

if (-not (Test-Path (Join-Path $Root ".env"))) { throw "missing $Root\.env" }
if (-not (Test-Path $Front)) { throw "missing frontend $Front" }

if (Test-Path $Stage) { Remove-Item $Stage -Recurse -Force }
New-Item -ItemType Directory -Path (Join-Path $Stage "linkedknowledge\deploy") | Out-Null
New-Item -ItemType Directory -Path (Join-Path $Stage "mindmap-ui") | Out-Null

Copy-Item (Join-Path $Root "pom.xml") (Join-Path $Stage "linkedknowledge\")
Copy-Item (Join-Path $Root "docker-compose.yml") (Join-Path $Stage "linkedknowledge\")
Copy-Item (Join-Path $Root ".dockerignore") (Join-Path $Stage "linkedknowledge\")
Copy-Item (Join-Path $Root ".env") (Join-Path $Stage "linkedknowledge\")
Copy-Item (Join-Path $Root "src") (Join-Path $Stage "linkedknowledge\src") -Recurse
Copy-Item (Join-Path $Root "deploy\Dockerfile.backend") (Join-Path $Stage "linkedknowledge\deploy\")
Copy-Item (Join-Path $Root "deploy\Dockerfile.frontend") (Join-Path $Stage "linkedknowledge\deploy\")
Copy-Item (Join-Path $Root "deploy\nginx.conf") (Join-Path $Stage "linkedknowledge\deploy\")
Copy-Item (Join-Path $Root "deploy\server-setup.sh") (Join-Path $Stage "linkedknowledge\deploy\")

Copy-Item (Join-Path $Front "package.json") (Join-Path $Stage "mindmap-ui\")
Copy-Item (Join-Path $Front "package-lock.json") (Join-Path $Stage "mindmap-ui\")
Copy-Item (Join-Path $Front "vite.config.js") (Join-Path $Stage "mindmap-ui\")
Copy-Item (Join-Path $Front "index.html") (Join-Path $Stage "mindmap-ui\")
Copy-Item (Join-Path $Front "src") (Join-Path $Stage "mindmap-ui\src") -Recurse
if (Test-Path (Join-Path $Front "public")) {
    Copy-Item (Join-Path $Front "public") (Join-Path $Stage "mindmap-ui\public") -Recurse
}

if (Test-Path $Bundle) { Remove-Item $Bundle -Force }
tar -czf $Bundle -C $Stage linkedknowledge mindmap-ui

scp @Ssh $Bundle "${HostSpec}:/tmp/lk-deploy.tgz"
scp @Ssh (Join-Path $Root "deploy\server-setup.sh") "${HostSpec}:/tmp/server-setup.sh"
ssh @Ssh $HostSpec "bash /tmp/server-setup.sh && tar -xzf /tmp/lk-deploy.tgz -C /opt && cd /opt/linkedknowledge && docker compose up -d --build && docker compose ps"
