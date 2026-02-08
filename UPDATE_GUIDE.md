# Actualización desde GitHub

## Introducción

El script `update.sh` permite actualizar el servidor MMORPG desde el repositorio de GitHub de forma remota. Es útil para aplicar cambios en código, configuración y plugins sin necesidad de acceder al servidor de desarrollo.

## Características

✅ **Descarga cambios** desde GitHub  
✅ **Verifica estado** del repositorio antes de actualizar  
✅ **Crea backups** automáticos antes de aplicar cambios  
✅ **Recompila plugin** si hay cambios en código Java  
✅ **Detiene/reinicia** servicios automáticamente  
✅ **Manejo de errores** robusto con rollback automático  
✅ **Logging completo** de operaciones  

## Uso Básico

### En el servidor:

```bash
cd /ruta/al/servidor
./update.sh
```

El script te guiará con confirmaciones interactivas en cada paso.

## Flujo de Actualización

```
1. Validar entorno (git, repositorio, permisos)
2. Verificar cambios locales sin guardar
   ↓ Si hay: Descartar o cancelar
3. Crear backup del servidor y plugin compilado
4. Descargar cambios de GitHub (git fetch)
5. Mostrar commits disponibles
   ↓ Si no hay cambios: Finalizar
6. Confirmar actualización
7. Detener servicios (servidor y panel web)
8. Aplicar cambios (git pull)
9. Recompilar plugin (maven)
10. Instalar plugin compilado
11. Reiniciar servicios
12. Mostrar resumen
```

## Opciones del Script

El script es interactivo y pregunta por:

- **Descartar cambios locales**: Si hay cambios sin confirmar en git
- **Descartar cambios locales**: Confirmación antes de tirar cambios
- **Aplicar actualización**: Confirmación final antes de actualizar

## Backups

Los backups se guardan automáticamente en `backups/` con timestamp:

```
backups/
├── backup_20260208_143022.tar.gz
├── backup_20260208_152015.tar.gz
└── ...
```

Para restaurar un backup:

```bash
# Ver backups disponibles
ls -lh backups/

# Restaurar un backup específico
tar -xzf backups/backup_20260208_143022.tar.gz -C /
```

## Servicios Soportados

El script detecta automáticamente y controla:

- **Systemd services**:
  - `mmorpg-server.service`
  - `mmorpg-web.service`

- **Scripts directos** (si no hay systemd):
  - `server/start-server.sh`, `server/stop-server.sh`
  - `server/start-web.sh`, `server/stop-web.sh`

## Ejemplos de Uso

### Actualizar a la versión más reciente:

```bash
./update.sh

# Salida:
# ═══════════════════════════════════════════════════════════════
#   Actualización - Sistema MMORPG Minecraft
# ═══════════════════════════════════════════════════════════════
# 
# ▶ Validando entorno...
# ✓ Entorno validado
# 
# ▶ Descargando cambios de GitHub...
# ✓ Cambios descargados
# 
# 📋 Cambios disponibles (3 commits):
#   abc1234 Añadir nueva mecánica de inventario
#   def5678 Corregir bug en conquistas
#   ghi9012 Optimizar carga de chunks
# 
# ¿Aplicar actualización? (s/n): s
```

### Si el repositorio ya está actualizado:

```bash
./update.sh

# Salida:
# ✓ Ya está actualizado a la versión más reciente
```

### Si hay cambios locales sin guardar:

```bash
./update.sh

# Salida:
# ⚠ Hay cambios sin confirmar en la rama local:
# On branch main
# Changes not staged for commit:
#   modified:   config/config.yml
# 
# ¿Descartar cambios locales? (s/n): s
# ✓ Cambios descartados
```

## Monitoreo Después de Actualizar

### Ver logs del servidor:

```bash
./server/logs-server.sh
# o
tail -f server/logs/latest.log
```

### Ver estado del panel web:

```bash
./server/status-web.sh
```

