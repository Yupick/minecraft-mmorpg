# Guía de Uso del Web Panel

## Introducción

El **Web Panel** es una interfaz administrativa web completa para gestionar el servidor Minecraft MMORPG. Permite administrar jugadores, economía, quests, mobs, configuración y mucho más desde cualquier navegador.

---

## Características Principales

- ✅ **Dashboard**: Métricas en tiempo real del servidor
- ✅ **Gestión de Jugadores**: Ver, editar y administrar jugadores
- ✅ **Sistema Económico**: Monitorear transacciones, añadir/retirar monedas
- ✅ **Quests**: Crear, editar y eliminar misiones
- ✅ **Mobs**: Configurar estadísticas de mobs personalizados
- ✅ **Consola**: Ejecutar comandos del servidor en tiempo real
- ✅ **Logs**: Visualizar y filtrar logs del servidor
- ✅ **Configuración**: Modificar ajustes del plugin

---

## Acceso al Panel

### URL de Acceso

Por defecto, el panel está disponible en:
```
http://localhost:5000
```

O desde otra máquina:
```
http://IP_DEL_SERVIDOR:5000
```

### Credenciales por Defecto

Al instalar por primera vez:
- **Usuario**: `admin`
- **Contraseña**: `admin123`

⚠️ **IMPORTANTE**: Cambiar la contraseña inmediatamente después del primer login.

### Cambiar Contraseña

1. Editar el archivo `web/.env`:
```bash
nano /opt/minecraft-mmorpg/web/.env
```

2. Modificar la línea:
```
ADMIN_PASSWORD=nueva_contraseña_segura
```

3. Reiniciar el servicio:
```bash
sudo systemctl restart minecraft-web-panel
```

---

## Dashboard

El dashboard muestra una vista general del servidor.

### Métricas Visibles

**Servidor**:
- Estado (Online/Offline)
- Jugadores conectados / Máximo
- TPS (Ticks por segundo)
- Uptime
- Uso de memoria
- Versión del servidor

**Economía**:
- Total de monedas en circulación
- Transacciones del día
- Monedas ganadas/gastadas hoy

**Jugadores**:
- Total de jugadores registrados
- Jugadores activos hoy
- Nivel promedio
- Clase más popular

**Quests**:
- Quests activas
- Quests completadas hoy
- Quest más popular

### Gráficas

1. **Jugadores Online**: Gráfica de línea mostrando jugadores conectados en las últimas 24 horas
2. **Transacciones**: Gráfica de barras con transacciones económicas por hora
3. **Distribución de Clases**: Gráfica circular con porcentaje de cada clase
4. **Top 5 Jugadores**: Tabla con los 5 jugadores de mayor nivel

---

## Gestión de Jugadores

### Ver Jugadores

1. Ir a **Jugadores** en el menú lateral
2. Ver tabla con todos los jugadores registrados

**Información visible**:
- Avatar (Minotar)
- Username
- Nivel y XP
- Clase
- Monedas
- Estado (Online/Offline)
- Última conexión

### Buscar Jugadores

Usar la barra de búsqueda para filtrar por:
- Username
- UUID
- Clase
- Nivel (rango)

**Ejemplo**: Buscar "warrior" mostrará todos los guerreros.

### Editar Jugador

1. Click en el botón **Editar** (icono de lápiz)
2. Se abre un modal con todos los datos editables

**Campos editables**:
- Nivel (1-100)
- XP (0-999999)
- Monedas
- Clase (warrior, mage, archer, rogue)
- Stats: Strength, Defense, Speed
- HP y Mana (actual y máximo)

3. Click en **Guardar Cambios**

### Ver Gráfico de Stats

En la vista de edición, se muestra un **gráfico de radar** con los stats del jugador:
- Strength
- Defense
- Speed
- Health
- Mana

Esto permite visualizar rápidamente el balance del jugador.

### Filtros Avanzados

**Por Estado**:
- Online: Solo jugadores conectados
- Offline: Solo jugadores desconectados
- Todos: Todos los jugadores

