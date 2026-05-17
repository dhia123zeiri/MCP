# Register all Kafka Connect source/sink connectors (Windows PowerShell).
# Run AFTER `docker compose up -d` and after Connect is healthy.

param(
    [string]$ConnectUrl = "http://localhost:8083"
)

$ErrorActionPreference = "Stop"
$cfgDir = Join-Path $PSScriptRoot "configs"

Write-Host "Waiting for Kafka Connect at $ConnectUrl ..."
while ($true) {
    try {
        Invoke-RestMethod -Uri "$ConnectUrl/" -TimeoutSec 5 | Out-Null
        break
    } catch {
        Start-Sleep -Seconds 2
    }
}
Write-Host "Connect is up."

function Register-Connector($file) {
    $full = Get-Content $file -Raw | ConvertFrom-Json
    $name = $full.name
    $configJson = $full.config | ConvertTo-Json -Depth 10 -Compress
    Write-Host ">>> Registering $name"
    Invoke-RestMethod -Method Put `
        -Uri "$ConnectUrl/connectors/$name/config" `
        -ContentType "application/json" `
        -Body $configJson | Out-Null
    Write-Host "    OK"
}

# Sources
Register-Connector "$cfgDir\source-pettypes.json"
Register-Connector "$cfgDir\source-countries.json"

# Sinks
$sinks = @(
    "sink-revenue-per-item",
    "sink-expenses-per-item",
    "sink-profit-per-item",
    "sink-total-revenue",
    "sink-total-expenses",
    "sink-total-profit",
    "sink-avg-by-item",
    "sink-avg-all",
    "sink-top-profit",
    "sink-windowed-revenue",
    "sink-windowed-expenses",
    "sink-windowed-profit",
    "sink-top-country"
)
foreach ($s in $sinks) {
    Register-Connector "$cfgDir\$s.json"
}

Write-Host ""
Write-Host "All connectors registered. Status:"
Invoke-RestMethod -Uri "$ConnectUrl/connectors?expand=status" |
    ConvertTo-Json -Depth 10
