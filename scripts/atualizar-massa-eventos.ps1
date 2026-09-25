param(
    [switch]$Aplicar,
    [string]$JavaCommand = 'java',
    [string]$DriverPath
)

$ErrorActionPreference = 'Stop'
$pastaBackend = Split-Path $PSScriptRoot -Parent
if (-not $DriverPath) {
    $pastaDriver = Join-Path $env:USERPROFILE '.m2/repository/org/mariadb/jdbc/mariadb-java-client'
    $driverLocal = Get-ChildItem -LiteralPath $pastaDriver -Recurse -Filter '*.jar' |
        Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $driverLocal) { throw 'Driver MariaDB não encontrado. Informe -DriverPath ou baixe as dependências do backend pelo Maven.' }
    $DriverPath = $driverLocal.FullName
}
$argumentosJava = @('--class-path', $DriverPath, (Join-Path $PSScriptRoot 'AtualizarMassaEventos.java'))
if ($Aplicar) { $argumentosJava += '--aplicar' }
Push-Location $pastaBackend
try {
    & $JavaCommand @argumentosJava
    if ($LASTEXITCODE -ne 0) { throw "Atualização terminou com código $LASTEXITCODE" }
} finally { Pop-Location }
