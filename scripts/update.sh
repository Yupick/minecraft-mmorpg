#!/bin/bash

###############################################################################
# Script de Actualización - Sistema MMORPG Minecraft
# Actualiza el sistema desde el repositorio Git
###############################################################################

set -e

# Colores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BASE_DIR="${MINECRAFT_INSTALL_DIR:-$REPO_DIR/server}"
SERVER_DIR="$BASE_DIR"
if [ -d "$BASE_DIR/server" ]; then
    SERVER_DIR="$BASE_DIR/server"
elif [ -d "$BASE_DIR/minecraft-server" ]; then
    SERVER_DIR="$BASE_DIR/minecraft-server"
fi

echo -e "${BLUE}"
echo "═══════════════════════════════════════════════════════════════"
echo "  🔄 ACTUALIZACIÓN - SISTEMA MMORPG MINECRAFT"
echo "═══════════════════════════════════════════════════════════════"
echo -e "${NC}"

echo "📍 Repositorio: $REPO_DIR"
echo "📍 Instalación: $SERVER_DIR"
echo ""

# =====================================================================
# PASO 1: Hacer Backup Antes de Actualizar
# =====================================================================
echo -e "${BLUE}[1/5] Creando backup antes de actualizar...${NC}"

if [ -f "$REPO_DIR/scripts/backup.sh" ]; then
    bash "$REPO_DIR/scripts/backup.sh"
else
    echo -e "${YELLOW}⚠️  Script de backup no encontrado, continuando sin backup${NC}"
fi

# =====================================================================
# PASO 2: Actualizar Código desde Git
# =====================================================================
echo ""
echo -e "${BLUE}[2/5] Actualizando código desde Git...${NC}"

cd "$REPO_DIR"

# Verificar si hay cambios locales
if ! git diff-index --quiet HEAD --; then
    echo -e "${YELLOW}⚠️  Hay cambios locales no commitidos${NC}"
    echo "  → Guardando cambios locales..."
    git stash
    STASHED=true
else
    STASHED=false
fi

# Actualizar desde origin
echo "  → Obteniendo últimos cambios..."
git fetch origin

# Obtener rama actual
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "  → Rama actual: $CURRENT_BRANCH"

# Hacer pull
echo "  → Actualizando..."
git pull origin "$CURRENT_BRANCH"

# Restaurar cambios si se guardaron
if [ "$STASHED" = true ]; then
    echo "  → Restaurando cambios locales..."
    git stash pop || true
fi

echo -e "${GREEN}✓ Código actualizado${NC}"

# =====================================================================
# PASO 3: Recompilar Plugin
# =====================================================================
echo ""
echo -e "${BLUE}[3/5] Recompilando plugin...${NC}"

if [ -f "$REPO_DIR/build.sh" ]; then
    cd "$REPO_DIR"
    bash build.sh
else
    # Compilar manualmente con Maven
    cd "$REPO_DIR/mmorpg-plugin"
    
    if command -v mvn &> /dev/null; then
        echo "  → Compilando con Maven..."
        mvn clean package -DskipTests
        
        if [ -f "target/mmorpg-plugin-1.0.0.jar" ]; then
            echo -e "${GREEN}✓ Plugin compilado${NC}"
        else
            echo -e "${YELLOW}⚠️  Advertencia: JAR no encontrado${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  Maven no encontrado, omitiendo compilación${NC}"
    fi
fi

# =====================================================================
# PASO 4: Copiar Archivos Actualizados
# =====================================================================
echo ""
echo -e "${BLUE}[4/5] Copiando archivos actualizados...${NC}"

# Copiar plugin compilado
if [ -f "$REPO_DIR/mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar" ]; then
    echo "  → Copiando plugin..."
     mkdir -p "$SERVER_DIR/plugins"
    cp "$REPO_DIR/mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar" \
         "$SERVER_DIR/plugins/"
    echo -e "${GREEN}✓ Plugin actualizado${NC}"
fi

# Copiar configuraciones nuevas (sin sobrescribir)
echo "  → Actualizando configuraciones..."
if [ -d "$REPO_DIR/config" ]; then
    for config in "$REPO_DIR/config"/*.json "$REPO_DIR/config"/*.yml; do
        if [ -f "$config" ]; then
            filename=$(basename "$config")
            
            # Solo copiar si no existe en destino
            if [ ! -f "$SERVER_DIR/config/$filename" ]; then
                echo "    • Nueva configuración: $filename"
                cp "$config" "$SERVER_DIR/config/" || true
            fi
        fi
    done
fi

# Actualizar panel web
echo "  → Actualizando panel web..."
if [ -d "$REPO_DIR/web" ]; then
    mkdir -p "$SERVER_DIR/web"
    cp -r "$REPO_DIR/web"/*.py "$SERVER_DIR/web/" 2>/dev/null || true
    cp -r "$REPO_DIR/web/templates" "$SERVER_DIR/web/" 2>/dev/null || true
    cp -r "$REPO_DIR/web/static" "$SERVER_DIR/web/" 2>/dev/null || true
    
    # Actualizar dependencias de Python
    if [ -f "$REPO_DIR/web/requirements.txt" ]; then
        if [ -f "$SERVER_DIR/web/venv/bin/activate" ]; then
            source "$SERVER_DIR/web/venv/bin/activate"
            pip install -r "$REPO_DIR/web/requirements.txt" --upgrade
            deactivate
        fi
    fi
fi

# Actualizar scripts
echo "  → Actualizando scripts..."
if [ -d "$REPO_DIR/scripts" ]; then
    mkdir -p "$SERVER_DIR/scripts"
    cp -r "$REPO_DIR/scripts"/*.sh "$SERVER_DIR/scripts/" 2>/dev/null || true
    chmod +x "$SERVER_DIR/scripts"/*.sh 2>/dev/null || true
fi

echo -e "${GREEN}✓ Archivos actualizados${NC}"

# =====================================================================
# PASO 5: Reiniciar Servicios
# =====================================================================
echo ""
echo -e "${BLUE}[5/5] Reiniciando servicios...${NC}"

if systemctl --version &> /dev/null && [ "$EUID" -eq 0 ]; then
    echo "  → Reiniciando mmorpg-server.service..."
    systemctl restart mmorpg-server.service || true
    
    echo "  → Reiniciando mmorpg-web.service..."
    systemctl restart mmorpg-web.service || true
    
    echo -e "${GREEN}✓ Servicios reiniciados${NC}"
else
    echo -e "${YELLOW}  → Debes reiniciar los servicios manualmente:${NC}"
    echo "     systemctl restart mmorpg-server.service"
    echo "     systemctl restart mmorpg-web.service"
fi

# =====================================================================
# FINALIZACIÓN
# =====================================================================
echo ""
echo -e "${GREEN}"
echo "═══════════════════════════════════════════════════════════════"
echo "  ✅ ACTUALIZACIÓN COMPLETADA"
echo "═══════════════════════════════════════════════════════════════"
echo -e "${NC}"

# Mostrar cambios
echo -e "${BLUE}📝 Últimos cambios:${NC}"
cd "$REPO_DIR"
git log -5 --oneline --decorate
echo ""

echo -e "${YELLOW}ℹ️  Próximos pasos:${NC}"
echo "   1. Verifica los logs del servidor"
echo "   2. Comprueba que el plugin cargó correctamente"
echo "   3. Accede al panel web para verificar funcionalidad"
echo ""

exit 0
