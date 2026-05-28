#!/usr/bin/env bash
set -euo pipefail

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/compose.yaml"
LOG_DIR="$SCRIPT_DIR/logs"

# Criar diretório de logs
mkdir -p "$LOG_DIR"

# Função para imprimir com cores
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERRO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[AVISO]${NC} $1"
}

# Verificar Docker
if ! command -v docker &> /dev/null; then
    log_error "Docker não está instalado ou não está no PATH."
    exit 1
fi

# Verificar docker compose
if ! docker compose version &> /dev/null; then
    log_error "Docker Compose não está instalado ou não está acessível."
    exit 1
fi

# Parar e remover containers anteriores (opcional mas recomendado)
log_info "1/4 -> Parando containers anteriores (se houver)..."
docker compose -f "$COMPOSE_FILE" down --remove-orphans 2>/dev/null || true

# Subir RabbitMQ
log_info "2/4 -> Subindo RabbitMQ via Docker Compose..."
docker compose -f "$COMPOSE_FILE" up -d

# Aguardar RabbitMQ ficar pronto
log_info "3/4 -> Aguardando RabbitMQ responder na porta 5672..."
RETRIES=60
COUNT=0
while ! timeout 1 bash -c "cat < /dev/tcp/localhost/5672" >/dev/null 2>&1; do
    COUNT=$((COUNT+1))
    if [ "$COUNT" -ge "$RETRIES" ]; then
        log_error "RabbitMQ não respondeu após $RETRIES tentativas."
        log_error "Verifique com: docker ps e docker logs"
        exit 1
    fi
    printf "."
    sleep 1
done
echo
log_info "RabbitMQ está pronto!"

# Iniciar aplicação Spring Boot
log_info "4/4 -> Iniciando aplicação Spring Boot..."
if [ ! -x "$SCRIPT_DIR/mvnw" ]; then
    log_warn "mvnw não está executável. Tentando chmod +x..."
    chmod +x "$SCRIPT_DIR/mvnw"
fi

log_info "Aplicação iniciada. Acesse http://localhost:8080/api"
log_info "Swagger UI: http://localhost:8080/api/swagger-ui.html"
log_info "RabbitMQ Management: http://localhost:15672 (myuser/secret)"

cd "$SCRIPT_DIR"
exec "$SCRIPT_DIR/mvnw" spring-boot:run