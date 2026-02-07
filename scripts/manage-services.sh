#!/bin/bash

###############################################################################
# Script de gestión de servicios - Minecraft MMORPG
# Permite iniciar, detener, reiniciar, estado y logs del servidor y panel web
###############################################################################

set -e

# Colores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="${MINECRAFT_INSTALL_DIR:-$SCRIPT_DIR/../server}"
SERVER_DIR="$BASE_DIR"
if [ -d "$BASE_DIR/server" ]; then
    SERVER_DIR="$BASE_DIR/server"
fi

usage() {
    echo "Uso: $0 <server|web|all> <start|stop|restart|status|logs>"
    echo "Ejemplos:"
    echo "  $0 server start"
    echo "  $0 web logs"
    echo "  $0 all restart"
}

if [ $# -ne 2 ]; then
    usage
    exit 1
fi

TARGET="$1"
ACTION="$2"

has_systemd() {
    systemctl --version &> /dev/null
}

start_service() {
    local name="$1"
    local unit="$2"
    if has_systemd; then
        sudo systemctl start "$unit"
    else
        echo -e "${YELLOW}Systemd no disponible. Inicia manualmente.${NC}"
        if [ "$name" = "server" ]; then
            echo "  cd $SERVER_DIR && java -Xms4G -Xmx4G -jar paper-1.20.6-151.jar nogui"
        else
            echo "  cd $SERVER_DIR/web && ./start-web.sh"
        fi
    fi
}

stop_service() {
    local name="$1"
    local unit="$2"
    if has_systemd; then
        sudo systemctl stop "$unit"
    else
        if [ "$name" = "server" ]; then
            pkill -f "paper.*jar" || true
        else
            pkill -f "flask.*app.py" || true
        fi
    fi
}

restart_service() {
    local name="$1"
    local unit="$2"
    if has_systemd; then
        sudo systemctl restart "$unit"
    else
        stop_service "$name" "$unit"
        start_service "$name" "$unit"
    fi
}

status_service() {
    local unit="$1"
    if has_systemd; then
        systemctl status "$unit" --no-pager
    else
        echo -e "${YELLOW}Systemd no disponible.${NC}"
    fi
}

logs_service() {
    local name="$1"
    local unit="$2"
    if has_systemd; then
        journalctl -u "$unit" -f --no-pager
    else
        if [ "$name" = "server" ]; then
            tail -f "$SERVER_DIR/logs/latest.log"
        else
            tail -f "$SERVER_DIR/web/panel.log"
        fi
    fi
}

run_action() {
    local name="$1"
    local unit="$2"
    case "$ACTION" in
        start) start_service "$name" "$unit" ;;
        stop) stop_service "$name" "$unit" ;;
        restart) restart_service "$name" "$unit" ;;
        status) status_service "$unit" ;;
        logs) logs_service "$name" "$unit" ;;
        *) usage; exit 1 ;;
    esac
}

echo -e "${BLUE}Gestión de servicios - MMORPG${NC}"

ecase() { :; }

case "$TARGET" in
    server)
        run_action "server" "mmorpg-server.service"
        ;;
    web)
        run_action "web" "mmorpg-web.service"
        ;;
    all)
        run_action "server" "mmorpg-server.service"
        run_action "web" "mmorpg-web.service"
        ;;
    *)
        usage
        exit 1
        ;;
esac
