#!/bin/bash

###############################################################################
# Script para Mostrar Estado Detallado del Panel Web
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
WEB_DIR="$SERVER_DIR/web"

echo -e "${BLUE}"
echo "═══════════════════════════════════════════════════════════════"
echo "  📊 ESTADO DETALLADO - PANEL WEB MMORPG"
echo "═══════════════════════════════════════════════════════════════"
echo -e "${NC}"

# =====================================================================
# SERVICIO SYSTEMD
# =====================================================================
echo -e "${BLUE}[Servicio Systemd]${NC}"

if systemctl --version &> /dev/null; then
    if systemctl is-active --quiet mmorpg-web.service; then
        echo -e "  Estado: ${GREEN}●${NC} activo (corriendo)"
        
        # Información del servicio
        MAIN_PID=$(systemctl show mmorpg-web.service -p MainPID | cut -d= -f2)
        UPTIME=$(systemctl show mmorpg-web.service -p ActiveEnterTimestamp | cut -d= -f2)
        MEMORY=$(systemctl show mmorpg-web.service -p MemoryCurrent | cut -d= -f2)
        
        echo "  PID: $MAIN_PID"
        echo "  Inicio: $UPTIME"
        
        if [ "$MEMORY" != "[not set]" ] && [ -n "$MEMORY" ]; then
            MEMORY_MB=$((MEMORY / 1024 / 1024))
            echo "  Memoria: ${MEMORY_MB}MB"
        fi
        
    else
        echo -e "  Estado: ${RED}●${NC} inactivo (muerto)"
        
        # Mostrar último log de error
        LAST_ERROR=$(systemctl status mmorpg-web.service 2>&1 | tail -1)
        if [ -n "$LAST_ERROR" ]; then
            echo -e "  ${YELLOW}Último estado: $LAST_ERROR${NC}"
        fi
    fi
    
    # Estado de habilitación
    if systemctl is-enabled --quiet mmorpg-web.service 2>/dev/null; then
        echo "  Habilitado: sí (inicia con el sistema)"
    else
        echo "  Habilitado: no"
    fi
else
    echo "  Systemd: no disponible"
fi

# =====================================================================
# PROCESO
# =====================================================================
echo ""
echo -e "${BLUE}[Proceso Python]${NC}"

PID=$(pgrep -f "flask.*app.py" || pgrep -f "gunicorn.*app:app")

if [ -n "$PID" ]; then
    echo -e "  Estado: ${GREEN}CORRIENDO${NC}"
    echo "  PID: $PID"
    
    # Información detallada del proceso
    if [ -f "/proc/$PID/status" ]; then
        THREADS=$(grep "Threads:" /proc/$PID/status | awk '{print $2}')
        echo "  Threads: $THREADS"
    fi
    
    # Recursos
    MEM=$(ps -p $PID -o %mem --no-headers | tr -d ' ')
    CPU=$(ps -p $PID -o %cpu --no-headers | tr -d ' ')
    RSS=$(ps -p $PID -o rss --no-headers | tr -d ' ')
    RSS_MB=$((RSS / 1024))
    
    echo "  CPU: ${CPU}%"
    echo "  Memoria: ${MEM}% (${RSS_MB}MB)"
    
    # Tiempo de ejecución
    ETIME=$(ps -p $PID -o etime --no-headers | tr -d ' ')
    echo "  Tiempo ejecutándose: $ETIME"
    
else
    echo -e "  Estado: ${RED}NO CORRIENDO${NC}"
fi

# =====================================================================
# RED Y PUERTOS
# =====================================================================
echo ""
echo -e "${BLUE}[Red y Conectividad]${NC}"

WEB_PORT=5000

# Verificar puerto
if command -v ss &> /dev/null; then
    PORT_INFO=$(ss -tulpn 2>/dev/null | grep ":$WEB_PORT " | head -1)
    
    if [ -n "$PORT_INFO" ]; then
        echo -e "  Puerto $WEB_PORT: ${GREEN}ABIERTO${NC}"
        echo "  $PORT_INFO"
    else
        echo -e "  Puerto $WEB_PORT: ${RED}CERRADO${NC}"
    fi
elif command -v netstat &> /dev/null; then
    if netstat -tuln | grep -q ":$WEB_PORT "; then
        echo -e "  Puerto $WEB_PORT: ${GREEN}ABIERTO${NC}"
    else
        echo -e "  Puerto $WEB_PORT: ${RED}CERRADO${NC}"
    fi
fi

