#!/bin/bash

###############################################################################
# Script de Backup - Sistema MMORPG Minecraft
# Crea backups completos del servidor, mundos, configuraciones y base de datos
###############################################################################

set -e

# Colores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Configuración
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="${MINECRAFT_INSTALL_DIR:-$SCRIPT_DIR/../server}"
SERVER_DIR="$BASE_DIR"

if [ -d "$BASE_DIR/server" ]; then
    SERVER_DIR="$BASE_DIR/server"
elif [ -d "$BASE_DIR/minecraft-server" ]; then
    SERVER_DIR="$BASE_DIR/minecraft-server"
fi
BACKUP_BASE_DIR="${BACKUP_DIR:-$HOME/minecraft-backups}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_NAME="mmorpg-backup-$TIMESTAMP"
BACKUP_DIR="$BACKUP_BASE_DIR/$BACKUP_NAME"

# Crear directorio de backups
mkdir -p "$BACKUP_DIR"

echo -e "${BLUE}"
echo "═══════════════════════════════════════════════════════════════"
echo "  💾 BACKUP - SISTEMA MMORPG MINECRAFT"
echo "═══════════════════════════════════════════════════════════════"
echo -e "${NC}"

echo "📍 Directorio de instalación: $SERVER_DIR"
echo "📦 Directorio de backup: $BACKUP_DIR"
echo ""

# Función para calcular tamaño
get_size() {
    du -sh "$1" 2>/dev/null | cut -f1 || echo "0"
}

# Función para copiar con progreso
backup_item() {
    local source=$1
    local dest=$2
    local name=$3
    
    if [ -e "$source" ]; then
        echo -n "  → Copiando $name... "
        cp -r "$source" "$dest/" 2>/dev/null || {
            echo -e "${YELLOW}⚠️  Error copiando $name${NC}"
            return 1
        }
        local size=$(get_size "$dest/$(basename $source)")
        echo -e "${GREEN}✓ ($size)${NC}"
        return 0
    else
        echo -e "  → $name no encontrado, omitiendo"
        return 1
    fi
}

# =====================================================================
# BACKUP DE MUNDOS
# =====================================================================
echo -e "${BLUE}[1/5] Backup de mundos...${NC}"

mkdir -p "$BACKUP_DIR/worlds"

backup_item "$SERVER_DIR/world" "$BACKUP_DIR/worlds" "Mundo principal"
backup_item "$SERVER_DIR/world_nether" "$BACKUP_DIR/worlds" "Nether"
backup_item "$SERVER_DIR/world_the_end" "$BACKUP_DIR/worlds" "The End"

