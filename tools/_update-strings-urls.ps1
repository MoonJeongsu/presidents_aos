param(
    [Parameter(Mandatory = $true)]
    [string]$ProjectId,
    [Parameter(Mandatory = $true)]
    [string]$Region
)

$ErrorActionPreference = "Stop"

$translateUrl = "https://$Region-$ProjectId.cloudfunctions.net/translateSentence"
$bonusUrl = "https://$Region-$ProjectId.cloudfunctions.net/grantTranslationBonus"
$synthesizeUrl = "https://$Region-$ProjectId.cloudfunctions.net/synthesizeSentence"
$ttsBonusUrl = "https://$Region-$ProjectId.cloudfunctions.net/grantTtsBonus"

$stringsPath = Join-Path (Split-Path -Parent $PSScriptRoot) "app\src\main\res\values\strings.xml"
if (-not (Test-Path $stringsPath)) {
    Write-Error "strings.xml not found: $stringsPath"
}

$content = Get-Content -Path $stringsPath -Raw -Encoding UTF8

function Set-StringResourceRegex($name, $value) {
    $pattern = "(<string name=`"$name`" translatable=`"false`">)[^<]*(</string>)"
    if ($content -notmatch $pattern) {
        throw "String resource not found: $name"
    }
    $script:content = [regex]::Replace($content, $pattern, "`${1}$value`${2}")
}

Set-StringResourceRegex "translate_function_url" $translateUrl
Set-StringResourceRegex "grant_bonus_function_url" $bonusUrl
Set-StringResourceRegex "synthesize_function_url" $synthesizeUrl
Set-StringResourceRegex "grant_tts_bonus_function_url" $ttsBonusUrl

Set-Content -Path $stringsPath -Value $content -Encoding UTF8 -NoNewline

Write-Host "[OK] translate_function_url"
Write-Host "     $translateUrl"
Write-Host "[OK] grant_bonus_function_url"
Write-Host "     $bonusUrl"
Write-Host "[OK] synthesize_function_url"
Write-Host "     $synthesizeUrl"
Write-Host "[OK] grant_tts_bonus_function_url"
Write-Host "     $ttsBonusUrl"
