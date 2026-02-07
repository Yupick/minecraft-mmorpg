#!/bin/bash

###############################################################################
# Script de Desinstalación - Sistema MMORPG Minecraft
# Desinstala completamente el sistema incluyendo servidor, plugin y panel web
###############################################################################

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}"
echo "═══════════════════════════════════════════════════════════════"
echo "  🗑️  DESINSTALACIÓN - SISTEMA MMORPG MINECRAFT"
echo "═══════════════════════════════════════════════════════════════"
echo -e "${NC}"

# Verificar si se ejecuta con permisos de root para systemd
if systemctl --version &> /dev/null; then
    if [ "$EUID" -ne 0 ]; then
        echo -e "${YELLOW}⚠️  Se recomienda ejecutar con sudo para desinstalar servicios systemd${NC}"
    fi
fi

# Función para preguntar confirmación
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

# Preguntar confirmación
echo -e "${RED}¡ADVERTENCIA!${NC}"
echo "Esta acción desinstalará completamente el sistema MMORPG."
echo ""
if ! confirm "¿Estás seguro de que deseas continuar?"; then
    echo -e "${GREEN}Desinstalación cancelada.${NC}"
    exit 0
fi

# Variables de rutas
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INSTALL_DIR="${MINECRAFT_INSTALL_DIR:-$SCRIPT_DIR/server}"
SERVICE_USER="${MINECRAFT_USER:-minecraft}"

# Detectar estructura de servidor (actual vs legacy)
SERVER_DIR="$INSTALL_DIR"
if [ ! -d "$SERVER_DIR" ]; then
    if [ -d "$INSTALL_DIR/server" ]; then
        SERVER_DIR="$INSTALL_DIR/server"
    elif [ -d "$INSTALL_DIR/minecraft-server" ]; then
        SERVER_DIR="$INSTALL_DIR/minecraft-server"
    fi
fi

echo ""
echo -e "${BLUE}📍 Directorio de instalación: $INSTALL_DIR${NC}"
echo ""

# =====================================================================
# PASO 1: Detener Servicios
# =====================================================================
echo -e "${BLUE}[1/6] Deteniendo servicios...${NC}"

if systemctl --version &> /dev/null && [ "$EUID" -eq 0 ]; then
    # Detener servicio del servidor
    if systemctl is-active --quiet mmorpg-server.service; then
        echo "  → Deteniendo mmorpg-server.service..."
        systemctl stop mmorpg-server.service
    fi
    
    # Detener servicio del panel web
    if systemctl is-active --quiet mmorpg-web.service; then
        echo "  → Deteniendo mmorpg-web.service..."
        systemctl stop mmorpg-web.service
    fi
    
    echo -e "${GREEN}✓ Servicios detenidos${NC}"
else
    echo "  → Deteniendo procesos manualmente..."
    pkill -f "paper.*jar" || true
    pkill -f "flask.*app.py" || true
    echo -e "${GREEN}✓ Procesos detenidos${NC}"
fi

# =====================================================================
# PASO 2: Crear Backup (opcional)
# =====================================================================
echo ""
echo -e "${BLUE}[2/6] Backup de datos...${NC}"

