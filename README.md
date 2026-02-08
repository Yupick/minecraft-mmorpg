# 🎮 Minecraft MMORPG Server - Paper 1.20.6

Sistema completo de servidor MMORPG para Minecraft Paper 1.20.6 con plugin Java personalizado y panel web de administración.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Paper](https://img.shields.io/badge/Paper-1.20.6-blue.svg)](https://papermc.io/)
[![Python](https://img.shields.io/badge/Python-3.12+-green.svg)](https://www.python.org/)
[![Flask](https://img.shields.io/badge/Flask-3.0+-lightgrey.svg)](https://flask.palletsprojects.com/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## 📋 Tabla de Contenidos

- [Características](#-características)
- [Requisitos](#-requisitos)
- [Instalación](#-instalación)
- [Actualización](#-actualización)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Uso](#-uso)
- [Configuración](#-configuración)
- [Panel Web](#-panel-web)
- [Desarrollo](#-desarrollo)
- [Roadmap](#-roadmap)
- [Contribuir](#-contribuir)
- [Licencia](#-licencia)

## ✨ Características

### 🎯 Sistema RPG Completo

- **Sistema de Clases**: Warrior, Mage, Rogue, Paladin (extensible)
- **Sistema de Quests**: Misiones dinámicas con seguimiento de progreso
- **Sistema de Economía**: Moneda personalizada (Gold/Coins) con NPCs comerciantes
- **Sistema de Crafting**: Recetas personalizadas con requisitos de nivel
- **Sistema de Encantamientos**: Encantamientos RPG personalizados
- **Mobs Customizados**: Enemigos con estadísticas y drops personalizados
- **Dungeons**: Mazmorras instanciables con oleadas de enemigos
- **Invasiones**: Eventos periódicos del servidor
- **Sistema de Pets**: Mascotas adoptables y entrenables
- **Squads/Grupos**: Sistema de grupos con progresión colectiva
- **Logros**: Sistema de achievements desbloqueables
- **Bestiario**: Registro de mobs derrotados

### 🗄️ Base de Datos

- **SQLite Central**: Base de datos universal (`universal.db`)
- **BD por Mundo**: Base de datos local por cada mundo (`world.db`)
- **Migración Automática**: Migración de datos JSON a SQLite
- **Persistencia**: Todos los datos RPG persisten en BD

### 🌍 Múltiples Mundos

- **Soporte Multi-Mundo**: Gestión de múltiples mundos independientes
- **Symlinks**: Sistema de symlinks para mundo activo
- **BD Local**: Cada mundo tiene su propia base de datos
- **Cambio Dinámico**: Scripts para cambiar mundo activo

### 🖥️ Panel Web de Administración

- **Dashboard**: Estado del servidor en tiempo real
- **Gestión de Jugadores**: Ver/editar estadísticas de jugadores
- **Economía**: Monitor de transacciones y balance
- **Gestión de Quests**: Crear/editar misiones
- **Gestión de Mobs**: Configurar mobs personalizados
- **Consola RCON**: Ejecutar comandos remotamente
- **Logs**: Visualización de logs del servidor
- **Configuración**: Editor de configuraciones

### 🔌 Compatibilidad

- **Bedrock Edition**: Soporte via Geyser + Floodgate
- **Versiones Antiguas**: Soporte via ViaVersion, ViaBackwards, ViaRewind
- **Cross-Platform**: Java Edition + Bedrock Edition

## 📦 Requisitos

### Sistema Operativo
- **Linux** (Ubuntu 20.04+, Debian 11+, CentOS 8+, Arch Linux)
- **macOS** (10.15+)
- **Windows** (WSL2 recomendado)

### Software Requerido

#### Servidor Minecraft
- **Java JDK**: 21+ ([Adoptium](https://adoptium.net/))
- **Maven**: 3.9+ (para compilar el plugin)
- **Git**: Para clonar el repositorio

#### Panel Web
- **Python**: 3.12+
- **pip**: Gestor de paquetes Python
- **virtualenv**: Para entorno virtual Python

#### Opcional
- **systemd**: Para ejecutar como servicio (Linux)
- **screen/tmux**: Para ejecutar en background

### Recursos del Servidor
- **RAM**: Mínimo 4GB (8GB+ recomendado)
- **CPU**: 2+ cores
- **Disco**: 10GB+ libres
- **Red**: Puerto 25565 (Minecraft), 25575 (RCON), 5000 (Panel Web)

## 🚀 Instalación

### Instalación Rápida (Recomendado)

```bash
# 1. Clonar el repositorio
git clone https://github.com/tu-usuario/minecraft-mmorpg.git
cd minecraft-mmorpg

# 2. Dar permisos de ejecución
chmod +x install-native.sh

# 3. Ejecutar instalación
./install-native.sh
```

El script de instalación:
- ✅ Verifica dependencias (Java, Maven, Python)
- ✅ Descarga Paper Server 1.20.6
- ✅ Compila el plugin MMORPG
- ✅ Configura la base de datos SQLite
- ✅ Instala el panel web Flask
- ✅ Configura plugins adicionales (Geyser, ViaVersion)
- ✅ Crea scripts de inicio
- ✅ (Opcional) Configura servicios systemd

### Instalación Manual

Ver [INSTALL.md](docs/INSTALL.md) para instrucciones detalladas de instalación manual.

## 📁 Estructura del Proyecto

```
minecraft-mmorpg/
├── install-native.sh           # Script de instalación principal
├── uninstall-native.sh         # Script de desinstalación
├── README.md                   # Este archivo
├── ROADMAP.md                  # Plan de desarrollo
├── LICENSE                     # Licencia MIT
├── .gitignore                  # Archivos ignorados por Git
│
├── mmorpg-plugin/              # Plugin Java MMORPG
│   ├── pom.xml                 # Configuración Maven
│   ├── src/main/java/          # Código fuente Java
│   ├── src/main/resources/     # Recursos (plugin.yml, configs)
│   ├── target/                 # Archivos compilados
│   └── docs/                   # Documentación del plugin
│
├── web/                        # Panel web Flask
│   ├── app.py                  # Aplicación principal
│   ├── requirements.txt        # Dependencias Python
│   ├── templates/              # Plantillas HTML
│   ├── static/                 # CSS, JS, imágenes
│   └── venv/                   # Entorno virtual Python
│
├── server/                     # Servidor Paper (directorio de instalación)
│   ├── paper-1.20.6.jar        # Ejecutable del servidor
│   ├── eula.txt                # EULA aceptado
│   ├── server.properties       # Propiedades del servidor
│   ├── config/                 # Configuraciones
│   │   ├── config.yml           # Configuración del plugin
│   │   ├── crafting_config.json # Recetas de crafting
│   │   ├── enchanting_config.json # Configuración de encantamientos
│   │   ├── respawn_config.json  # Zonas de respawn
│   │   ├── dungeon_config.json  # Configuración de dungeons
│   │   ├── events_config.json   # Eventos/invasiones
│   │   ├── pets_config.json     # Configuración de pets
│   │   ├── squad_config.json    # Configuración de squads
│   │   ├── panel_config.json    # Configuración del panel web
│   │   └── data/
│   │       └── universal.db     # Base de datos SQLite principal
│   ├── plugins/                # Plugins instalados
│   │   ├── MMORPGPlugin.jar
│   │   ├── Geyser-Spigot.jar
│   │   ├── floodgate-spigot.jar
│   │   ├── ViaVersion.jar
│   │   ├── ViaBackwards.jar
│   │   └── ViaRewind.jar
│   ├── worlds/                 # Mundos del servidor
│   │   ├── active -> mundo-inicial/  # Symlink al mundo activo
│   │   ├── mundo-inicial/
│   │   │   ├── world/
│   │   │   ├── world_nether/
│   │   │   ├── world_the_end/
│   │   │   └── data/
│   │   │       └── world.db    # BD local del mundo
│   │   └── worlds.json         # Configuración de mundos
│   └── logs/                   # Logs del servidor
│
├── scripts/                    # Scripts utilitarios
│   ├── build-mmorpg-plugin.sh  # Compilar plugin
│   ├── change-server-version.sh # Cambiar versión de Paper
│   ├── check-panel.sh          # Verificar estado del panel
│   ├── logs-web-panel.sh       # Ver logs del panel web
│   ├── update.sh               # Actualizar sistema
│   └── status-web-panel.sh     # Estado del panel web
│
├── test/                       # Tests
│   ├── test_api_endpoints.py  # Tests de API
│   ├── test_backup_service.py # Tests de backups
│   └── run-tests.sh            # Ejecutar tests
│
└── docs/                       # Documentación
    ├── INSTALL.md              # Guía de instalación
    ├── API.md                  # Documentación de API
    ├── DATABASE.md             # Esquema de base de datos
    ├── PLUGIN_DEV.md           # Guía de desarrollo del plugin
    └── WEB_PANEL.md            # Guía del panel web
```

## 🚄 Actualización

### Actualizar desde GitHub

Ejecuta el script de actualización automática desde el servidor:

```bash
# Actualización automática (interactiva)
./update.sh

# El script:
# ✅ Descarga cambios de GitHub
# ✅ Verifica cambios locales
# ✅ Crea backup automático
# ✅ Detiene servicios
# ✅ Recompila plugin si es necesario
# ✅ Instala cambios
# ✅ Reinicia servicios
# ✅ Muestra resumen
```

Para más información, ver [UPDATE_GUIDE.md](UPDATE_GUIDE.md)

## 🎮 Uso

### Iniciar el Servidor

```bash
# Método 1: Script directo
cd server
java -Xmx4G -Xms2G -jar paper-1.20.6.jar nogui

# Método 2: Con screen (recomendado)
screen -S minecraft
cd server
java -Xmx4G -Xms2G -jar paper-1.20.6.jar nogui
# Ctrl+A+D para desconectar

# Método 3: Servicio systemd (si configurado)
sudo systemctl start mmorpg-server
```

### Iniciar el Panel Web

```bash
# Método 1: Script directo
cd server/web
./start-web.sh

# Método 2: Manual
cd server/web
source venv/bin/activate
python app.py

# Método 3: Servicio systemd (si configurado)
sudo systemctl start minecraft-web-panel
```

### Comandos del Plugin

En el juego o consola:

```
/class <warrior|mage|rogue|paladin> - Cambiar clase
/quest list - Listar quests disponibles
/quest start <id> - Iniciar quest
/quest progress - Ver progreso de quests
/stats - Ver tus estadísticas RPG
/balance - Ver tu balance de coins
/squad create <nombre> - Crear un squad
/pets list - Ver tus pets
/bestiary - Ver bestiario
/achievements - Ver logros
```

### Acceder al Panel Web

1. Abrir navegador en: `http://localhost:5000`
2. Iniciar sesión con credenciales de admin (generadas durante instalación)
3. Dashboard mostrará estado del servidor

## ⚙️ Configuración

### Configuración del Servidor

Editar [server/server.properties](server/server.properties):

```properties
server-port=25565
max-players=20
difficulty=normal
gamemode=survival
pvp=true
level-name=world
```

### Configuración del Plugin

Editar [server/config/config.yml](server/config/config.yml):

```yaml
language: es_ES

database:
  type: sqlite
  path: config/data/universal.db

rpg:
  enabled: true
  default_class: warrior
  max_level: 100
  enable_class_switching: true

economy:
  enabled: true
  initial_balance: 1000
  currency_name: Gold
  currency_symbol: "⛃"

quests:
  enabled: true
  daily_quests: true
  daily_quest_count: 3
```

### Configuración del Panel Web

Editar [server/config/panel_config.json](server/config/panel_config.json):

```json
{
  "web_host": "0.0.0.0",
  "web_port": 5000,
  "secret_key": "CHANGE_THIS_SECRET_KEY",
  "session_timeout": 3600,
  "rcon_host": "localhost",
  "rcon_port": 25575,
  "rcon_password": "minecraft"
}
```

## 🖥️ Panel Web

### Funcionalidades

- **Dashboard**: Monitoreo en tiempo real
  - Estado del servidor (online/offline)
  - Jugadores conectados
  - Uso de recursos (TPS, RAM)
  - Estadísticas RPG globales

- **Gestión de Jugadores**
  - Listar todos los jugadores
  - Ver estadísticas individuales
  - Editar nivel, clase, balance
  - Banear/desbanear jugadores

- **Economía**
  - Ver transacciones recientes
  - Balance global de la economía
  - Top jugadores más ricos
  - Ajustar balance de jugadores

- **Quests**
  - Crear nuevas quests
  - Editar quests existentes
  - Ver progreso de jugadores
  - Activar/desactivar quests

- **Mobs**
  - Configurar mobs personalizados
  - Ajustar estadísticas (HP, daño)
  - Configurar drops
  - Ver estadísticas de kills

- **Consola RCON**
  - Ejecutar comandos remotamente
  - Ver output en tiempo real
  - Historial de comandos

- **Configuración**
  - Editor de archivos de configuración
  - Aplicar cambios sin reiniciar (cuando sea posible)

- **Logs**
  - Ver logs del servidor
  - Filtrar por tipo (info, warning, error)
  - Buscar en logs

### Seguridad

- Autenticación con contraseña hash (bcrypt)
- Sesiones con timeout configurable
- Rate limiting en endpoints críticos
- Validación de todas las entradas
- **IMPORTANTE**: Usar HTTPS en producción

## 🛠️ Desarrollo

### Compilar el Plugin

```bash
cd mmorpg-plugin
mvn clean package

# El JAR compilado estará en:
# target/mmorpg-plugin-1.0.0.jar
```

### Ejecutar Tests

```bash
# Tests del plugin Java
cd mmorpg-plugin
mvn test

# Tests del panel web
cd test
./run-tests.sh
```

### Contribuir

Ver [CONTRIBUTING.md](CONTRIBUTING.md) para guías de contribución.

### Estructura del Código del Plugin

```
src/main/java/com/nightslayer/mmorpg/
├── MMORPGPlugin.java           # Clase principal
├── database/                   # Gestión de BD
├── npcs/                       # NPCs y diálogos
├── quests/                     # Sistema de quests
├── items/                      # Items personalizados
├── mobs/                       # Mobs personalizados
├── economy/                    # Sistema económico
├── crafting/                   # Sistema de crafting
├── enchanting/                 # Encantamientos
├── respawn/                    # Gestión de respawn
├── dungeon/                    # Dungeons
├── invasions/                  # Invasiones
├── pets/                       # Sistema de pets
├── spawns/                     # Gestión de spawns
├── ranks/                      # Sistema de rangos
├── squads/                     # Grupos/squads
├── achievements/               # Logros
├── bestiary/                   # Bestiario
├── api/                        # API para panel web
├── i18n/                       # Internacionalización
├── listeners/                  # Event listeners
├── commands/                   # Comandos del juego
└── events/                     # Eventos personalizados
```

## 📊 Roadmap

Ver [ROADMAP.md](ROADMAP.md) para el plan de desarrollo completo.

### Estado Actual

- ✅ **Fase 1**: Estructura base y configuraciones
- ✅ **Fase 2**: Plugin Java - Core y Database
- ✅ **Fase 3**: Plugin Java - Managers RPG básicos
- ✅ **Fase 4**: Plugin Java - Sistemas avanzados
- ✅ **Fase 5**: Panel Web Flask
- ✅ **Fase 6**: Scripts de instalación
- ✅ **Fase 7**: Testing y documentación

### Próximas Características

- [ ] Sistema de profesiones (minería, herrería, alquimia)
- [ ] PvP con arenas y rankings
- [ ] Sistema de guilds/clanes
- [ ] Eventos estacionales automáticos
- [ ] Integración con Discord
- [ ] API REST pública
- [ ] Sistema de backups automáticos

## 🤝 Contribuir

Las contribuciones son bienvenidas! Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

## 🙏 Agradecimientos

- [Paper](https://papermc.io/) - Servidor de Minecraft de alto rendimiento
- [GeyserMC](https://geysermc.org/) - Soporte para Bedrock Edition
- [ViaVersion](https://www.viaversion.com/) - Soporte para versiones antiguas
- [Flask](https://flask.palletsprojects.com/) - Framework web Python
- [SQLite](https://www.sqlite.org/) - Base de datos embebida

## 📞 Soporte

- **Issues**: [GitHub Issues](https://github.com/tu-usuario/minecraft-mmorpg/issues)
- **Discussions**: [GitHub Discussions](https://github.com/tu-usuario/minecraft-mmorpg/discussions)
- **Wiki**: [GitHub Wiki](https://github.com/tu-usuario/minecraft-mmorpg/wiki)

## 📈 Estadísticas

![GitHub stars](https://img.shields.io/github/stars/tu-usuario/minecraft-mmorpg?style=social)
![GitHub forks](https://img.shields.io/github/forks/tu-usuario/minecraft-mmorpg?style=social)
![GitHub watchers](https://img.shields.io/github/watchers/tu-usuario/minecraft-mmorpg?style=social)

---

**Desarrollado con ❤️ para la comunidad de Minecraft**
