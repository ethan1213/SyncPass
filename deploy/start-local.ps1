param([switch]$NoSeed)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location -LiteralPath $root

if (-not (Test-Path -LiteralPath '.env')) {
    $bytes = New-Object byte[] 32
    [Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    $password = [Convert]::ToHexString($bytes)
    $values = @(
        "DATABASE_PASSWORD=$password",
        'COGNITO_ISSUER=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_2C9bbmKGd',
        'COGNITO_CLIENT_ID=677bhbj1qg892bmlk2pr1h6had',
        'CORS_ORIGINS=http://localhost:3000',
        'LOCAL_API_PORT=18080',
        'VITE_API_URL=http://localhost:18080'
    )
    [IO.File]::WriteAllLines((Join-Path $root '.env'), $values, [Text.UTF8Encoding]::new($false))
    Write-Output 'Configuración local creada en .env (ignorada por Git).'
}

$apiPort = 18080
$portLine = Get-Content -LiteralPath '.env' | Where-Object { $_ -match '^LOCAL_API_PORT=' } | Select-Object -Last 1
if ($portLine) { $apiPort = [int]($portLine -replace '^LOCAL_API_PORT=', '') }
if ($apiPort -lt 1 -or $apiPort -gt 65535) { throw 'LOCAL_API_PORT debe ser un puerto válido.' }
docker compose up --build -d
if ($LASTEXITCODE -ne 0) { throw 'Docker Compose no pudo iniciar SyncPass.' }

$ready = $false
for ($attempt = 0; $attempt -lt 60; $attempt++) {
    try {
        $health = Invoke-RestMethod "http://localhost:$apiPort/actuator/health" -TimeoutSec 3
        if ($health.status -eq 'UP') { $ready = $true; break }
    } catch { Start-Sleep -Seconds 3 }
}
if (-not $ready) { throw 'La API no respondió. Revisa docker compose logs api --tail 80.' }

if (-not $NoSeed) {
    Get-Content -LiteralPath (Join-Path $root 'deploy/local-seed.sql') -Raw |
        docker compose exec -T db psql -U syncpass -d syncpass -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw 'No se pudieron cargar los talleres de prueba.' }
}

Write-Output 'SYNC_PASS_LOCAL_UP=true'
Write-Output 'Web: http://localhost:3000'
Write-Output "API: http://localhost:$apiPort/actuator/health"
Write-Output "Catálogo: http://localhost:$apiPort/api/eventos"
Write-Output 'Mockups: http://localhost:3000/mockups.html'
