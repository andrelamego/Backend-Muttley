param(
    [string]$MavenCommand = 'mvn.cmd',
    [string]$MavenRepository,
    [switch]$Offline,
    [switch]$Integration
)

$ErrorActionPreference = 'Stop'
$workspace = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$reportDir = Join-Path $workspace 'Backend-Muttley/target/relatorios-requisitos'
$results = [System.Collections.Generic.List[object]]::new()
$modules = @('Backend-Muttley', 'Microservice-Email-Muttley', 'Microservice-Pdf-Muttley', 'Microservice-QrCode-Muttley')

foreach ($module in $modules) {
    $modulePath = Join-Path $workspace $module
    $arguments = @('clean', 'test', '-B', '-Dstyle.color=never')
    if ($Integration -and $module -eq 'Backend-Muttley') {
        $arguments = @('clean', 'verify', '-Pintegration', '-B', '-Dstyle.color=never')
    }
    if ($Offline) { $arguments += '-o' }
    if ($MavenRepository) { $arguments += "-Dmaven.repo.local=$MavenRepository" }
    Write-Host "Executando testes: $module"
    Push-Location $modulePath
    try {
        # Armazena a saída até o clean terminar, pois ele remove target do backend.
        $output = & $MavenCommand @arguments 2>&1
        $exitCode = $LASTEXITCODE
    } finally { Pop-Location }
    New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
    $output | Out-File (Join-Path $reportDir "$module.log") -Encoding utf8
    $counts = @{ testes=0; falhas=0; erros=0; ignorados=0 }
    $xmlFiles = Get-ChildItem -LiteralPath (Join-Path $modulePath 'target/surefire-reports') -Filter 'TEST-*.xml' -ErrorAction SilentlyContinue
    foreach ($file in $xmlFiles) {
        [xml]$xml = Get-Content -LiteralPath $file.FullName -Raw
        $counts.testes += [int]$xml.testsuite.tests
        $counts.falhas += [int]$xml.testsuite.failures
        $counts.erros += [int]$xml.testsuite.errors
        $counts.ignorados += [int]$xml.testsuite.skipped
    }
    $results.Add([pscustomobject]@{ modulo=$module; codigoSaida=$exitCode; testes=$counts.testes; falhas=$counts.falhas; erros=$counts.erros; ignorados=$counts.ignorados })
    if ($Integration -and $module -eq 'Backend-Muttley') {
        $integrationCounts = @{ testes=0; falhas=0; erros=0; ignorados=0 }
        $integrationFiles = Get-ChildItem -LiteralPath (Join-Path $modulePath 'target/failsafe-reports') -Filter 'TEST-*.xml' -ErrorAction SilentlyContinue
        foreach ($file in $integrationFiles) {
            [xml]$xml = Get-Content -LiteralPath $file.FullName -Raw
            $integrationCounts.testes += [int]$xml.testsuite.tests
            $integrationCounts.falhas += [int]$xml.testsuite.failures
            $integrationCounts.erros += [int]$xml.testsuite.errors
            $integrationCounts.ignorados += [int]$xml.testsuite.skipped
        }
        $results.Add([pscustomobject]@{ modulo='Backend-Muttley (integracao MySQL)'; codigoSaida=$exitCode; testes=$integrationCounts.testes; falhas=$integrationCounts.falhas; erros=$integrationCounts.erros; ignorados=$integrationCounts.ignorados })
    }
    if ($exitCode -ne 0) { $output | Select-Object -Last 35 | Write-Host }
}

Write-Host 'Executando testes: front-muttley (Node.js 22.18+ ou 24+)'
Push-Location (Join-Path $workspace 'front-muttley')
try {
    $output = & node --test --test-reporter=tap 'tests/*.test.mjs' 2>&1
    $exitCode = $LASTEXITCODE
} finally { Pop-Location }
$output | Out-File (Join-Path $reportDir 'front-muttley.log') -Encoding utf8
$textOutput = $output -join "`n"
$tests = if ($textOutput -match '(?m)^# tests (\d+)') { [int]$Matches[1] } else { 0 }
$failures = if ($textOutput -match '(?m)^# fail (\d+)') { [int]$Matches[1] } else { 0 }
$skipped = if ($textOutput -match '(?m)^# skipped (\d+)') { [int]$Matches[1] } else { 0 }
$results.Add([pscustomobject]@{ modulo='front-muttley'; codigoSaida=$exitCode; testes=$tests; falhas=$failures; erros=0; ignorados=$skipped })
if ($exitCode -ne 0) { $output | Select-Object -Last 35 | Write-Host }
$results | ConvertTo-Json | Set-Content (Join-Path $reportDir 'resumo.json') -Encoding utf8
$results | Format-Table -AutoSize
Write-Host "Relatórios: $reportDir"
if (@($results | Where-Object { $_.codigoSaida -ne 0 -or $_.testes -eq 0 }).Count -gt 0) { exit 1 }
exit 0