# Backup de mundos adicionales en worlds/
if [ -d "$SERVER_DIR/worlds" ]; then
    for world in "$SERVER_DIR/worlds"/*; do
        if [ -d "$world" ]; then
            backup_item "$world" "$BACKUP_DIR/worlds" "$(basename $world)"
        fi
    done
fi

# =====================================================================
# BACKUP DE BASE DE DATOS
# =====================================================================
echo ""
echo -e "${BLUE}[2/5] Backup de base de datos...${NC}"

mkdir -p "$BACKUP_DIR/data"

backup_item "$SERVER_DIR/config/data/universal.db" "$BACKUP_DIR/data" "Base de datos universal"

# Backup de bases de datos de mundos
if [ -d "$SERVER_DIR/config/data" ]; then
    for db in "$SERVER_DIR/config/data"/*.db; do
        if [ -f "$db" ] && [ "$(basename $db)" != "universal.db" ]; then
            backup_item "$db" "$BACKUP_DIR/data" "$(basename $db)"
        fi
    done
fi

# =====================================================================
# BACKUP DE CONFIGURACIONES
# =====================================================================
echo ""
echo -e "${BLUE}[3/5] Backup de configuraciones...${NC}"

mkdir -p "$BACKUP_DIR/config"

backup_item "$SERVER_DIR/config" "$BACKUP_DIR" "Configuraciones del plugin"
backup_item "$SERVER_DIR/server.properties" "$BACKUP_DIR/config" "server.properties"
backup_item "$SERVER_DIR/bukkit.yml" "$BACKUP_DIR/config" "bukkit.yml"
backup_item "$SERVER_DIR/spigot.yml" "$BACKUP_DIR/config" "spigot.yml"
backup_item "$SERVER_DIR/paper.yml" "$BACKUP_DIR/config" "paper.yml"

# =====================================================================
# BACKUP DE PLUGINS
# =====================================================================
echo ""
echo -e "${BLUE}[4/5] Backup de plugins...${NC}"

mkdir -p "$BACKUP_DIR/plugins"

backup_item "$SERVER_DIR/plugins" "$BACKUP_DIR" "Plugins"

# =====================================================================
# BACKUP DE LOGS (últimos 7 días)
# =====================================================================
echo ""
echo -e "${BLUE}[5/5] Backup de logs...${NC}"

mkdir -p "$BACKUP_DIR/logs"

if [ -d "$SERVER_DIR/logs" ]; then
    echo "  → Copiando logs recientes..."
    find "$SERVER_DIR/logs" -name "*.log*" -mtime -7 -exec cp {} "$BACKUP_DIR/logs/" \; 2>/dev/null || true
    local log_size=$(get_size "$BACKUP_DIR/logs")
    echo -e "${GREEN}✓ Logs copiados ($log_size)${NC}"
fi

# =====================================================================
# COMPRIMIR BACKUP
# =====================================================================
echo ""
echo -e "${BLUE}Comprimiendo backup...${NC}"

cd "$BACKUP_BASE_DIR"
tar -czf "$BACKUP_NAME.tar.gz" "$BACKUP_NAME" 2>/dev/null

if [ $? -eq 0 ]; then
    # Eliminar directorio sin comprimir
    rm -rf "$BACKUP_NAME"
    
    # Calcular tamaño del archivo comprimido
    COMPRESSED_SIZE=$(get_size "$BACKUP_NAME.tar.gz")
    
    echo -e "${GREEN}✓ Backup comprimido: $BACKUP_NAME.tar.gz ($COMPRESSED_SIZE)${NC}"
else
    echo -e "${YELLOW}⚠️  Error al comprimir backup${NC}"
fi

# =====================================================================
# LIMPIAR BACKUPS ANTIGUOS (opcional)
# =====================================================================
echo ""
echo -e "${BLUE}Limpiando backups antiguos...${NC}"

# Mantener solo los últimos 7 backups
KEEP_BACKUPS=7
BACKUP_COUNT=$(ls -1 "$BACKUP_BASE_DIR"/mmorpg-backup-*.tar.gz 2>/dev/null | wc -l)

if [ "$BACKUP_COUNT" -gt "$KEEP_BACKUPS" ]; then
    echo "  → Eliminando backups antiguos (manteniendo últimos $KEEP_BACKUPS)..."
    ls -1t "$BACKUP_BASE_DIR"/mmorpg-backup-*.tar.gz | tail -n +$((KEEP_BACKUPS + 1)) | xargs rm -f
    echo -e "${GREEN}✓ Backups antiguos eliminados${NC}"
else
    echo "  → No hay backups antiguos para eliminar"
fi

# =====================================================================
# CREAR REGISTRO DE BACKUP
# =====================================================================
cat > "$BACKUP_BASE_DIR/backup-$TIMESTAMP.log" <<EOF
Backup realizado: $(date)
Nombre: $BACKUP_NAME.tar.gz
Tamaño: $COMPRESSED_SIZE
Ubicación: $BACKUP_BASE_DIR/$BACKUP_NAME.tar.gz

Contenido:
- Mundos del servidor
- Base de datos SQLite
- Configuraciones
- Plugins
- Logs recientes (últimos 7 días)

Para restaurar:
  tar -xzf $BACKUP_NAME.tar.gz
  ./restore-backup.sh $BACKUP_NAME
EOF

# =====================================================================
# FINALIZACIÓN
# =====================================================================
echo ""
echo -e "${GREEN}"
echo "═══════════════════════════════════════════════════════════════"
echo "  ✅ BACKUP COMPLETADO"
echo "═══════════════════════════════════════════════════════════════"
echo -e "${NC}"

echo -e "${BLUE}📦 Archivo de backup:${NC}"
echo "   $BACKUP_BASE_DIR/$BACKUP_NAME.tar.gz"
echo "   Tamaño: $COMPRESSED_SIZE"
echo ""

echo -e "${BLUE}📋 Registro de backup:${NC}"
echo "   $BACKUP_BASE_DIR/backup-$TIMESTAMP.log"
echo ""

echo -e "${BLUE}💡 Para restaurar este backup:${NC}"
echo "   ./restore-backup.sh $BACKUP_NAME"
echo ""

exit 0
