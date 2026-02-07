#!/bin/bash

# ═══════════════════════════════════════════════════════════════
# Script de compilación del plugin MMORPG
# ═══════════════════════════════════════════════════════════════

set -e

PLUGIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/mmorpg-plugin"

echo "═══════════════════════════════════════════════════════════════"
echo "  Compilando Plugin MMORPG"
echo "═══════════════════════════════════════════════════════════════"

cd "$PLUGIN_DIR"

echo "[1/3] Limpiando compilaciones anteriores..."
mvn clean

echo "[2/3] Compilando plugin..."
mvn package -DskipTests

echo "[3/3] Verificando JAR generado..."
if [ -f "target/mmorpg-plugin-1.0-SNAPSHOT.jar" ]; then
    echo "✅ Plugin compilado exitosamente: target/mmorpg-plugin-1.0-SNAPSHOT.jar"
    ls -lh target/mmorpg-plugin-1.0-SNAPSHOT.jar
else
    echo "❌ Error: JAR no encontrado"
    exit 1
fi

echo "═══════════════════════════════════════════════════════════════"
echo "  Compilación completada!"
echo "  El plugin está listo en: target/mmorpg-plugin-1.0-SNAPSHOT.jar"
echo "═══════════════════════════════════════════════════════════════"
