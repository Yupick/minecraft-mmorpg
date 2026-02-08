#!/bin/bash

# ═══════════════════════════════════════════════════════════════
# Script de Actualización - Sistema MMORPG Minecraft
# Actualiza el servidor desde el repositorio de GitHub
# ═══════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="$SCRIPT_DIR/server"
INSTALL_DIR="$SCRIPT_DIR/mmorpg-plugin"
BACKUP_DIR="$SCRIPT_DIR/backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

echo "═══════════════════════════════════════════════════════════════"
echo "  Actualización - Sistema MMORPG Minecraft"
echo "  Repositorio: https://github.com/Yupick/minecraft-mmorpg"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Funciones auxiliares
confirm() {
    while true; do
        read -r -p "$1 (s/n): " yn
        case $yn in
            [Ss]* ) return 0;;
            [Nn]* ) return 1;;
            * ) echo "Por favor, responde s o n.";;
        esac
    done
}

print_step() {
    echo "▶ $1"
}

print_success() {
    echo "✓ $1"
}

print_error() {
    echo "✗ ERROR: $1"
}

# Validaciones iniciales
print_step "Validando entorno..."

if [ ! -d "$SCRIPT_DIR/.git" ]; then
    print_error "No se encontró repositorio git en $SCRIPT_DIR"
    exit 1
fi

if ! command -v git &> /dev/null; then
    print_error "Git no está instalado"
    exit 1
fi

print_success "Entorno validado"
echo ""

# Mostrar rama y remoto actual
echo "📍 Estado actual:"
BRANCH=$(git -C "$SCRIPT_DIR" branch --show-current)
REMOTE=$(git -C "$SCRIPT_DIR" config --get remote.origin.url)
echo "  • Rama actual: $BRANCH"
echo "  • Remoto: $REMOTE"
echo ""

# Comprobar cambios sin guardar
print_step "Verificando cambios locales..."
if ! git -C "$SCRIPT_DIR" diff-index --quiet HEAD --; then
    echo "⚠ Hay cambios sin confirmar en la rama local:"
    git -C "$SCRIPT_DIR" status -sb
    echo ""
    
    if confirm "¿Descartar cambios locales?"; then
        git -C "$SCRIPT_DIR" checkout . 2>/dev/null || true
        git -C "$SCRIPT_DIR" clean -fd 2>/dev/null || true
        print_success "Cambios descartados"
    else
        print_error "Operación cancelada - hay cambios sin guardar"
        exit 1
    fi
fi
print_success "Sin cambios pendientes"
echo ""

# Crear backup antes de actualizar
print_step "Creando backup..."
mkdir -p "$BACKUP_DIR"
BACKUP_FILE="$BACKUP_DIR/backup_$TIMESTAMP.tar.gz"

# Backup del servidor
if [ -d "$SERVER_DIR" ]; then
    tar -czf "$BACKUP_FILE" \
        --exclude="$SERVER_DIR/logs" \
        --exclude="$SERVER_DIR/cache" \
        "$SERVER_DIR" 2>/dev/null || true
    echo "  • Servidor: ✓"
else
    echo "  • Servidor: (no existe, saltando)"
fi

# Backup del plugin compilado
if [ -f "$INSTALL_DIR/target/mmorpg-plugin-1.0.0.jar" ]; then
    tar -czf "$BACKUP_FILE" "$INSTALL_DIR/target/" 2>/dev/null || true
    echo "  • Plugin compilado: ✓"
fi

print_success "Backup creado: $BACKUP_FILE"
echo ""

# Obtener cambios del remoto
print_step "Descargando cambios de GitHub..."
git -C "$SCRIPT_DIR" fetch origin "$BRANCH" || {
    print_error "No se pudieron descargar cambios del remoto"
    exit 1
}
print_success "Cambios descargados"
echo ""

# Mostrar cambios disponibles
COMMITS_BEHIND=$(git -C "$SCRIPT_DIR" log HEAD..origin/"$BRANCH" --oneline 2>/dev/null | wc -l)
if [ "$COMMITS_BEHIND" -eq 0 ]; then
    echo "✓ Ya está actualizado a la versión más reciente"
    echo ""
    exit 0
fi

echo "📋 Cambios disponibles ($COMMITS_BEHIND commits):"
git -C "$SCRIPT_DIR" log HEAD..origin/"$BRANCH" --oneline | head -5
if [ "$COMMITS_BEHIND" -gt 5 ]; then
    echo "  ... y $(($COMMITS_BEHIND - 5)) más"
fi
echo ""