**Por Clase**:
- Warrior
- Mage
- Archer
- Rogue

**Por Nivel**:
- Principiantes (1-20)
- Intermedios (21-50)
- Avanzados (51-80)
- Expertos (81-100)

---

## Economía

### Vista de Economía

Muestra 4 tarjetas principales:

1. **Circulación Total**: Suma de monedas de todos los jugadores
2. **Ganado Hoy**: Total de monedas generadas hoy (quests, mobs, etc.)
3. **Gastado Hoy**: Total de monedas consumidas hoy (tiendas, servicios, etc.)
4. **Transacciones**: Número de transacciones del día

### Gráficas Económicas

**Actividad Económica** (Gráfica de línea):
- Muestra monedas ganadas vs gastadas por día
- Últimos 7 días
- Permite identificar tendencias

**Distribución de Riqueza** (Gráfica circular):
- Top 10 jugadores más ricos
- Porcentaje de la riqueza total que poseen
- Colores: oro (1°), plata (2°), bronce (3°)

### Top Jugadores

**Top Ganadores**:
Lista de jugadores que más monedas han ganado (total histórico)

**Top Gastadores**:
Lista de jugadores que más monedas han gastado

### Historial de Transacciones

Tabla con todas las transacciones recientes:
- Jugador
- Cantidad (+verde, -rojo)
- Tipo (earn, spend, admin_add, admin_remove)
- Fuente (quest, mob_kill, shop, admin, etc.)
- Descripción
- Fecha/Hora

**Filtros**:
- Por jugador
- Por tipo de transacción
- Por rango de fechas
- Por fuente

### Añadir Monedas

1. Click en **Añadir Monedas**
2. Ingresar:
   - UUID o username del jugador
   - Cantidad de monedas
   - Razón (opcional, para logs)
3. Click en **Confirmar**

Se registra en `economy_log` con tipo `admin_add`.

### Retirar Monedas

1. Click en **Retirar Monedas**
2. Ingresar:
   - UUID o username del jugador
   - Cantidad de monedas
   - Razón (opcional, para logs)
3. Click en **Confirmar**

Se registra en `economy_log` con tipo `admin_remove`.

⚠️ **Nota**: No se puede retirar más de lo que el jugador tiene.

---

## Quests

### Ver Quests

Lista todas las quests disponibles en el servidor.

**Información visible**:
- ID de la quest
- Nombre
- Descripción
- Nivel mínimo
- Tipo (kill, collect, deliver, talk, location)
- Repetible (Sí/No)
- Estado (Activa/Inactiva)

### Crear Quest

1. Click en **Nueva Quest**
2. Completar formulario:

**Campos básicos**:
- Nombre de la quest
- Descripción
- Nivel mínimo requerido
- Tipo de quest
- Repetible (checkbox)
- Cooldown (si es repetible, en segundos)
- NPC asignado (opcional)

**Objetivos** (JSON):
```json
[
    {"type": "kill", "target": "ZOMBIE", "count": 50},
    {"type": "collect", "target": "DIAMOND", "count": 10}
]
```

**Recompensas** (JSON):
```json
{
    "coins": 500,
    "exp": 250,
    "items": [
        {"item": "DIAMOND_SWORD", "amount": 1}
    ]
}
```

3. Click en **Crear Quest**

### Editar Quest

1. Click en **Editar** (icono de lápiz)
2. Modificar campos deseados
3. Click en **Guardar Cambios**

**Campos editables**:
- Nombre
- Descripción
- Objetivos
- Recompensas
- Nivel mínimo
- Estado (Activa/Inactiva)

### Eliminar Quest

1. Click en **Eliminar** (icono de papelera)
2. Confirmar eliminación

⚠️ **Advertencia**: No se puede eliminar una quest si hay jugadores que la tienen activa.

### Filtrar Quests

**Por Tipo**:
- Kill: Matar mobs
- Collect: Recolectar items
- Deliver: Entregar items a NPC
- Talk: Hablar con NPC
- Location: Llegar a ubicación