if confirm "¿Deseas crear un backup antes de desinstalar?"; then
    BACKUP_DIR="$HOME/minecraft-mmorpg-backup-$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$BACKUP_DIR"
    
    echo "  → Creando backup en: $BACKUP_DIR"
    
    # Copiar mundos
    if [ -n "$SERVER_DIR" ] && [ -d "$SERVER_DIR/worlds" ]; then
        echo "    • Copiando mundos..."
        cp -r "$SERVER_DIR/worlds" "$BACKUP_DIR/" || true
    elif [ -n "$SERVER_DIR" ] && [ -d "$SERVER_DIR/world" ]; then
        echo "    • Copiando mundos (legacy)..."
        cp -r "$SERVER_DIR/world" "$BACKUP_DIR/" || true
    fi
    
    # Copiar base de datos
    if [ -n "$SERVER_DIR" ] && [ -f "$SERVER_DIR/config/data/universal.db" ]; then
        echo "    • Copiando base de datos..."
        mkdir -p "$BACKUP_DIR/data"
        cp "$SERVER_DIR/config/data/universal.db" "$BACKUP_DIR/data/" || true
    elif [ -n "$SERVER_DIR" ] && [ -f "$SERVER_DIR/data/universal.db" ]; then
        echo "    • Copiando base de datos (legacy)..."
        mkdir -p "$BACKUP_DIR/data"
        cp "$SERVER_DIR/data/universal.db" "$BACKUP_DIR/data/" || true
    elif [ -f "$INSTALL_DIR/data/universal.db" ]; then
        echo "    • Copiando base de datos (legacy)..."
        mkdir -p "$BACKUP_DIR/data"
        cp "$INSTALL_DIR/data/universal.db" "$BACKUP_DIR/data/" || true
    fi
    
    # Copiar configuraciones
    if [ -n "$SERVER_DIR" ] && [ -d "$SERVER_DIR/config" ]; then
        echo "    • Copiando configuraciones..."
        cp -r "$SERVER_DIR/config" "$BACKUP_DIR/" || true
    elif [ -n "$SERVER_DIR" ]; then
        echo "    • Copiando configuraciones (legacy)..."
        cp -r "$SERVER_DIR"/config.yml "$BACKUP_DIR/" 2>/dev/null || true
        cp -r "$SERVER_DIR"/*.json "$BACKUP_DIR/" 2>/dev/null || true
        if [ -d "$SERVER_DIR/data" ]; then
            cp -r "$SERVER_DIR/data" "$BACKUP_DIR/" || true
        fi
    elif [ -d "$INSTALL_DIR/config" ]; then
        echo "    • Copiando configuraciones (legacy)..."
        cp -r "$INSTALL_DIR/config" "$BACKUP_DIR/" || true
    fi
    
    # Comprimir backup
    echo "  → Comprimiendo backup..."
    tar -czf "$BACKUP_DIR.tar.gz" -C "$BACKUP_DIR" . 2>/dev/null || true
    rm -rf "$BACKUP_DIR"
    
    echo -e "${GREEN}✓ Backup creado: $BACKUP_DIR.tar.gz${NC}"
else
    echo "  → Backup omitido"
fi

# =====================================================================
# PASO 3: Eliminar Servicios Systemd
# =====================================================================
echo ""
echo -e "${BLUE}[3/6] Eliminando servicios systemd...${NC}"

if systemctl --version &> /dev/null && [ "$EUID" -eq 0 ]; then
    # Deshabilitar servicios
    if systemctl list-unit-files | grep -q "mmorpg-server.service"; then
        echo "  → Deshabilitando mmorpg-server.service..."
        systemctl disable mmorpg-server.service || true
        rm -f /etc/systemd/system/mmorpg-server.service
    fi
    
    if systemctl list-unit-files | grep -q "mmorpg-web.service"; then
        echo "  → Deshabilitando mmorpg-web.service..."
        systemctl disable mmorpg-web.service || true
        rm -f /etc/systemd/system/mmorpg-web.service
    fi
    
    # Recargar systemd
    systemctl daemon-reload
    
    echo -e "${GREEN}✓ Servicios systemd eliminados${NC}"
else
    echo "  → Servicios systemd no disponibles o sin permisos"
fi

# =====================================================================
# PASO 4: Eliminar Directorios
# =====================================================================
echo ""
echo -e "${BLUE}[4/6] Eliminando archivos del sistema...${NC}"

if [ -d "$INSTALL_DIR" ]; then
    echo "  → Eliminando directorio: $INSTALL_DIR"
    
    if [ "$EUID" -eq 0 ]; then
        rm -rf "$INSTALL_DIR"
    else
        # Si no es root, intentar eliminar con permisos actuales
        rm -rf "$INSTALL_DIR" 2>/dev/null || {
            echo -e "${YELLOW}⚠️  No se pudo eliminar $INSTALL_DIR - puede requerir sudo${NC}"
            echo "Ejecuta: sudo rm -rf $INSTALL_DIR"
        }
    fi
    
    echo -e "${GREEN}✓ Archivos eliminados${NC}"
else
    echo "  → Directorio no encontrado: $INSTALL_DIR"
fi

# =====================================================================
# PASO 5: Eliminar Usuario del Sistema (opcional)
# =====================================================================
echo ""
echo -e "${BLUE}[5/6] Usuario del sistema...${NC}"

if id "$SERVICE_USER" &>/dev/null && [ "$EUID" -eq 0 ]; then
    if confirm "¿Deseas eliminar el usuario '$SERVICE_USER'?"; then
        echo "  → Eliminando usuario $SERVICE_USER..."
        userdel -r "$SERVICE_USER" 2>/dev/null || userdel "$SERVICE_USER" 2>/dev/null || true
        echo -e "${GREEN}✓ Usuario eliminado${NC}"
    else
        echo "  → Usuario mantenido"
    fi
else
    echo "  → Usuario no existe o sin permisos para eliminarlo"
fi

# =====================================================================
# PASO 6: Limpiar Symlinks
# =====================================================================
echo ""
echo -e "${BLUE}[6/6] Limpiando symlinks...${NC}"

# Buscar y eliminar symlinks rotos
echo "  → Buscando symlinks rotos en $HOME..."
find "$HOME" -maxdepth 3 -type l ! -exec test -e {} \; -print 2>/dev/null | while read -r link; do
    if [[ "$link" == *"minecraft"* ]] || [[ "$link" == *"mmorpg"* ]]; then
        echo "    • Eliminando: $link"
        rm -f "$link" || true
    fi
done

echo -e "${GREEN}✓ Symlinks limpiados${NC}"

# =====================================================================
# FINALIZACIÓN
# =====================================================================
echo ""
echo -e "${GREEN}"
echo "═══════════════════════════════════════════════════════════════"
echo "  ✅ DESINSTALACIÓN COMPLETADA"
echo "═══════════════════════════════════════════════════════════════"
echo -e "${NC}"

if [ -f "$BACKUP_DIR.tar.gz" ]; then
    echo -e "${BLUE}📦 Backup guardado en:${NC}"
    echo "   $BACKUP_DIR.tar.gz"
    echo ""
fi

echo -e "${BLUE}📝 Resumen:${NC}"
echo "   • Servicios detenidos y eliminados"
echo "   • Archivos del sistema eliminados"
echo "   • Configuración limpiada"
echo ""

echo -e "${YELLOW}ℹ️  Notas:${NC}"
echo "   • Si deseas reinstalar, ejecuta: ./install-native.sh"
echo "   • Para restaurar backup: tar -xzf backup.tar.gz"
echo ""

echo -e "${GREEN}¡Gracias por usar el Sistema MMORPG para Minecraft!${NC}"
echo ""

exit 0
