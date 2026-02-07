#!/bin/bash

###############################################################################
# Script para Cambiar la Versión de Paper del Servidor
###############################################################################

set -e

# Colores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

INSTALL_DIR="${MINECRAFT_INSTALL_DIR:-/opt/minecraft-mmorpg}"
SERVER_DIR="$INSTALL_DIR/minecraft-server"

echo -e "${BLUE}"
echo "═══════════════════════════════════════════════════════════════"
echo "  🔄 CAMBIO DE VERSIÓN - PAPER SERVER"
echo "═══════════════════════════════════════════════════════════════"
echo -e "${NC}"

# Función de confirmación
confirm() {
    while true; do
        read -p "$1 (s/n): " yn
        case $yn in
            [Ss]* ) return 0;;
            [Nn]* ) return 1;;
            * ) echo "Por favor responde s (sí) o n (no).";;
        esac
    done
}

# Mostrar versión actual
if [ -f "$SERVER_DIR/paper.jar" ]; then
    CURRENT_SIZE=$(du -sh "$SERVER_DIR/paper.jar" | cut -f1)
    echo "📍 Servidor actual: $SERVER_DIR/paper.jar ($CURRENT_SIZE)"
else
    echo "📍 No se encontró paper.jar en $SERVER_DIR"
fi

echo ""

# Pedir nueva versión
read -p "Versión de Minecraft (ej: 1.20.6): " MC_VERSION
read -p "Build de Paper (ej: 151, o 'latest'): " BUILD_NUMBER

if [ -z "$MC_VERSION" ]; then
    echo -e "${RED}Error: Debes especificar una versión${NC}"
    exit 1
fi

# Resolver 'latest'
if [ "$BUILD_NUMBER" = "latest" ] || [ -z "$BUILD_NUMBER" ]; then
    echo "  → Obteniendo último build..."
    BUILD_NUMBER=$(curl -s "https://api.papermc.io/v2/projects/paper/versions/$MC_VERSION" | \
                   grep -o '"builds":\[[0-9,]*\]' | grep -o '[0-9]*' | tail -1)
    
    if [ -z "$BUILD_NUMBER" ]; then
        echo -e "${RED}Error: No se pudo obtener el último build${NC}"
        exit 1
    fi
    
    echo "  → Último build: $BUILD_NUMBER"
fi

DOWNLOAD_URL="https://api.papermc.io/v2/projects/paper/versions/$MC_VERSION/builds/$BUILD_NUMBER/downloads/paper-$MC_VERSION-$BUILD_NUMBER.jar"

echo ""
echo -e "${BLUE}📥 Descargando Paper $MC_VERSION build $BUILD_NUMBER...${NC}"
echo "   URL: $DOWNLOAD_URL"
echo ""

if ! confirm "¿Continuar con la descarga?"; then
    echo "Operación cancelada."
    exit 0
fi

# =====================================================================
# PASO 1: Detener Servidor
# =====================================================================
echo ""
echo -e "${BLUE}[1/5] Deteniendo servidor...${NC}"

if systemctl --version &> /dev/null && systemctl is-active --quiet mmorpg-server.service; then
    echo "  → Deteniendo mmorpg-server.service..."
    systemctl stop mmorpg-server.service
else
    echo "  → Deteniendo proceso manualmente..."
    pkill -f "paper.*jar" || true
fi

sleep 2
echo -e "${GREEN}✓ Servidor detenido${NC}"

# =====================================================================
# PASO 2: Hacer Backup del JAR Actual
# =====================================================================
echo ""
echo -e "${BLUE}[2/5] Haciendo backup del JAR actual...${NC}"

if [ -f "$SERVER_DIR/paper.jar" ]; then
    BACKUP_NAME="paper-backup-$(date +%Y%m%d_%H%M%S).jar"
    echo "  → Guardando como: $BACKUP_NAME"
    mv "$SERVER_DIR/paper.jar" "$SERVER_DIR/$BACKUP_NAME"
    echo -e "${GREEN}✓ Backup creado${NC}"
