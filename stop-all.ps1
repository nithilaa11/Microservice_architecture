# ==========================================================
# E-Commerce Microservices - Quick Stop Script (PowerShell)
# Finds and terminates processes listening on project ports
# ==========================================================

$ports = @(8761, 8085, 8081, 8082, 8083, 8084)

Write-Host "Stopping all E-Commerce microservices..." -ForegroundColor Yellow

foreach ($port in $ports) {
    try {
        $connections = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
        if ($connections) {
            foreach ($conn in $connections) {
                $processId = $conn.OwningProcess
                if ($processId -gt 0) {
                    $proc = Get-Process -Id $processId -ErrorAction SilentlyContinue
                    if ($proc) {
                        Write-Host "Stopping process on port $port (PID: $processId, Name: $($proc.ProcessName))..." -ForegroundColor Red
                        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
                    }
                }
            }
        } else {
            Write-Host "Port $port is clear." -ForegroundColor Gray
        }
    } catch {
        # Ignore errors if already stopped
    }
}

Write-Host "All microservices stopped successfully!" -ForegroundColor Green