**Por Estado**:
- Activas: Disponibles para jugadores
- Inactivas: Deshabilitadas temporalmente

**Por Nivel**:
- Nivel 1-20
- Nivel 21-50
- Nivel 51+

### Ver Progreso de Jugadores

1. Click en **Ver Progreso**
2. Ver tabla con todos los jugadores que tienen la quest
3. Información:
   - Username
   - Progreso (ej: 47/50 zombies)
   - Estado (in_progress, completed, failed)
   - Fecha de inicio
   - Veces completada (si es repetible)

---

## Mobs

### Ver Mobs

Lista todos los mobs personalizados configurados.

**Información visible**:
- ID del mob
- Tipo (ZOMBIE_ELITE, SKELETON_BOSS, etc.)
- Nivel
- Health (HP)
- Damage (Daño)
- Boss (Sí/No)
- Agresivo (Sí/No)
- Recompensas (XP + Monedas)

### Editar Mob

1. Click en **Editar**
2. Modificar estadísticas:

**Stats básicos**:
- Nivel (1-100)
- Health (20.0 - 10000.0)
- Damage (1.0 - 100.0)
- Speed (0.1 - 1.0)
- Defense (0.0 - 100.0)

**Recompensas**:
- XP reward (1-10000)
- Coins mínimo (0-1000)
- Coins máximo (0-10000)

**Configuración**:
- Es Boss (checkbox)
- Es Agresivo (checkbox)

**Drops** (JSON):
```json
[
    {"item": "DIAMOND", "chance": 0.05, "amount": 1},
    {"item": "GOLD_INGOT", "chance": 0.2, "amount": 3}
]
```

**Habilidades** (JSON):
```json
[
    {"type": "speed_boost", "duration": 5, "cooldown": 20},
    {"type": "explosion", "radius": 3, "cooldown": 15}
]
```

3. Click en **Guardar Cambios**

### Crear Mob Personalizado

1. Click en **Nuevo Mob**
2. Seleccionar tipo base (ZOMBIE, SKELETON, etc.)
3. Configurar stats y habilidades
4. Click en **Crear**

### Identificación Visual

- **Boss**: Corona dorada (👑) junto al nombre
- **Agresivo**: Badge rojo con "Agresivo"
- **Pasivo**: Badge verde con "Pasivo"

---

## Consola

La consola permite ejecutar comandos del servidor en tiempo real mediante **RCON**.

### Ejecutar Comando

1. Ir a **Consola**
2. Escribir comando en el input (sin el `/`)
3. Presionar **Enter** o click en **Ejecutar**

**Ejemplos**:
```
say Hola desde el panel
tp Steve 100 64 200
give Alex diamond 64
gamemode creative Notch
weather clear
time set day
```

### Historial de Comandos

El panel guarda un historial de comandos ejecutados. Usar flechas **↑** y **↓** para navegar.

### Comandos Rápidos

Botones predefinidos para comandos comunes:
- **Lista de Jugadores**: `list`
- **Guardar Mundo**: `save-all`
- **TPS**: `tps`
- **Memoria**: `gc`
- **Clima Despejado**: `weather clear`
- **Día**: `time set day`

### Logs en Tiempo Real

La consola muestra los últimos 100 mensajes del servidor en tiempo real (si está configurado).

**Auto-scroll**: Checkbox para desplazamiento automático al final.

### Control del Servidor

⚠️ **Comandos Peligrosos**:

**Reiniciar Servidor**:
```
restart
```

**Detener Servidor**:
```
stop
```

**Recargar Plugin**:
```
reload
```

---

## Logs

### Visualizar Logs

1. Ir a **Logs**
2. Seleccionar archivo de log:
   - **latest.log**: Log actual del servidor
   - **panel.log**: Log del panel web
   - **error.log**: Solo errores
   - **debug.log**: Logs de debug (si está activado)

### Filtrar Logs

**Por Nivel**:
- INFO: Mensajes informativos
- WARN: Advertencias
- ERROR: Errores
- DEBUG: Mensajes de debug
- SUCCESS: Operaciones exitosas

