# ═══════════════════════════════════════════════════════════════
# INSTALACIÓN Y USO - MMORPG System
# ═══════════════════════════════════════════════════════════════

## 📋 Prerrequisitos

- **Java 21** (OpenJDK o Oracle JDK)
- **Maven 3.9+** (para compilar el plugin)
- **Python 3.12+** (para el panel web)
- **Linux** (Ubuntu/Debian recomendado)
- **4GB RAM mínimo** (8GB recomendado)

## 🚀 Instalación Rápida

### Opción 1: Instalación Automática (Recomendado)

```bash
# Dar permisos de ejecución
chmod +x install-native.sh

# Ejecutar instalación
sudo ./install-native.sh
```

Este script hará:
1. ✅ Verificar Java 21 y Maven
2. ✅ Descargar Paper 1.20.6 build 151
3. ✅ Compilar el plugin MMORPG
4. ✅ Copiar archivos de configuración
5. ✅ Crear entorno virtual Python
6. ✅ Instalar Flask y dependencias
7. ✅ Crear servicios systemd
8. ✅ Iniciar servidor y panel web

### Opción 2: Compilación Manual

```bash
# 1. Compilar el plugin
chmod +x build.sh
./build.sh

# 2. Crear directorio del servidor
mkdir -p server/plugins

# 3. Descargar Paper
cd server
wget https://api.papermc.io/v2/projects/paper/versions/1.20.6/builds/151/downloads/paper-1.20.6-151.jar

# 4. Aceptar EULA
echo "eula=true" > eula.txt

# 5. Copiar plugin y configs
cp ../mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar plugins/
mkdir -p config
cp -r ../config/* config/

# 6. Iniciar servidor (primera vez)
java -Xms4G -Xmx4G -jar paper-1.20.6-151.jar nogui
```

## 🎮 Primer Inicio

### 1. Iniciar Servidor Minecraft

```bash
# Con systemd (si usaste install-native.sh)
sudo systemctl start mmorpg-server
sudo systemctl status mmorpg-server

# Manual
cd server
java -Xms4G -Xmx4G -jar paper-1.20.6-151.jar nogui
```

### 2. Verificar Plugin

Conecta al servidor en `localhost:25565` y ejecuta:

```
/mmorpgadmin reload
```

Deberías ver el mensaje de confirmación.

### 3. Iniciar Panel Web

```bash
# Con systemd
sudo systemctl start mmorpg-web
sudo systemctl status mmorpg-web

# Manual
cd server/web
source venv/bin/activate
python app.py
```

Accede a: **http://localhost:5000**

**Credenciales por defecto:**
- Usuario: `admin`
- Contraseña: `admin`

⚠️ **IMPORTANTE:** Cambia la contraseña inmediatamente después del primer login.

## 🎯 Comandos del Juego

### Para Jugadores

```
/class <warrior|mage|rogue|paladin>   - Seleccionar tu clase
/stats                                 - Ver tus estadísticas
/balance                               - Ver tu balance de monedas
/quest list                            - Ver misiones disponibles
/squad create <nombre>                 - Crear un escuadrón
/pets spawn <nombre>                   - Invocar una mascota
/bestiary                              - Ver criaturas descubiertas
/achievements                          - Ver logros
```

### Para Administradores

```
/mmorpgadmin reload                    - Recargar configuración
/mmorpgadmin give <jugador> <coins>    - Dar monedas
/mmorpgadmin setlevel <jugador> <lvl>  - Establecer nivel
/mmorpgadmin cleardata <jugador>       - Limpiar datos de jugador
```

## 🔧 Configuración

### Archivo Principal: `server/config/config.yml`

```yaml
# Cambiar idioma
language: es_ES  # es_ES o en_US

# Ajustar economía
economy:
  starting_coins: 100
  max_coins: 999999999

# Modificar clases
classes:
  warrior:
    base_health: 100
    base_mana: 50
    base_strength: 15
    # ...
```

### Añadir Recetas de Crafteo: `server/config/crafting_config.json`

```json
{
  "recipes": [
    {
      "item_id": "custom_sword",
      "name": "Espada Custom",
      "materials": ["DIAMOND:2", "STICK:1"],
      "required_level": 20,
      "cost": 500
    }
  ]
}
```

### Crear Mazmorras: `server/config/dungeon_config.json`

```json
{
  "dungeons": [
    {
      "dungeon_id": "my_dungeon",
      "name": "Mi Mazmorra",
      "difficulty": "hard",
      "min_level": 30,
      "max_level": 50,
      "waves": 5
    }
  ]
}
```

## 📊 Base de Datos

### Ubicación

- **Base de datos universal:** `server/config/data/universal.db`
- **Base de datos por mundo:** `server/worlds/<nombre_mundo>/world.db`

### Consultar Datos

```bash
# Abrir base de datos
sqlite3 server/config/data/universal.db

# Ver jugadores
SELECT username, level, player_class, coins FROM players JOIN player_economy ON players.uuid = player_economy.player_uuid;

# Ver misiones activas
SELECT p.username, q.name, pq.progress FROM player_quests pq 
JOIN players p ON pq.player_uuid = p.uuid 
JOIN quests q ON pq.quest_id = q.id 
WHERE pq.status = 'active';
```

### Backup de Base de Datos