# Confirmar actualización
if ! confirm "¿Aplicar actualización?"; then
    print_error "Actualización cancelada"
    exit 0
fi
echo ""

# Detener servidor si está corriendo
print_step "Deteniendo servicios..."
if systemctl is-active --quiet mmorpg-server.service 2>/dev/null; then
    sudo systemctl stop mmorpg-server.service
    echo "  • Servidor detenido: ✓"
elif [ -f "$SERVER_DIR/stop-server.sh" ]; then
    bash "$SERVER_DIR/stop-server.sh" 2>/dev/null || true
    echo "  • Servidor detenido: (intento)"
fi

if systemctl is-active --quiet mmorpg-web.service 2>/dev/null; then
    sudo systemctl stop mmorpg-web.service
    echo "  • Panel web detenido: ✓"
elif [ -f "$SERVER_DIR/stop-web.sh" ]; then
    bash "$SERVER_DIR/stop-web.sh" 2>/dev/null || true
    echo "  • Panel web detenido: (intento)"
fi

sleep 2
print_success "Servicios detenidos"
echo ""

# Actualizar código
print_step "Actualizando código fuente..."
git -C "$SCRIPT_DIR" pull origin "$BRANCH" || {
    print_error "Fallo al actualizar. Restaurando backup..."
    git -C "$SCRIPT_DIR" reset --hard HEAD
    exit 1
}
print_success "Código actualizado"
echo ""

# Recompilar plugin
print_step "Recompilando plugin..."
cd "$INSTALL_DIR"
if ! mvn -q clean package -DskipTests; then
    print_error "Fallo la compilación del plugin"
    print_step "Restaurando backup..."
    git -C "$SCRIPT_DIR" reset --hard HEAD
    exit 1
fi

# Copiar JAR al servidor
if [ -d "$SERVER_DIR/plugins" ]; then
    cp "$INSTALL_DIR/target/mmorpg-plugin-*.jar" "$SERVER_DIR/plugins/"
    print_success "Plugin compilado e instalado"
else
    print_error "Directorio de plugins no encontrado"
fi
echo ""

# Mostrar cambios aplicados
COMMITS_UPDATED=$(git -C "$SCRIPT_DIR" log origin/"$BRANCH"..HEAD --oneline 2>/dev/null | wc -l)
echo "📝 Cambios aplicados:"
git -C "$SCRIPT_DIR" log -n 5 --oneline
echo ""

# Reiniciar servicios
print_step "Reiniciando servicios..."

if systemctl --version &> /dev/null && systemctl list-unit-files | grep -q "mmorpg-server.service"; then
    sudo systemctl start mmorpg-server.service 2>/dev/null && {
        echo "  • Servidor iniciado: ✓"
    } || {
        if [ -f "$SERVER_DIR/start-server.sh" ]; then
            bash "$SERVER_DIR/start-server.sh" &
            echo "  • Servidor iniciado: (background)"
        fi
    }
elif [ -f "$SERVER_DIR/start-server.sh" ]; then
    bash "$SERVER_DIR/start-server.sh" &
    echo "  • Servidor iniciado: (background)"
fi

if systemctl --version &> /dev/null && systemctl list-unit-files | grep -q "mmorpg-web.service"; then
    sudo systemctl start mmorpg-web.service 2>/dev/null && {
        echo "  • Panel web iniciado: ✓"
    } || {
        if [ -f "$SERVER_DIR/start-web.sh" ]; then
            bash "$SERVER_DIR/start-web.sh" &
            echo "  • Panel web iniciado: (background)"
        fi
    }
elif [ -f "$SERVER_DIR/start-web.sh" ]; then
    bash "$SERVER_DIR/start-web.sh" &
    echo "  • Panel web iniciado: (background)"
fi

sleep 3
print_success "Servicios reiniciados"
echo ""

echo "═══════════════════════════════════════════════════════════════"
echo "  ✅ Actualización completada con éxito"
echo "═══════════════════════════════════════════════════════════════"
echo ""
echo "📊 Resumen:"
echo "  • Rama: $BRANCH"
echo "  • Cambios: $COMMITS_UPDATED commits"
echo "  • Backup: $BACKUP_FILE"
echo "  • Plugin: Recompilado e instalado"
echo ""
echo "ℹ️  Notas:"
echo "  • Para ver logs: $SERVER_DIR/logs-server.sh"
echo "  • Para detener: $SERVER_DIR/stop-server.sh"
echo "  • Para reiniciar: $SERVER_DIR/restart-server.sh"
echo ""