else
    echo "  → No hay JAR actual para hacer backup"
fi

# =====================================================================
# PASO 3: Descargar Nueva Versión
# =====================================================================
echo ""
echo -e "${BLUE}[3/5] Descargando nueva versión...${NC}"

cd "$SERVER_DIR"

if curl -L -o "paper.jar" "$DOWNLOAD_URL"; then
    NEW_SIZE=$(du -sh paper.jar | cut -f1)
    echo -e "${GREEN}✓ Descarga completada ($NEW_SIZE)${NC}"
else
    echo -e "${RED}❌ Error en la descarga${NC}"
    
    # Restaurar backup si existe
    if [ -f "$SERVER_DIR/$BACKUP_NAME" ]; then
        echo "  → Restaurando backup..."
        mv "$SERVER_DIR/$BACKUP_NAME" "$SERVER_DIR/paper.jar"
    fi
    
    exit 1
fi

# =====================================================================
# PASO 4: Verificar Integridad
# =====================================================================
echo ""
echo -e "${BLUE}[4/5] Verificando integridad...${NC}"

# Verificar que es un archivo JAR válido
if file paper.jar | grep -q "Java archive data"; then
    echo -e "${GREEN}✓ Archivo JAR válido${NC}"
else
    echo -e "${RED}❌ Archivo no parece ser un JAR válido${NC}"
    
    # Restaurar backup
    if [ -f "$SERVER_DIR/$BACKUP_NAME" ]; then
        echo "  → Restaurando backup..."
        rm -f paper.jar
        mv "$SERVER_DIR/$BACKUP_NAME" "$SERVER_DIR/paper.jar"
    fi
    
    exit 1
fi

# =====================================================================
# PASO 5: Iniciar Servidor
# =====================================================================
echo ""
echo -e "${BLUE}[5/5] Iniciando servidor...${NC}"

if confirm "¿Deseas iniciar el servidor ahora?"; then
    if systemctl --version &> /dev/null && [ "$EUID" -eq 0 ]; then
        echo "  → Iniciando mmorpg-server.service..."
        systemctl start mmorpg-server.service
        
        echo "  → Esperando inicio..."
        sleep 5
        
        if systemctl is-active --quiet mmorpg-server.service; then
            echo -e "${GREEN}✓ Servidor iniciado correctamente${NC}"
        else
            echo -e "${YELLOW}⚠️  Servidor podría tener problemas al iniciar${NC}"
            echo "     Revisa los logs: journalctl -u mmorpg-server.service -f"
        fi
    else
        echo -e "${YELLOW}  → Inicia el servidor manualmente:${NC}"
        echo "     cd $SERVER_DIR"
        echo "     java -Xmx2G -Xms2G -jar paper.jar --nogui"
    fi
else
    echo "  → Servidor no iniciado"
fi

# =====================================================================
# FINALIZACIÓN
# =====================================================================
echo ""
echo -e "${GREEN}"
echo "═══════════════════════════════════════════════════════════════"
echo "  ✅ CAMBIO DE VERSIÓN COMPLETADO"
echo "═══════════════════════════════════════════════════════════════"
echo -e "${NC}"

echo -e "${BLUE}📝 Resumen:${NC}"
echo "   Nueva versión: Paper $MC_VERSION build $BUILD_NUMBER"
echo "   Ubicación: $SERVER_DIR/paper.jar"

if [ -f "$SERVER_DIR/$BACKUP_NAME" ]; then
    echo "   Backup: $SERVER_DIR/$BACKUP_NAME"
fi

echo ""

echo -e "${YELLOW}ℹ️  Notas importantes:${NC}"
echo "   • Verifica los logs del servidor tras el inicio"
echo "   • Comprueba que todos los plugins carguen correctamente"
echo "   • El backup del JAR anterior se conserva por seguridad"
echo "   • Para volver a la versión anterior: mv $BACKUP_NAME paper.jar"
echo ""

exit 0
