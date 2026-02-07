#!/bin/bash

###############################################################################
# Script para Verificar Estado del Panel Web
###############################################################################

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
WEB_PORT=5000

echo -e "${BLUE}═══ Estado del Panel Web MMORPG ═══${NC}"
echo ""

# Verificar servicio systemd
if systemctl --version &> /dev/null; then
    if systemctl is-active --quiet mmorpg-web.service; then
        echo -e "${GREEN}✓ Servicio: ACTIVO${NC}"
        
        # Obtener información del servicio
        UPTIME=$(systemctl show mmorpg-web.service -p ActiveEnterTimestamp | cut -d= -f2)
        echo "  Inicio: $UPTIME"
        
    elif systemctl is-enabled --quiet mmorpg-web.service 2>/dev/null; then
        echo -e "${YELLOW}⚠  Servicio: INACTIVO (pero habilitado)${NC}"
    else
        echo -e "${RED}✗ Servicio: NO CONFIGURADO${NC}"
    fi
else
    echo "  → Systemd no disponible"
fi

echo ""

# Verificar proceso
PID=$(pgrep -f "flask.*app.py" || pgrep -f "gunicorn.*app:app")
if [ -n "$PID" ]; then
    echo -e "${GREEN}✓ Proceso: CORRIENDO${NC}"
    echo "  PID: $PID"
    
    # Memoria y CPU
    MEM=$(ps -p $PID -o %mem --no-headers | tr -d ' ')
    CPU=$(ps -p $PID -o %cpu --no-headers | tr -d ' ')
    echo "  Memoria: ${MEM}%"
    echo "  CPU: ${CPU}%"
else
    echo -e "${RED}✗ Proceso: NO CORRIENDO${NC}"
fi

echo ""

# Verificar puerto
if command -v nc &> /dev/null; then
    if nc -z localhost $WEB_PORT 2>/dev/null; then
        echo -e "${GREEN}✓ Puerto $WEB_PORT: ABIERTO${NC}"
    else
        echo -e "${RED}✗ Puerto $WEB_PORT: CERRADO${NC}"
    fi
elif command -v netstat &> /dev/null; then
    if netstat -tuln | grep -q ":$WEB_PORT "; then
        echo -e "${GREEN}✓ Puerto $WEB_PORT: ABIERTO${NC}"
    else
        echo -e "${RED}✗ Puerto $WEB_PORT: CERRADO${NC}"
    fi
fi

# Verificar acceso HTTP
if command -v curl &> /dev/null; then
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$WEB_PORT/ 2>/dev/null || echo "000")
    
    if [ "$HTTP_CODE" -eq 200 ]; then
        echo -e "${GREEN}✓ HTTP: ACCESIBLE (código $HTTP_CODE)${NC}"
    elif [ "$HTTP_CODE" -eq 302 ] || [ "$HTTP_CODE" -eq 301 ]; then
        echo -e "${GREEN}✓ HTTP: REDIRIGIENDO (código $HTTP_CODE)${NC}"
    elif [ "$HTTP_CODE" -eq 000 ]; then
        echo -e "${RED}✗ HTTP: NO ACCESIBLE${NC}"
    else
        echo -e "${YELLOW}⚠  HTTP: Código $HTTP_CODE${NC}"
    fi
fi

echo ""

# Verificar base de datos
if [ -f "$SERVER_DIR/config/data/universal.db" ]; then
    DB_SIZE=$(du -sh "$SERVER_DIR/config/data/universal.db" | cut -f1)
    echo -e "${GREEN}✓ Base de datos: $DB_SIZE${NC}"
else
    echo -e "${RED}✗ Base de datos: NO ENCONTRADA${NC}"
fi

echo ""

# URL de acceso
IP=$(hostname -I | awk '{print $1}')
echo -e "${BLUE}🌐 URLs de acceso:${NC}"
echo "   Local:  http://localhost:$WEB_PORT"
echo "   Red:    http://$IP:$WEB_PORT"

echo ""
echo "═══════════════════════════════════════════════════════════════"

exit 0
