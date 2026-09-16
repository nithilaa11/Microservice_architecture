# ==========================================================
# E-Commerce Microservices - Quick Launch Script (PowerShell)
# Starts all 6 microservices in order with separate log files
# ==========================================================

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "Starting E-Commerce Microservices Architecture..." -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# Set Java 21 Home if present
if (Test-Path "C:\Program Files\Java\jdk-21") {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    Write-Host "[INFO] Using Java 21: $env:JAVA_HOME" -ForegroundColor Green
}

# Ensure logs directory exists
$logDir = Join-Path $PSScriptRoot "logs"
if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir | Out-Null
}

function Start-ServiceProcess {
    param (
        [string]$ServiceName,
        [string]$JarPath,
        [int]$Port,
        [int]$DelaySeconds = 3
    )

    $fullJarPath = Join-Path $PSScriptRoot $JarPath
    $logFile = Join-Path $logDir "$ServiceName.log"
    $errFile = Join-Path $logDir "$ServiceName-error.log"

    Write-Host "[STARTING] $ServiceName on port $Port..." -ForegroundColor Yellow
    $process = Start-Process -FilePath "java" `
        -ArgumentList "-jar", "`"$fullJarPath`"" `
        -RedirectStandardOutput $logFile `
        -RedirectStandardError $errFile `
        -PassThru

    Write-Host "[RUNNING]  $ServiceName (PID: $($process.Id)) -> Log: logs\$ServiceName.log" -ForegroundColor Green
    Start-Sleep -Seconds $DelaySeconds
}

# 1. Start Eureka Service Discovery (Port 8761)
Start-ServiceProcess -ServiceName "service-discovery" `
    -JarPath "service-discovery\target\service-discovery-1.0.0.jar" `
    -Port 8761 `
    -DelaySeconds 8

# 2. Start Core Domain Microservices
Start-ServiceProcess -ServiceName "user-service" `
    -JarPath "user-service\target\user-service-1.0.0.jar" `
    -Port 8081 `
    -DelaySeconds 4

Start-ServiceProcess -ServiceName "product-service" `
    -JarPath "product-service\target\product-service-1.0.0.jar" `
    -Port 8082 `
    -DelaySeconds 4

Start-ServiceProcess -ServiceName "payment-service" `
    -JarPath "payment-service\target\payment-service-1.0.0.jar" `
    -Port 8084 `
    -DelaySeconds 4

Start-ServiceProcess -ServiceName "order-service" `
    -JarPath "order-service\target\order-service-1.0.0.jar" `
    -Port 8083 `
    -DelaySeconds 6

# 3. Start API Gateway (Port 8085)
Start-ServiceProcess -ServiceName "api-gateway" `
    -JarPath "api-gateway\target\api-gateway-1.0.0.jar" `
    -Port 8085 `
    -DelaySeconds 3

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "All 6 microservices have been launched!" -ForegroundColor Green
Write-Host "Eureka Dashboard:  http://localhost:8761" -ForegroundColor Magenta
Write-Host "API Gateway:       http://localhost:8085" -ForegroundColor Magenta
Write-Host "To stop all services, run: .\stop-all.ps1" -ForegroundColor Yellow
Write-Host "==================================================" -ForegroundColor Cyan
