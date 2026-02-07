#!/bin/bash

###############################################################################
# Script para Ver Logs del Panel Web en Tiempo Real
###############################################################################

# Colores
BLUE='\033[0;34m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="${MINECRAFT_INSTALL_DIR:-$SCRIPT_DIR/../server}"
SERVER_DIR="$BASE_DIR"
if [ -d "$BASE_DIR/server" ]; then
    SERVER_DIR="$BASE_DIR/server"
fi
LOG_FILE="$SERVER_DIR/web/panel.log"

echo -e "${BLUE}═══ Logs del Panel Web MMORPG ═══${NC}"
echo ""

# Verificar si existe el archivo de log
if [ ! -f "$LOG_FILE" ]; then
    # Intentar con journalctl si está usando systemd
    if systemctl --version &> /dev/null; then
        echo "Mostrando logs desde systemd (journalctl)..."
        echo ""
        journalctl -u mmorpg-web.service -f --no-pager
    else
        echo "Archivo de log no encontrado: $LOG_FILE"
        echo "Y systemd no está disponible."
        exit 1
    fi
else
    # Mostrar logs desde archivo
    echo "Mostrando logs desde: $LOG_FILE"
    echo "Presiona Ctrl+C para salir"
    echo ""
    tail -f "$LOG_FILE"
fi
