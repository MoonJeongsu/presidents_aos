param(
    [Parameter(Mandatory = $true)]
    [string]$ProjectId
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$firebasercPath = Join-Path $root ".firebaserc"

$content = @"
{
  "projects": {
    "default": "$ProjectId"
  }
}
"@

Set-Content -Path $firebasercPath -Value $content -Encoding UTF8
Write-Host "[OK] .firebaserc updated -> $ProjectId"