```bash
# Backup manual
cp server/config/data/universal.db server/config/data/universal.db.backup

# Restaurar
cp server/config/data/universal.db.backup server/config/data/universal.db
```

## 🌐 Panel Web

### Funcionalidades

- **Dashboard:** Resumen del servidor
- **Jugadores:** Lista de todos los jugadores registrados
- **Economía:** Transacciones y balances
- **Misiones:** Gestión de misiones
- **Logs:** Registro de eventos del sistema

### API REST

```bash
# Obtener lista de jugadores
curl http://localhost:5000/api/players

# Ejemplo de respuesta:
[
  {
    "uuid": "550e8400-e29b-41d4-a716-446655440000",
    "username": "Player1",
    "class": "warrior",
    "level": 25,
    "experience": 5420
  }
]
```

## 🔐 Seguridad

### Cambiar Contraseña de Admin

```python
# Conectar a la base de datos
import sqlite3
import bcrypt

conn = sqlite3.connect('server/config/data/universal.db')
c = conn.cursor()

# Generar nuevo hash
new_password = "mi_nueva_contraseña_segura"
password_hash = bcrypt.hashpw(new_password.encode(), bcrypt.gensalt()).decode()

# Actualizar
c.execute("UPDATE admin_users SET password_hash = ? WHERE username = 'admin'", (password_hash,))
conn.commit()
conn.close()
```

### Crear Nuevo Usuario Admin

```sql
-- En sqlite3 server/config/data/universal.db
INSERT INTO admin_users (username, password_hash, email, role) 
VALUES ('nuevo_admin', '<hash_bcrypt>', 'admin@example.com', 'admin');
```

## 🛠️ Mantenimiento

### Ver Logs del Servidor

```bash
# Con systemd
sudo journalctl -u mmorpg-server -f

# Manual
tail -f server/logs/latest.log
```

### Ver Logs del Panel Web

```bash
sudo journalctl -u mmorpg-web -f
```

### Reiniciar Servicios

```bash
# Servidor Minecraft
sudo systemctl restart mmorpg-server

# Panel Web
sudo systemctl restart mmorpg-web

# Ambos
sudo systemctl restart mmorpg-*
```

### Detener Servicios

```bash
sudo systemctl stop mmorpg-server
sudo systemctl stop mmorpg-web
```

### Deshabilitar Inicio Automático

```bash
sudo systemctl disable mmorpg-server
sudo systemctl disable mmorpg-web
```

## 🐛 Solución de Problemas

### El plugin no carga

```bash
# Verificar versión de Java
java -version  # Debe ser 21

# Ver logs del plugin
tail -f server/logs/latest.log | grep MMORPG
```

### Error de base de datos

```bash
# Verificar permisos
ls -la server/config/data/

# Recrear base de datos
rm server/config/data/universal.db
# Reiniciar servidor (se creará automáticamente)
```

### Panel web no accesible

```bash
# Verificar que Python está corriendo
ps aux | grep python

# Verificar puerto
netstat -tulpn | grep 5000

# Reinstalar dependencias
cd server/web
source venv/bin/activate
pip install -r requirements.txt --upgrade
```

### Bajo rendimiento

```bash
# Aumentar RAM del servidor
# Editar: /etc/systemd/system/mmorpg-server.service
# Cambiar: -Xms4G -Xmx4G a -Xms8G -Xmx8G

sudo systemctl daemon-reload
sudo systemctl restart mmorpg-server
```

## 📈 Optimización

### Base de Datos

```sql
-- Optimizar base de datos
sqlite3 server/config/data/universal.db "VACUUM;"
sqlite3 server/config/data/universal.db "ANALYZE;"
```

### Limpiar Logs Antiguos

```bash
# Eliminar logs de más de 7 días
find server/logs -name "*.log.gz" -mtime +7 -delete
```

## 🔄 Actualización

```bash
# 1. Hacer backup
cp -r server/config/data server/config/data.backup
cp -r server/plugins server/plugins.backup

# 2. Compilar nueva versión
git pull  # Si usas Git
./build.sh

# 3. Reemplazar plugin
cp mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar server/plugins/

# 4. Reiniciar
sudo systemctl restart mmorpg-server
```

## 📞 Soporte

### Archivos de Log Importantes

```
server/logs/latest.log           - Log principal del servidor
server/config/data/universal.db   - Base de datos principal
server/plugins/MMORPG/config.yml  - Configuración del plugin
server/web/logs/web.log            - Log del panel web
```

### Información de Debug

```bash
# Recopilar información para reportar un bug
./debug-info.sh > debug-report.txt
```

## ✅ Checklist Post-Instalación

- [ ] Servidor Minecraft iniciado correctamente
- [ ] Plugin MMORPG cargado sin errores
- [ ] Base de datos creada en `server/config/data/universal.db`
- [ ] Panel web accesible en http://localhost:5000
- [ ] Contraseña de admin cambiada
- [ ] Al menos 1 jugador puede conectarse
- [ ] Comandos `/class` y `/stats` funcionan
- [ ] Sistema de economía operativo
- [ ] Logs del sistema sin errores críticos

---

**¡Listo!** Tu servidor MMORPG está configurado y funcionando. 🎮

Para más ayuda, consulta:
- [README.md](README.md) - Documentación general
- [ROADMAP.md](ROADMAP.md) - Plan de desarrollo
- [STATUS.md](STATUS.md) - Estado actual del proyecto