### Verificar si el plugin está activo:

```bash
grep "Plugin enabled" server/logs/latest.log
```

## Solución de Problemas

### Error: "No se encontró repositorio git"

**Causa**: El script se ejecuta desde un directorio que no tiene `.git`  
**Solución**: Ejecutar desde el directorio raíz del proyecto:

```bash
cd /home/mkd/contenedores/minecraft-mmorpg
./update.sh
```

### Error: "Git no está instalado"

**Causa**: Git no está en el PATH  
**Solución**: Instalar git:

```bash
# Ubuntu/Debian
sudo apt install git

# CentOS/RHEL
sudo yum install git
```

### Error: "Fallo la compilación del plugin"

**Causa**: Problemas en el código Java o dependencias  
**Solución**:

```bash
# Ver detalles del error
cd mmorpg-plugin
mvn clean package

# Restaurar de backup si es necesario
tar -xzf backups/backup_TIMESTAMP.tar.gz -C /
```

### Error: "Fallo al actualizar. Restaurando backup..."

**Causa**: Conflictos en git o cambios remotos incompatibles  
**Solución**: Restaurar manualmente:

```bash
# Ver cambios remotos
git fetch origin main
git log HEAD..origin/main

# Forzar actualización (⚠ cuidado, perderá cambios locales)
git reset --hard origin/main
mvn clean package -DskipTests
```

### El servidor no reinicia después de actualizar

**Verificar**:

```bash
# Logs del servidor
./server/logs-server.sh

# Estado del servicio
systemctl status mmorpg-server.service

# Intentar reiniciar manualmente
./server/restart-server.sh
```

## Integración con CI/CD (Opcional)

Para automatizar actualizaciones en horarios específicos:

```bash
# Agregar a crontab (actualizar diariamente a las 3 AM)
0 3 * * * /home/mkd/contenedores/minecraft-mmorpg/update.sh >> /home/mkd/contenedores/minecraft-mmorpg/logs/update.log 2>&1
```

## Variantes del Script

### Actualizar rama específica:

```bash
# Editar el script para cambiar rama
# O crear alias:
alias update-dev="git -C /ruta/al/servidor checkout dev && ./update.sh"
```

### Actualización sin reiniciar:

```bash
# Descomentar fallback en el script para saltar reinicio
# (útil para testear cambios)
```

### Actualización forzada (descarta todos los cambios):

```bash
cd /ruta/al/servidor
git fetch origin main
git reset --hard origin/main
./mmorpg-plugin update.sh  # Luego ejecutar compilación
```

## Notas Importantes

⚠️ **Respaldos**: El script crea backups automáticos, pero no está de más hacer backup manual de bases de datos críticas.

⚠️ **Downtime**: El servidor se detiene durante la actualización (~30 segundos).

⚠️ **Cambios locales**: Si hay cambios locales en archivos de configuración, el script pedirá confirmación para descartarlos.

⚠️ **Permisos**: Necesita permisos para ejecutar `sudo systemctl`. Asegurar que el usuario que ejecuta el script tenga permisos sudo sin contraseña para los servicios.

## Configurar sudo sin contraseña (Opcional)

Para automatizar completamente (ej: con cron):

```bash
# Como root:
sudo visudo

# Agregar estas líneas al final:
%mmorpg ALL=(ALL) NOPASSWD: /bin/systemctl stop mmorpg-server.service
%mmorpg ALL=(ALL) NOPASSWD: /bin/systemctl start mmorpg-server.service
%mmorpg ALL=(ALL) NOPASSWD: /bin/systemctl stop mmorpg-web.service
%mmorpg ALL=(ALL) NOPASSWD: /bin/systemctl start mmorpg-web.service
```

## Ver También

- [Instalación](INSTALL_GUIDE.md)
- [Desinstalación](uninstall-native.sh)
- [Control de Servicios](server/README.md)
- [Logs y Debugging](scripts/)
