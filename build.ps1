param(
    [int]$MaxRetries = 60,
    [int]$DelaySeconds = 1
)

# Função para logs coloridos
function Write-InfoLog {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Write-ErrorLog {
    param([string]$Message)
    Write-Host "[ERRO] $Message" -ForegroundColor Red
}

function Write-WarnLog {
    param([string]$Message)
    Write-Host "[AVISO] $Message" -ForegroundColor Yellow
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$composeFile = Join-Path $scriptDir 'compose.yaml'
$logsDir = Join-Path $scriptDir 'logs'

# Criar diretório de logs
if (-not (Test-Path $logsDir)) {
    New-Item -ItemType Directory -Path $logsDir | Out-Null
}

# Verificar Docker
Write-InfoLog "1/4 -> Parando containers anteriores (se houver)..."
try {
    docker compose -f $composeFile down --remove-orphans 2>$null
} catch {
    # Ignorar erros se não houver containers
}

Write-InfoLog "2/4 -> Subindo RabbitMQ via Docker Compose..."
docker compose -f $composeFile up -d

if ($LASTEXITCODE -ne 0) {
    Write-ErrorLog "Erro ao subir RabbitMQ. Verifique se Docker está rodando."
    exit 1
}

Write-InfoLog "3/4 -> Aguardando RabbitMQ responder na porta 5672..."
$attempt = 0
while ($true) {
    try {
        $res = Test-NetConnection -ComputerName 'localhost' -Port 5672 -WarningAction SilentlyContinue
        if ($res.TcpTestSucceeded) {
            break
        }
    } catch {
        # Continuar tentando
    }

    $attempt++
    if ($attempt -ge $MaxRetries) {
        Write-ErrorLog "RabbitMQ não respondeu na porta 5672 após $MaxRetries tentativas."
        Write-ErrorLog "Tente verificar: docker ps e docker logs"
        exit 1
    }

    Write-Host -NoNewline "."
    Start-Sleep -Seconds $DelaySeconds
}

Write-Host ""
Write-InfoLog "RabbitMQ está pronto!"

Write-InfoLog "4/4 -> Iniciando aplicação Spring Boot..."

Write-InfoLog "Aplicação iniciada. Acesse http://localhost:8080/api"
Write-InfoLog "Swagger UI: http://localhost:8080/api/swagger-ui.html"
Write-InfoLog "RabbitMQ Management: http://localhost:15672 (myuser/secret)"

Push-Location $scriptDir
try {
    if (Test-Path ".\mvnw.cmd") {
        & ".\mvnw.cmd" spring-boot:run
    } elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
        mvn spring-boot:run
    } else {
        Write-ErrorLog "Maven wrapper (.\\mvnw.cmd) ou mvn não encontrado."
        exit 1
    }
} finally {
    Pop-Location
}