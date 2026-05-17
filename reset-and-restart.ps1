# reset-and-restart.ps1
# Full reset: wipes Kafka topics, Postgres data, and Streams state.
# Use when topology output format changes (rare during dev).

param(
    [switch]$KeepData = $false
)

$ErrorActionPreference = "Stop"

Write-Host "=== 1) Stop everything ==="
docker compose down

if (-not $KeepData) {
    Write-Host "=== 2) Wipe Postgres volume + Kafka log volumes ==="
    # The postgres volume is named in compose (`pgdata`); Kafka brokers use
    # anonymous volumes which will be re-created on next `up`.
    docker volume rm doctor-office-eai_pgdata -f 2>$null
    # Anonymous Kafka data volumes — list them and remove the dangling ones
    docker volume prune -f
}

Write-Host "=== 3) Rebuild and start ==="
docker compose up -d --build

Write-Host ""
Write-Host "Waiting 45 seconds for brokers + Postgres + Connect to be healthy..."
Start-Sleep -Seconds 45

Write-Host ""
Write-Host "=== 4) Register connectors ==="
& "$PSScriptRoot\kafka-connect\register-connectors.ps1"

Write-Host ""
Write-Host "=== 5) Status ==="
docker compose ps

Write-Host ""
Write-Host "Done. Tail logs with:"
Write-Host "  docker compose logs -f streams-app"
Write-Host "  docker compose logs -f customers-app"
Write-Host ""
Write-Host "Then check metrics with:"
Write-Host "  python admin-cli\admin.py metrics totals"