**Por Búsqueda**:
Buscar texto específico en los logs.

**Límite de Líneas**:
- Últimas 50 líneas
- Últimas 100 líneas
- Últimas 500 líneas
- Todas las líneas

### Estadísticas de Logs

En la parte superior se muestran:
- Total de líneas
- Errores detectados
- Advertencias detectadas
- Última actualización

### Descargar Logs

1. Click en **Descargar Logs**
2. Se descarga el archivo completo

### Limpiar Logs

1. Click en **Limpiar Logs**
2. Confirmar acción
3. El archivo de log se vacía (se hace backup automático)

⚠️ **Precaución**: Esta acción no se puede deshacer.

---

## Configuración

### Archivo panel_config.json

El panel se configura desde [server/config/panel_config.json](../server/config/panel_config.json). Ahí se definen:

- **web_server**: host, puerto, `secret_key` y tiempo de sesión.
- **authentication**: cookies de sesión, lockout y políticas de login.
- **rate_limiting**: límites de API, login y consola.
- **minecraft_server**: RCON, rutas y logs.
- **backup**: ruta y políticas de backups.
- **integrations**: secretos y reintentos para webhooks.

> En producción, cambia `secret_key` y activa `session_cookie_secure`.

### Secciones de Configuración

#### 1. General
- Activar/Desactivar plugin
- Modo debug
- Idioma
- Prefijo de mensajes

#### 2. Gameplay
- Monedas iniciales
- Nivel máximo
- Multiplicador de XP
- Penalización por muerte (%)
- Bonus de login diario

#### 3. Economía
- Activar tiendas
- Tasa de impuesto (%)
- Recompensa de login diario
- Activar comercio entre jugadores

#### 4. Base de Datos
- Auto-backup
- Intervalo de backup (segundos)
- Tamaño del connection pool
- Timeout de queries

#### 5. Seguridad
- Activar RCON
- Puerto RCON
- Contraseña RCON
- Whitelist del panel

#### 6. Avanzado
Configuración JSON completa del plugin.

### Guardar Cambios

1. Modificar los campos deseados
2. Click en **Guardar Configuración**
3. El panel sugiere hacer `/reload` o reiniciar el servidor

### Restaurar Valores por Defecto

1. Click en **Restaurar Valores por Defecto**
2. Confirmar acción
3. Todos los valores vuelven a su configuración inicial

### Vacuum Database

Optimiza la base de datos eliminando espacio no utilizado.

1. Click en **Vacuum Database**
2. Esperar a que complete (puede tardar varios minutos en bases de datos grandes)
3. Ver estadísticas de espacio recuperado

**Recomendación**: Ejecutar una vez por mes.

---

## Uso Común: Casos de Uso

### Caso 1: Añadir Monedas a un Jugador

1. Ir a **Economía**
2. Click en **Añadir Monedas**
3. Ingresar username: `Steve`
4. Cantidad: `1000`
5. Razón: `Compensación por bug`
6. Confirmar

### Caso 2: Crear una Quest de Evento

1. Ir a **Quests**
2. Click en **Nueva Quest**
3. Datos:
   - Nombre: `Evento de Halloween`
   - Descripción: `Mata 100 zombies durante el evento`
   - Nivel mínimo: `1`
   - Tipo: `kill`
   - Repetible: ✅
   - Cooldown: `86400` (1 día)
4. Objetivos:
```json
[{"type":"kill","target":"ZOMBIE","count":100}]
```
5. Recompensas:
```json
{"coins":2000,"exp":1000,"items":[{"item":"PUMPKIN","amount":10}]}
```
6. Crear

### Caso 3: Investigar un Error

1. Ir a **Logs**
2. Seleccionar `error.log`
3. Filtrar nivel: `ERROR`
4. Buscar el timestamp del problema
5. Revisar stacktrace
6. Descargar logs completos si es necesario

### Caso 4: Monitorear el Servidor

