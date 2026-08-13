param(
    [Parameter(Mandatory = $true)]
    [string]$BucketName
)

$ErrorActionPreference = "Stop"

$stringsPath = Join-Path (Split-Path -Parent $PSScriptRoot) "app\src\main\res\values\strings.xml"
$envPath = Join-Path $PSScriptRoot "firebase.env.bat"

if (-not (Test-Path $stringsPath)) {
    Write-Error "strings.xml not found: $stringsPath"
}

$content = Get-Content -Path $stringsPath -Raw -Encoding UTF8
$pattern = '(<string name="firebase_storage_bucket" translatable="false">)[^<]*(</string>)'
if ($content -notmatch $pattern) {
    Write-Error "String resource not found: firebase_storage_bucket"
}
$content = [regex]::Replace($content, $pattern, "`${1}$BucketName`${2}")
Set-Content -Path $stringsPath -Value $content -Encoding UTF8 -NoNewline

if (Test-Path $envPath) {
    $lines = Get-Content -Path $envPath -Encoding UTF8
    $found = $false
    $updated = foreach ($line in $lines) {
        if ($line -match "^set FIREBASE_STORAGE_BUCKET=") {
            $found = $true
            "set FIREBASE_STORAGE_BUCKET=$BucketName"
        } else {
            $line
        }
    }
    if (-not $found) {
        $updated += "set FIREBASE_STORAGE_BUCKET=$BucketName"
    }
    Set-Content -Path $envPath -Value $updated -Encoding UTF8
}

Write-Host "[OK] firebase_storage_bucket -> $BucketName"
