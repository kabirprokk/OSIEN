# Osien Forge — native Windows installer (double-click Install-Windows.bat).
# No admin. Installs Node 22 + uv + browsers locally, copies forge files, merges config safely.
# Editions: -Edition full|everyday|creator|agency (or auto from .edition file in package).
param([string]$Edition = "")
$ErrorActionPreference = "Continue"
$ForgeDir = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
if (-not $Edition) {
  $ef = Join-Path $ForgeDir ".edition"
  $Edition = "full"
  if (Test-Path $ef) { $Edition = (Get-Content $ef -Raw).Trim() }
}
$OpenCodeDir = if ($env:OPENCODE_DIR) { $env:OPENCODE_DIR } else { Join-Path $HOME ".config\opencode" }

function Say($m) { Write-Host "[forge] $m" -ForegroundColor Green }
function Warn($m) { Write-Host "[forge] NOTE: $m" -ForegroundColor Yellow }

# 1. Node 20+
$needNode = $true
try {
  $v = (node -p "process.versions.node" 2>$null)
  if ($v -and [int]$v.Split('.')[0] -ge 20) { $needNode = $false; Say "Node OK: v$v" }
} catch {}
if ($needNode) {
  Say "Installing Node 22 (local, no admin)..."
  $idx = Invoke-RestMethod "https://nodejs.org/dist/index.json"
  $ver = ($idx | Where-Object { $_.version -like "v22.*" })[0].version
  $zip = "$env:TEMP\forge-node.zip"
  Invoke-WebRequest "https://nodejs.org/dist/$ver/node-$ver-win-x64.zip" -OutFile $zip
  $dest = "$env:LOCALAPPDATA\OsienForge\nodejs"
  New-Item -ItemType Directory -Force -Path $dest | Out-Null
  Expand-Archive $zip -DestinationPath "$env:TEMP\forge-node" -Force
  Copy-Item "$env:TEMP\forge-node\node-$ver-win-x64\*" $dest -Recurse -Force
  Remove-Item $zip -Force; Remove-Item "$env:TEMP\forge-node" -Recurse -Force
  $env:PATH = "$dest;$env:PATH"
  [Environment]::SetEnvironmentVariable("PATH", "$dest;" + [Environment]::GetEnvironmentVariable("PATH", "User"), "User")
}

# 2. uv
if (-not (Get-Command uvx -ErrorAction SilentlyContinue) -and -not (Test-Path "$HOME\.local\bin\uvx.exe")) {
  Say "Installing uv..."
  powershell -ExecutionPolicy Bypass -Command "irm https://astral.sh/uv/install.ps1 | iex"
  $env:PATH = "$HOME\.local\bin;$env:PATH"
} else { Say "uv OK" }

# 3. Browsers
Say "Installing headless Chromium (one-time download)..."
& npx -y playwright@latest install chromium --only-shell 2>&1 | Select-Object -Last 1

# 4. Warm + files
New-Item -ItemType Directory -Force -Path "$OpenCodeDir\plugin", "$OpenCodeDir\agents", "$OpenCodeDir\game-templates", "$OpenCodeDir\web-templates", "$OpenCodeDir\skills" | Out-Null
Copy-Item "$ForgeDir\plugin\*.ts" "$OpenCodeDir\plugin\" -Force
Copy-Item "$ForgeDir\agents\*.md" "$OpenCodeDir\agents\" -Force
$WebSets = @{
  everyday = @('landing','blog','resume','menu','invite','links','invoice')
  creator  = @('landing','blog','resume','menu','invite','links','invoice','portfolio')
  agency   = @('landing','blog','dashboard','portfolio','api','resume','menu','invite','links','invoice','proposal')
  full     = @('landing','blog','dashboard','portfolio','api','resume','menu','invite','links','invoice','proposal')
}
if (-not $WebSets.ContainsKey($Edition)) { $Edition = "full" }
foreach ($t in $WebSets[$Edition]) {
  $src = Join-Path $ForgeDir "web-templates\$t"
  if (Test-Path $src) { Copy-Item "$src" "$OpenCodeDir\web-templates\" -Recurse -Force }
}
if ($Edition -in @('creator','full')) {
  Copy-Item "$ForgeDir\game-templates\*" "$OpenCodeDir\game-templates\" -Recurse -Force
}
if (Test-Path "$ForgeDir\skills") { Copy-Item "$ForgeDir\skills\*" "$OpenCodeDir\skills\" -Recurse -Force }

# 5. Merge config (safe)
Say "Merging config (yours never overwritten)..."
& python "$ForgeDir\scripts\merge-config.py" --patch "$ForgeDir\config.patch.json" --config "$OpenCodeDir\opencode.json" --edition $Edition

Say "Done! Close this window, open a NEW terminal, restart opencode."
Say "Then double-click studio.html and copy a command. No terminal needed again."
