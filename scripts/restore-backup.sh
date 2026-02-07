#!/bin/bash

###############################################################################
# Script de Restauración - Sistema MMORPG Minecraft
# Restaura un backup completo del sistema
###############################################################################

set -e

# Colores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Configuración
INSTALL_DIR="${MINECRAFT_INSTALL_DIR:-/opt/minecraft-mmorpg}"
BACKUP_BASE_DIR="${BACKUP_DIR:-$HOME/minecraft-backups}"

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

echo -e "${BLUE}"
echo "═══════════════════════════════════════════════════════════════"
echo "  📥 RESTAURACIÓN - SISTEMA MMORPG MINECRAFT"
echo "═══════════════════════════════════════════════════════════════"
echo -e "${NC}"

# Verificar argumento
if [ $# -eq 0 ]; then
    echo "Backups disponibles:"
    echo ""
    ls -lth "$BACKUP_BASE_DIR"/*.tar.gz 2>/dev/null | awk '{print "  " $9 " (" $5 ")"}'
    echo ""
    echo -e "${YELLOW}Uso: $0 <nombre-del-backup>${NC}"
    echo "Ejemplo: $0 mmorpg-backup-20260204_120000"
    exit 1
fi

BACKUP_NAME="$1"
BACKUP_FILE="$BACKUP_BASE_DIR/$BACKUP_NAME.tar.gz"

# Verificar que el backup existe
if [ ! -f "$BACKUP_FILE" ]; then
    echo -e "${RED}❌ Error: Backup no encontrado: $BACKUP_FILE${NC}"
    exit 1
fi

echo "📦 Backup a restaurar: $BACKUP_FILE"
BACKUP_SIZE=$(du -sh "$BACKUP_FILE" | cut -f1)
echo "📊 Tamaño: $BACKUP_SIZE"
echo ""

# =====================================================================
# ADVERTENCIAS
# =====================================================================
echo -e "${RED}⚠️  ADVERTENCIA:${NC}"
echo "Esta operación sobrescribirá los datos actuales del servidor."
echo "Se recomienda hacer un backup del estado actual antes de continuar."
echo ""

if ! confirm "¿Deseas continuar con la restauración?"; then
    echo "Restauración cancelada."
    exit 0
fi

# =====================================================================
# PASO 1: Detener Servicios
# =====================================================================
echo ""
echo -e "${BLUE}[1/5] Deteniendo servicios...${NC}"

if systemctl --version &> /dev/null && [ "$EUID" -eq 0 ]; then
    if systemctl is-active --quiet mmorpg-server.service; then
        echo "  → Deteniendo mmorpg-server.service..."
        systemctl stop mmorpg-server.service
    fi
    
    if systemctl is-active --quiet mmorpg-web.service; then
        echo "  → Deteniendo mmorpg-web.service..."
        systemctl stop mmorpg-web.service
    fi
else
    echo "  → Deteniendo procesos manualmente..."
    pkill -f "paper.*jar" || true
    pkill -f "flask.*app.py" || true
fi

sleep 2
echo -e "${GREEN}✓ Servicios detenidos${NC}"

# =====================================================================
# PASO 2: Extraer Backup
# =====================================================================
echo ""
echo -e "${BLUE}[2/5] Extrayendo backup...${NC}"

TEMP_DIR="/tmp/mmorpg-restore-$$"
mkdir -p "$TEMP_DIR"

echo "  → Extrayendo archivo..."
tar -xzf "$BACKUP_FILE" -C "$TEMP_DIR"

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error al extraer backup${NC}"
    rm -rf "$TEMP_DIR"
    exit 1
fi

echo -e "${GREEN}✓ Backup extraído${NC}"

# =====================================================================
# PASO 3: Validar Backup
# =====================================================================
echo ""
echo -e "${BLUE}[3/5] Validando backup...${NC}"

BACKUP_CONTENT="$TEMP_DIR/$BACKUP_NAME"

# Verificar directorios esperados
REQUIRED_DIRS=("data" "worlds")
for dir in "${REQUIRED_DIRS[@]}"; do
    if [ ! -d "$BACKUP_CONTENT/$dir" ]; then
        echo -e "${YELLOW}⚠️  Advertencia: Directorio $dir no encontrado en backup${NC}"
    else
        echo "  ✓ Encontrado: $dir"
    fi
done

# Verificar base de datos
if [ -f "$BACKUP_CONTENT/data/universal.db" ]; then
    DB_SIZE=$(du -sh "$BACKUP_CONTENT/data/universal.db" | cut -f1)
    echo "  ✓ Base de datos: universal.db ($DB_SIZE)"
else
    echo -e "${YELLOW}⚠️  Advertencia: Base de datos no encontrada${NC}"
fi

echo -e "${GREEN}✓ Validación completada${NC}"

# =====================================================================
# PASO 4: Restaurar Archivos
# =====================================================================
echo ""
echo -e "${BLUE}[4/5] Restaurando archivos...${NC}"

# Crear backup del estado actual
if confirm "¿Deseas hacer un backup rápido del estado actual antes de restaurar?"; then
    CURRENT_BACKUP="$BACKUP_BASE_DIR/pre-restore-$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$CURRENT_BACKUP"
    
    echo "  → Guardando estado actual en: $CURRENT_BACKUP"
    cp -r "$INSTALL_DIR/data" "$CURRENT_BACKUP/" 2>/dev/null || true
    cp -r "$INSTALL_DIR/config" "$CURRENT_BACKUP/" 2>/dev/null || true
    
    echo -e "${GREEN}✓ Backup actual guardado${NC}"
fi

# Restaurar mundos
if [ -d "$BACKUP_CONTENT/worlds" ]; then
    echo "  → Restaurando mundos..."
    mkdir -p "$INSTALL_DIR/minecraft-server"
    cp -r "$BACKUP_CONTENT/worlds"/* "$INSTALL_DIR/minecraft-server/" || true
    echo -e "${GREEN}✓ Mundos restaurados${NC}"
fi

# Restaurar base de datos
if [ -d "$BACKUP_CONTENT/data" ]; then
    echo "  → Restaurando base de datos..."
    mkdir -p "$INSTALL_DIR/data"
    cp -r "$BACKUP_CONTENT/data"/* "$INSTALL_DIR/data/" || true
    echo -e "${GREEN}✓ Base de datos restaurada${NC}"
fi

# Restaurar configuraciones
if [ -d "$BACKUP_CONTENT/config" ]; then
    echo "  → Restaurando configuraciones..."
    mkdir -p "$INSTALL_DIR/config"
    cp -r "$BACKUP_CONTENT/config"/* "$INSTALL_DIR/config/" || true
    
    # Restaurar server.properties si existe
    if [ -f "$BACKUP_CONTENT/config/server.properties" ]; then
        cp "$BACKUP_CONTENT/config/server.properties" "$INSTALL_DIR/minecraft-server/" || true
    fi
    
    echo -e "${GREEN}✓ Configuraciones restauradas${NC}"
fi

# Restaurar plugins
if [ -d "$BACKUP_CONTENT/plugins" ]; then
    echo "  → Restaurando plugins..."
    mkdir -p "$INSTALL_DIR/minecraft-server/plugins"
    cp -r "$BACKUP_CONTENT/plugins"/* "$INSTALL_DIR/minecraft-server/plugins/" || true
    echo -e "${GREEN}✓ Plugins restaurados${NC}"
fi

# Ajustar permisos
if [ -n "$MINECRAFT_USER" ] && id "$MINECRAFT_USER" &>/dev/null && [ "$EUID" -eq 0 ]; then
    echo "  → Ajustando permisos..."
    chown -R "$MINECRAFT_USER:$MINECRAFT_USER" "$INSTALL_DIR"
    echo -e "${GREEN}✓ Permisos ajustados${NC}"
fi

# =====================================================================
# PASO 5: Limpiar y Reiniciar
# =====================================================================
echo ""
echo -e "${BLUE}[5/5] Finalizando restauración...${NC}"

# Limpiar archivos temporales
rm -rf "$TEMP_DIR"
echo "  ✓ Archivos temporales eliminados"

# Reiniciar servicios
if confirm "¿Deseas reiniciar los servicios ahora?"; then
    if systemctl --version &> /dev/null && [ "$EUID" -eq 0 ]; then
        echo "  → Iniciando mmorpg-server.service..."
        systemctl start mmorpg-server.service || true
        
        echo "  → Iniciando mmorpg-web.service..."
        systemctl start mmorpg-web.service || true
        
        echo -e "${GREEN}✓ Servicios iniciados${NC}"
    else
        echo -e "${YELLOW}  → Inicia los servicios manualmente:${NC}"
        echo "     systemctl start mmorpg-server.service"
        echo "     systemctl start mmorpg-web.service"
    fi
else
    echo "  → Servicios no reiniciados"
fi

# =====================================================================
# FINALIZACIÓN
# =====================================================================
echo ""
echo -e "${GREEN}"
echo "═══════════════════════════════════════════════════════════════"
echo "  ✅ RESTAURACIÓN COMPLETADA"
echo "═══════════════════════════════════════════════════════════════"
echo -e "${NC}"

echo -e "${BLUE}📦 Backup restaurado:${NC} $BACKUP_NAME"
echo ""

echo -e "${BLUE}📝 Resumen:${NC}"
echo "   • Mundos restaurados"
echo "   • Base de datos restaurada"
echo "   • Configuraciones restauradas"
echo "   • Plugins restaurados"
echo ""

echo -e "${YELLOW}ℹ️  Próximos pasos:${NC}"
echo "   1. Verifica que los servicios estén corriendo"
echo "   2. Comprueba los logs del servidor"
echo "   3. Conecta al servidor y verifica el mundo"
echo "   4. Accede al panel web y verifica los datos"
echo ""

exit 0