1. Ir a **Dashboard**
2. Revisar métricas:
   - TPS (debe estar >19.0)
   - Memoria (no debe estar >90%)
   - Jugadores online
3. Ver gráfica de jugadores para detectar picos
4. Revisar transacciones económicas (detectar exploits)

**Health check rápido**:
```bash
curl http://localhost:5000/api/health
```

### Caso 5: Configurar un Boss Custom

1. Ir a **Mobs**
2. Click en **Nuevo Mob**
3. Tipo base: `ZOMBIE`
4. Configurar:
   - Nombre: `ZOMBIE_KING`
   - Nivel: `50`
   - Health: `1000.0`
   - Damage: `25.0`
   - Es Boss: ✅
   - XP: `500`
   - Coins: `500-1000`
5. Drops:
```json
[
    {"item":"DIAMOND","chance":0.5,"amount":5},
    {"item":"EMERALD","chance":0.3,"amount":3}
]
```
6. Habilidades:
```json
[
    {"type":"summon","mob":"ZOMBIE","count":3,"cooldown":30},
    {"type":"heal","amount":100,"cooldown":60}
]
```
7. Guardar

---

## Troubleshooting

### Panel no carga

1. Verificar que el servicio esté corriendo:
```bash
sudo systemctl status minecraft-web-panel
```

2. Revisar logs del panel:
```bash
sudo journalctl -u minecraft-web-panel -n 50
```

3. Verificar puerto abierto:
```bash
sudo netstat -tulpn | grep 5000
```

4. Verificar estado interno:
```bash
curl http://localhost:5000/api/health
```

### No se pueden ejecutar comandos (RCON)

1. Verificar RCON habilitado en `server.properties`:
```properties
enable-rcon=true
rcon.port=25575
rcon.password=tu_contraseña
```

2. Verificar puerto abierto:
```bash
sudo ufw allow 25575/tcp
```

3. Reiniciar servidor Minecraft

### Datos no se actualizan

1. Verificar conexión a base de datos:
```bash
ls -lh /opt/minecraft-mmorpg/server/universal.db
```

2. Verificar permisos:
```bash
sudo chown -R minecraft:minecraft /opt/minecraft-mmorpg/server/
```

3. Revisar logs de error en el panel

### Gráficas no aparecen

1. Verificar que Chart.js se cargó (abrir consola del navegador F12)
2. Limpiar cache del navegador (Ctrl+Shift+Del)
3. Revisar logs de JavaScript en consola

---

## Actualizaciones

Para actualizar el panel a la última versión:

```bash
cd /opt/minecraft-mmorpg
sudo -u minecraft /opt/minecraft-mmorpg/scripts/update.sh
```

Este script:
1. Hace backup de configuración
2. Descarga última versión desde Git
3. Actualiza dependencias Python
4. Reinicia servicio

---

## Seguridad

### Recomendaciones

1. **Cambiar contraseña por defecto**
2. **Usar HTTPS** con Nginx/Apache como proxy reverso
3. **Configurar firewall** para permitir solo IPs autorizadas
4. **Mantener rate limiting** en [server/config/panel_config.json](../server/config/panel_config.json)
5. **Revisar logs regularmente**

### Ajustes recomendados en panel_config.json

- `web_server.secret_key`: valor fuerte y único.
- `authentication.session_cookie_secure`: `true` si hay HTTPS.
- `authentication.lockout_duration_minutes`: bloquear intentos masivos.
- `rate_limiting.*`: ajustar a tu tráfico real.
- `integrations.webhook_secret`: firma HMAC de webhooks.

### Configurar HTTPS con Nginx

```nginx
server {
    listen 443 ssl;
    server_name panel.tuservidor.com;
    
    ssl_certificate /etc/letsencrypt/live/panel.tuservidor.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/panel.tuservidor.com/privkey.pem;
    
    location / {
        proxy_pass http://localhost:5000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## Soporte

Si encuentras problemas:

1. Revisar logs del panel: `/opt/minecraft-mmorpg/logs/panel.log`
2. Revisar [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
3. Abrir issue en GitHub con detalles completos
