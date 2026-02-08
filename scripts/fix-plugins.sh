#!/bin/bash

# ═══════════════════════════════════════════════════════════════
# Script de Reparación de Plugins - Descarga de plugins corruptos
# ═══════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="$SCRIPT_DIR/../server"
PLUGINS_DIR="$SERVER_DIR/plugins"

echo "═══════════════════════════════════════════════════════════════"
echo "  Reparación de Plugins Corruptos"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Helper function to validate JAR file
validate_jar() {
    if [ -f "$1" ] && [ -s "$1" ]; then
        file "$1" | grep -q "Zip data" && return 0
        return 1
    fi
    return 1
}

# Helper function to download with retries
download_plugin() {
    local name="$1"
    local output="$2"
    shift 2
    local urls=("$@")
    
    echo "Descargando $name..."
    
    for url in "${urls[@]}"; do
        echo "  Intentando: $url"
        if wget -q -O "$output" "$url" 2>/dev/null && validate_jar "$output"; then
            size=$(du -h "$output" | cut -f1)
            echo "  ✓ Éxito: $size"
            return 0
        fi
    done
    
    echo "  ✗ No se pudo descargar $name"
    rm -f "$output"
    return 1
}

mkdir -p "$PLUGINS_DIR"
cd "$PLUGINS_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "Analizando plugins corruptos..."
echo ""

# Check each plugin
plugins_failed=0

# Geyser-Spigot
if ! validate_jar "Geyser-Spigot.jar" 2>/dev/null; then
    echo -e "${YELLOW}⚠ Geyser-Spigot.jar está corrupto o no existe${NC}"
    download_plugin "Geyser-Spigot" "$PLUGINS_DIR/Geyser-Spigot.jar" \
        "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot" \
        "https://ci.opencollab.dev/job/GeyserMC/job/Geyser/job/master/lastSuccessfulBuild/artifact/bootstrap/spigot/build/libs/Geyser-Spigot.jar" || \
        ((plugins_failed++))
fi

# Floodgate-Spigot
if ! validate_jar "floodgate-spigot.jar" 2>/dev/null; then
    echo -e "${YELLOW}⚠ floodgate-spigot.jar está corrupto o no existe${NC}"
    download_plugin "Floodgate-Spigot" "$PLUGINS_DIR/floodgate-spigot.jar" \
        "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot" \
        "https://ci.opencollab.dev/job/GeyserMC/job/Floodgate/job/master/lastSuccessfulBuild/artifact/spigot/build/libs/floodgate-spigot.jar" || \
        ((plugins_failed++))
fi

# ViaVersion
if ! validate_jar "ViaVersion.jar" 2>/dev/null; then
    echo -e "${YELLOW}⚠ ViaVersion.jar está corrupto o no existe${NC}"
    download_plugin "ViaVersion" "$PLUGINS_DIR/ViaVersion.jar" \
        "https://download.viaversion.com/ViaVersion.jar" \
        "https://hangar.papermc.io/api/v1/projects/ViaVersion/versions/LATEST/downloads/ViaVersion.jar" || \
        ((plugins_failed++))
fi

# ViaBackwards
if ! validate_jar "ViaBackwards.jar" 2>/dev/null; then
    echo -e "${YELLOW}⚠ ViaBackwards.jar está corrupto o no existe${NC}"
    download_plugin "ViaBackwards" "$PLUGINS_DIR/ViaBackwards.jar" \
        "https://download.viaversion.com/ViaBackwards.jar" \
        "https://hangar.papermc.io/api/v1/projects/ViaBackwards/versions/LATEST/downloads/ViaBackwards.jar" || \
        ((plugins_failed++))
fi

# ViaRewind
if ! validate_jar "ViaRewind.jar" 2>/dev/null; then
    echo -e "${YELLOW}⚠ ViaRewind.jar está corrupto o no existe${NC}"
    download_plugin "ViaRewind" "$PLUGINS_DIR/ViaRewind.jar" \
        "https://download.viaversion.com/ViaRewind.jar" \
        "https://hangar.papermc.io/api/v1/projects/ViaRewind/versions/LATEST/downloads/ViaRewind.jar" || \
        ((plugins_failed++))
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "  Plugins disponibles:"
echo "═══════════════════════════════════════════════════════════════"
ls -lh *.jar 2>/dev/null | awk '{printf "  %-30s %8s\n", $9, $5}' || echo "  (ninguno encontrado)"
echo ""

if [ $plugins_failed -eq 0 ]; then
    echo -e "${GREEN}✅ Todos los plugins están correctos${NC}"
    echo ""
    echo "Pasos siguientes:"
    echo "  1. Detener el servidor: ./stop-server.sh"
    echo "  2. Reiniciar el servidor: ./start-server.sh"
    echo ""
    exit 0
else
    echo -e "${RED}❌ $plugins_failed plugin(s) no se pudieron descargar${NC}"
    echo ""
    echo "Pasos siguientes:"
    echo "  1. Revisar conexión a Internet"
    echo "  2. Intentar de nuevo: $0"
    echo "  3. Descargar manualmente desde:"
    echo "     - Geyser: https://geysermc.org/download"
    echo "     - ViaVersion: https://hangar.papermc.io/ViaVersion/ViaVersion"
    echo ""
    exit 1
fi