# Test HTTP
if command -v curl &> /dev/null; then
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$WEB_PORT/ 2>/dev/null || echo "000")
    RESPONSE_TIME=$(curl -s -o /dev/null -w "%{time_total}" http://localhost:$WEB_PORT/ 2>/dev/null || echo "N/A")
    
    if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 302 ]; then
        echo -e "  HTTP Response: ${GREEN}$HTTP_CODE${NC} (${RESPONSE_TIME}s)"
    else
        echo -e "  HTTP Response: ${RED}$HTTP_CODE${NC}"
    fi
fi

# =====================================================================
# BASE DE DATOS
# =====================================================================
echo ""
echo -e "${BLUE}[Base de Datos]${NC}"

if [ -f "$SERVER_DIR/config/data/universal.db" ]; then
    DB_SIZE=$(du -sh "$SERVER_DIR/config/data/universal.db" | cut -f1)
    DB_MODIFIED=$(stat -c %y "$SERVER_DIR/config/data/universal.db" 2>/dev/null | cut -d. -f1)
    
    echo -e "  Estado: ${GREEN}ENCONTRADA${NC}"
    echo "  Tamaño: $DB_SIZE"
    echo "  Última modificación: $DB_MODIFIED"
    
    # Verificar si SQLite puede abrir la BD
    if command -v sqlite3 &> /dev/null; then
        TABLES=$(sqlite3 "$SERVER_DIR/config/data/universal.db" "SELECT COUNT(*) FROM sqlite_master WHERE type='table';" 2>/dev/null || echo "0")
        echo "  Tablas: $TABLES"
    fi
else
    echo -e "  Estado: ${RED}NO ENCONTRADA${NC}"
fi

# =====================================================================
# ARCHIVOS Y CONFIGURACIÓN
# =====================================================================
echo ""
echo -e "${BLUE}[Archivos]${NC}"

# Verificar app.py
if [ -f "$WEB_DIR/app.py" ]; then
    echo -e "  app.py: ${GREEN}✓${NC}"
else
    echo -e "  app.py: ${RED}✗${NC}"
fi

# Verificar templates
if [ -d "$WEB_DIR/templates" ]; then
    TEMPLATE_COUNT=$(find "$WEB_DIR/templates" -name "*.html" | wc -l)
    echo -e "  Templates: ${GREEN}$TEMPLATE_COUNT archivos${NC}"
else
    echo -e "  Templates: ${RED}✗ directorio no encontrado${NC}"
fi

# Verificar entorno virtual
if [ -d "$WEB_DIR/venv" ]; then
    echo -e "  Entorno virtual: ${GREEN}✓${NC}"
    
    # Versión de Python
    if [ -f "$WEB_DIR/venv/bin/python" ]; then
        PY_VERSION=$("$WEB_DIR/venv/bin/python" --version 2>&1)
        echo "    $PY_VERSION"
    fi
else
    echo -e "  Entorno virtual: ${RED}✗${NC}"
fi

# =====================================================================
# LOGS
# =====================================================================
echo ""
echo -e "${BLUE}[Logs]${NC}"

if [ -f "$WEB_DIR/panel.log" ]; then
    LOG_SIZE=$(du -sh "$WEB_DIR/panel.log" | cut -f1)
    LOG_LINES=$(wc -l < "$WEB_DIR/panel.log")
    
    echo "  Archivo: $WEB_DIR/panel.log"
    echo "  Tamaño: $LOG_SIZE ($LOG_LINES líneas)"
    
    # Últimas 3 líneas
    echo "  Últimas entradas:"
    tail -3 "$WEB_DIR/panel.log" | sed 's/^/    /'
fi

# =====================================================================
# URLs DE ACCESO
# =====================================================================
echo ""
echo -e "${BLUE}[URLs de Acceso]${NC}"

IP=$(hostname -I 2>/dev/null | awk '{print $1}')
HOSTNAME=$(hostname)

echo "  Local:    http://localhost:$WEB_PORT"
echo "  Hostname: http://$HOSTNAME:$WEB_PORT"
if [ -n "$IP" ]; then
    echo "  IP:       http://$IP:$WEB_PORT"
fi

# =====================================================================
# COMANDOS ÚTILES
# =====================================================================
echo ""
echo -e "${BLUE}[Comandos Útiles]${NC}"
echo "  Ver logs:      journalctl -u mmorpg-web.service -f"
echo "  Reiniciar:     systemctl restart mmorpg-web.service"
echo "  Detener:       systemctl stop mmorpg-web.service"
echo "  Iniciar:       systemctl start mmorpg-web.service"

echo ""
echo "═══════════════════════════════════════════════════════════════"

exit 0
