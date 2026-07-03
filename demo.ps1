# Demo automatizada - DonaTrack Logistica
# Uso: .\demo.ps1

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$BASE = "http://localhost:8080"

function Paso($numero, $descripcion) {
    Write-Host ""
    Write-Host "-- Paso ${numero}: $descripcion" -ForegroundColor Cyan
}

function Mostrar($url) {
    $resp = Invoke-RestMethod -Uri $url -Method GET
    $resp | ConvertTo-Json -Depth 5
}

function Post($url, $body) {
    try {
        $resp = Invoke-RestMethod -Uri $url -Method POST -Body ($body | ConvertTo-Json -Depth 5) -ContentType "application/json"
        $resp | ConvertTo-Json -Depth 5
    } catch {
        Write-Host $_.Exception.Message -ForegroundColor Yellow
    }
}

# ── Arrancar el server en background ──────────────────────────────────────────

Paso 0 "Compilando y arrancando el servidor..."
$jar = ".\target\tpa-entrega-1-1.0-SNAPSHOT.jar"

if (-not (Test-Path $jar)) {
    Write-Host "  Compilando JAR..." -ForegroundColor Gray
    mvn clean package -DskipTests -q
}

$server = Start-Process -FilePath "java" -ArgumentList "-jar", $jar `
    -PassThru -WindowStyle Hidden

Write-Host "  Servidor iniciado (PID $($server.Id)). Esperando que levante..." -ForegroundColor Gray
Start-Sleep -Seconds 3

# ── Flujo de demo ─────────────────────────────────────────────────────────────

Paso 1 "Lista de camiones (vacía al inicio)"
Mostrar "$BASE/camiones"

Paso 2 "Agregar camión AB123CD"
Post "$BASE/camiones" @{
    patente    = "AB123CD"
    volumenM3  = 30
    alturaM    = 2.6
    capacidadKg = 5000
}

Paso 3 "Verificar camión guardado"
Mostrar "$BASE/camiones"

Paso 4 "Simular callback del planificador (crea Ruta + Entregas)"
Post "$BASE/planificador/callback" @{
    rutas = @(@{
        camionId = 1
        paradas  = @(@{
            entidadBeneficiariaId = 5
            direccion             = "Av. Siempreviva 742"
            donacionesIds         = @(1, 2)
        })
    })
    donacionesNoAsignadas = @()
}

Paso 5 "Ver ruta creada (estado: PLANIFICADA)"
Mostrar "$BASE/rutas"

Paso 6 "Ver entregas creadas (estado: PENDIENTE)"
Mostrar "$BASE/entregas"

Paso 7 "El chofer inicia la ruta → entregas pasan a EN_TRASLADO automáticamente"
try {
    Invoke-RestMethod -Uri "$BASE/rutas/1/iniciar" -Method POST | Out-Null
    Write-Host "  Ruta iniciada OK" -ForegroundColor Green
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Yellow
}
Mostrar "$BASE/entregas"

Paso 8 "Reportar posición GPS del camión (app móvil del conductor)"
Post "$BASE/camiones/1/posicion" @{
    latitud   = -34.6037
    longitud  = -58.3816
    velocidad = 45.5
    timestamp = "2026-07-02T18:00:00Z"
}
Mostrar "$BASE/dashboard/camiones"

Paso 9 "La entidad confirma recepción → ENTREGADA (con camión y fechaHora)"
Post "$BASE/entregas/1/confirmar" @{
    camionId  = 1
    fechaHora = "2026-07-02T18:30:00"
}
Mostrar "$BASE/entregas"

Paso 10 "La entidad rechaza la entrega 2 → NO_RECIBIDA"
Post "$BASE/entregas/2/rechazar" @{ motivo = "Nadie en el domicilio" }

Paso 11 "La donación retorna al depósito → vuelve a PENDIENTE (replanificable)"
try {
    Invoke-RestMethod -Uri "$BASE/entregas/2/retornar" -Method POST | Out-Null
    Write-Host "  Retorno OK" -ForegroundColor Green
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Yellow
}
Mostrar "$BASE/entregas"

# ── Apagar el server ──────────────────────────────────────────────────────────

Write-Host ""
Write-Host "── Demo completada. Apagando servidor (PID $($server.Id))..." -ForegroundColor Cyan
Stop-Process -Id $server.Id -Force
Write-Host "   Servidor detenido." -ForegroundColor Gray
