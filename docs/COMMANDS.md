# Referencia de Comandos del Plugin

## Comandos de Jugador

### `/stats` - Ver Estadísticas
Muestra las estadísticas completas del jugador.

**Uso**:
```
/stats
/stats <jugador>
```

**Permisos**:
- `mmorpg.stats` - Ver tus propias stats
- `mmorpg.stats.others` - Ver stats de otros jugadores

**Ejemplos**:
```
/stats
/stats Steve
```

**Output**:
```
§6===== §eEstadísticas de Steve §6=====
§aNivel: §f45 §7(§e12580§7/§e13500 XP§7)
§aClase: §fWarrior
§aMonedas: §f15,420.50
§aRango: §fExpert §7(Multiplicador: §e2.0x§7)

§6===== §eStats §6=====
§cFuerza: §f75
§9Defensa: §f60
§bVelocidad: §f45
§cVida: §f120.0/120.0
§bMana: §f150.0/200.0
§aSkill Points: §f5
```

---

### `/quest` - Sistema de Misiones
Gestiona las quests del jugador.

**Subcomandos**:
```
/quest list              - Lista quests disponibles
/quest active            - Muestra quests activas
/quest start <id>        - Inicia una quest
/quest abandon <id>      - Abandona una quest
/quest info <id>         - Información detallada
/quest complete <id>     - Completa una quest (si está lista)
```

**Permisos**:
- `mmorpg.quest` - Acceso básico al sistema de quests
- `mmorpg.quest.start` - Iniciar quests
- `mmorpg.quest.abandon` - Abandonar quests

**Ejemplos**:
```
/quest list
/quest start 5
/quest active
/quest info 5
/quest complete 5
/quest abandon 5
```

**Output de `/quest active`**:
```
§6===== §eQuests Activas §6=====

§e[#5] Zombie Slayer
§7Mata 50 zombies
§aProgreso: §f47/50 §7(§e94%§7)
§6Recompensa: §f500 monedas, 250 XP

§e[#12] Emerald Hunter
§7Recolecta 10 esmeraldas
§aProgreso: §f3/10 §7(§e30%§7)
§6Recompensa: §f1000 monedas, 500 XP
```

---

### `/shop` - Tienda de Items
Abre la interfaz de tienda.

**Subcomandos**:
```
/shop                    - Abre la tienda principal
/shop weapons            - Tienda de armas
/shop armor              - Tienda de armaduras
/shop potions            - Tienda de pociones
/shop sell               - Vender items
/shop buy <item> [qty]   - Comprar item específico
```

**Permisos**:
- `mmorpg.shop` - Acceso a la tienda
- `mmorpg.shop.sell` - Vender items
- `mmorpg.shop.discount` - 10% de descuento

**Ejemplos**:
```
/shop
/shop weapons
/shop buy DIAMOND_SWORD
/shop buy IRON_INGOT 64
/shop sell
```

---

### `/skill` - Sistema de Habilidades
Gestiona las skills del jugador.

**Subcomandos**:
```
/skill list              - Lista todas las skills
/skill info <skill>      - Información de una skill
/skill upgrade <skill>   - Mejorar una skill
/skill reset             - Reiniciar todas las skills
```

**Permisos**:
- `mmorpg.skill` - Acceso al sistema de skills
- `mmorpg.skill.upgrade` - Mejorar skills
- `mmorpg.skill.reset` - Reiniciar skills (cuesta monedas)

**Ejemplos**:
```
/skill list
/skill info swordmastery
/skill upgrade swordmastery
/skill reset
```

**Output de `/skill list`**:
```
§6===== §eTus Skills §6=====

§c⚔ Sword Mastery §7- Nivel §e30 §7(§f5420/6000 XP§7)
  §7+15% daño con espadas
  
§e⛏ Mining §7- Nivel §e25 §7(§f3200/5000 XP§7)
  §7+12.5% velocidad de minado
  
§b🏹 Archery §7- Nivel §e20 §7(§f1800/4000 XP§7)
  §7+10% daño con arcos

§aSkill Points Disponibles: §f5
§7Usa §e/skill upgrade <skill> §7para mejorar
```

---

### `/party` - Sistema de Grupos
Gestiona parties (grupos de jugadores).

**Subcomandos**:
```
/party create            - Crea una party
/party invite <jugador>  - Invita a un jugador
/party accept            - Acepta invitación
/party decline           - Rechaza invitación
/party kick <jugador>    - Expulsa a un jugador
/party leave             - Abandona la party
/party info              - Información de la party
/party list              - Lista miembros
/party chat <mensaje>    - Chat de party
```

**Permisos**:
- `mmorpg.party` - Crear y unirse a parties
- `mmorpg.party.leader` - Comandos de líder (kick, etc.)

**Ejemplos**:
```
/party create
/party invite Alex
/party accept
/party info
/party chat Vamos a la dungeon!
/party leave
```

**Output de `/party info`**:
```
§6===== §eParty Info §6=====
§aLíder: §fSteve
§aMiembros: §f4/5

§f1. §aSteve §7(Leader) §f- Nivel 45
§f2. §fAlex §f- Nivel 42
§f3. §fHerobrine §f- Nivel 38
§f4. §fNotch §f- Nivel 50

§eXP Compartida: §aActivada §7(+10% bonus)
```

---

### `/class` - Cambiar Clase
Permite cambiar de clase.

**Uso**:
```
/class                   - Ver clase actual
/class info <clase>      - Información de una clase
/class change <clase>    - Cambiar de clase
```

**Clases disponibles**:
- `warrior` - Alto daño cuerpo a cuerpo
- `mage` - Alto daño mágico
- `archer` - Alto daño a distancia
- `rogue` - Alta velocidad y críticos

**Permisos**:
- `mmorpg.class` - Ver clase actual
- `mmorpg.class.change` - Cambiar de clase (cuesta monedas)

**Ejemplos**:
```
/class
/class info mage
/class change mage
```

**Output de `/class info mage`**:
```
§6===== §eClase: Mage §6=====

§9✦ Descripción:
§7Maestro de las artes arcanas. Inflige alto daño
§7mágico a distancia y posee habilidades de AoE.

§9✦ Bonificadores:
§b+20% Mana Máximo
§b+15% Daño Mágico
§c-10% Vida Máxima
§c-20% Defensa Física

§9✦ Habilidades Exclusivas:
§e⚡ Fireball §7- Dispara una bola de fuego
§e❄ Ice Spikes §7- Congela enemigos en área
§e🌟 Arcane Shield §7- Escudo mágico temporal

§6Costo de Cambio: §f5000 monedas
```

---

### `/rank` - Sistema de Rangos
Gestiona el sistema de rangos.

**Subcomandos**:
```
/rank                    - Ver rango actual
/rank list               - Lista todos los rangos
/rank ascend             - Ascender al siguiente rango
/rank info <rango>       - Información de un rango
```

**Permisos**:
- `mmorpg.rank` - Ver rango actual
- `mmorpg.rank.ascend` - Ascender de rango

**Ejemplos**:
```
/rank
/rank list
/rank ascend
/rank info Master
```

**Output de `/rank`**:
```
§6===== §eTu Rango §6=====

§aRango Actual: §eExpert §7(Nivel 4)

§6✦ Bonificadores:
§c+60% Daño
§a+60% Experiencia Ganada

§6✦ Progreso al Siguiente Rango:
§fRequiere: §e50,000 monedas
§fRequiere: §e5 Diamantes

§7Usa §e/rank ascend §7para ascender a §bMaster
```

---

### `/pet` - Sistema de Mascotas
Gestiona mascotas del jugador.

**Subcomandos**:
```
/pet list                - Lista tus mascotas
/pet adopt <tipo>        - Adopta una mascota
/pet summon <nombre>     - Invoca una mascota
/pet dismiss             - Despide mascota activa
/pet info <nombre>       - Información de mascota
/pet train <nombre>      - Entrena una mascota
/pet mount               - Monta la mascota (si es montable)
```

**Permisos**:
- `mmorpg.pet` - Sistema básico de mascotas
- `mmorpg.pet.adopt` - Adoptar mascotas
- `mmorpg.pet.mount` - Montar mascotas

**Ejemplos**:
```
/pet list
/pet adopt WOLF
/pet summon Buddy
/pet info Buddy
/pet train Buddy
/pet mount
/pet dismiss
```

**Output de `/pet info Buddy`**:
```
§6===== §eMascota: Buddy §6=====

§aTipo: §fLobo
§aNivel: §f15 §7(§e2300/3000 XP§7)

§6✦ Stats:
§cVida: §f45.0/45.0
§cDaño: §f12.0
§bVelocidad: §f0.4

§6✦ Habilidades:
§e🦴 Fetch §7- Recoge items caídos
§e⚔ Attack §7- Ataca a enemigos cercanos

§aMontable: §cNo
§aActiva: §aRí
```

---

### `/achievement` - Logros
Gestiona el sistema de logros.

**Subcomandos**:
```
/achievement list        - Lista todos los logros
/achievement progress    - Ver progreso de logros
/achievement info <id>   - Información de un logro
```

**Permisos**:
- `mmorpg.achievement` - Ver logros

**Ejemplos**:
```
/achievement list
/achievement progress
/achievement info first_kill
```

**Output de `/achievement list`**:
```
§6===== §eLogros §6=====

§a✔ Primera Sangre §7- Mata tu primer mob
  §6Recompensa: §f50 monedas, 20 XP

§a✔ Minero Novato §7- Mina 100 bloques de piedra
  §6Recompensa: §f100 monedas, 50 XP

§c✗ Maestro Guerrero §7- Alcanza nivel 50
  §aProgreso: §f45/50 §7(§e90%§7)
  §6Recompensa: §f1000 monedas, 500 XP

§7Logros Desbloqueados: §e23/50 §7(§a46%§7)
```

---

### `/bestiary` - Bestiario
Consulta el bestiario de mobs.

**Subcomandos**:
```
/bestiary                - Ver resumen del bestiario
/bestiary <tipo>         - Ver stats de un tipo de mob
/bestiary progress       - Ver progreso de completado
```

**Permisos**:
- `mmorpg.bestiary` - Acceso al bestiario

**Ejemplos**:
```
/bestiary
/bestiary ZOMBIE
/bestiary progress
```

**Output de `/bestiary`**:
```
§6===== §eBestiario §6=====

§a✔ §fZombie §7- §e523/100 kills §a(COMPLETADO)
§a✔ §fSkeleton §7- §e387/100 kills §a(COMPLETADO)
§e⚡ §fCreeper §7- §e87/100 kills §7(§e87%§7)
§e⚡ §fEnderman §7- §e45/100 kills §7(§e45%§7)
§c✗ §fWither Skeleton §7- §e12/50 kills §7(§e24%§7)
§c✗ §fEnder Dragon §7- §e0/1 kills §7(§e0%§7)

§7Tipos Completados: §e8/12 §7(§a67%§7)
§6Recompensa Total Ganada: §f1200 monedas, 480 XP
```

---

### `/dungeon` - Mazmorras
Gestiona instancias de dungeon.

**Subcomandos**:
```
/dungeon list            - Lista dungeons disponibles
/dungeon join <nombre>   - Unirse a una dungeon
/dungeon leave           - Salir de la dungeon
/dungeon info <nombre>   - Información de dungeon
/dungeon leaderboard     - Top completadores
```

**Permisos**:
- `mmorpg.dungeon` - Acceso a dungeons
- `mmorpg.dungeon.join` - Unirse a dungeons

**Ejemplos**:
```
/dungeon list
/dungeon join Crypta_Oscura
/dungeon info Crypta_Oscura
/dungeon leave
```

**Output de `/dungeon info Crypta_Oscura`**:
```
§6===== §eDungeon: Crypta Oscura §6=====

§cDificultad: §fMedio
§aNivel Requerido: §f25
§aJugadores: §f2-5

§6✦ Descripción:
§7Una antigua cripta llena de muertos vivientes.
§7Sobrevive a 5 oleadas de enemigos y derrota
§7al boss final para obtener grandes recompensas.

§6✦ Oleadas:
§f1. §7Zombies (x5)
§f2. §7Skeletons (x7)
§f3. §7Zombies + Skeletons (x10)
§f4. §7Wither Skeletons (x5)
§f5. §c§lBOSS: Rey Lich §r§7(500 HP)

§6✦ Recompensas:
§f• 5000 monedas
§f• 2500 XP
§f• Diamante x10
§f• Loot exclusivo de dungeon

§eCooldown: §f60 minutos
§aTu Cooldown: §aDisponible
```

---

## Comandos de Administrador

### `/rpg` - Comando Principal de Admin
Comando maestro de administración.

**Subcomandos**:
```
/rpg reload              - Recarga el plugin
/rpg save                - Guarda todos los datos
/rpg backup              - Crea backup manual
/rpg stats               - Estadísticas del servidor
/rpg debug <on|off>      - Activa/desactiva debug
```

**Permisos**:
- `mmorpg.admin` - Acceso completo a comandos admin

**Ejemplos**:
```
/rpg reload
/rpg save
/rpg stats
/rpg debug on
```

**Output de `/rpg stats`**:
```
§6===== §eEstadísticas del Servidor §6=====

§aJugadores Registrados: §f1,523
§aJugadores Online: §f45/100
§aNivel Promedio: §f32.5

§eEconomía:
§6Circulación Total: §f1,542,350.75 monedas
§6Transacciones Hoy: §f1,205

§eQuests:
§aQuests Activas: §f48
§aCompletadas Hoy: §f187

§eMobs:
§cMobs Personalizados: §f32 tipos
§cKills Totales Hoy: §f15,420
```

---

### `/rpgadmin player` - Gestión de Jugadores
Administra jugadores del servidor.

**Subcomandos**:
```
/rpgadmin player info <jugador>          - Ver info completa
/rpgadmin player setlevel <jugador> <lvl> - Establecer nivel
/rpgadmin player setcoins <jugador> <qty> - Establecer monedas
/rpgadmin player addcoins <jugador> <qty> - Añadir monedas
/rpgadmin player removecoins <jugador> <qty> - Quitar monedas
/rpgadmin player setclass <jugador> <clase> - Cambiar clase
/rpgadmin player reset <jugador>         - Resetear progreso
/rpgadmin player tp <jugador>            - Teletransportar
```

**Permisos**:
- `mmorpg.admin.player` - Gestión de jugadores

**Ejemplos**:
```
/rpgadmin player info Steve
/rpgadmin player setlevel Steve 50
/rpgadmin player addcoins Steve 10000
/rpgadmin player setclass Steve mage
/rpgadmin player reset Steve
```

---

### `/rpgadmin quest` - Gestión de Quests
Administra el sistema de quests.

**Subcomandos**:
```
/rpgadmin quest create <nombre>          - Crea una quest
/rpgadmin quest edit <id>                - Edita una quest
/rpgadmin quest delete <id>              - Elimina una quest
/rpgadmin quest assign <jugador> <id>    - Asigna quest a jugador
/rpgadmin quest complete <jugador> <id>  - Completa quest de jugador
/rpgadmin quest reset <jugador> <id>     - Resetea progreso de quest
/rpgadmin quest reload                   - Recarga quests desde BD
```

**Permisos**:
- `mmorpg.admin.quest` - Gestión de quests

**Ejemplos**:
```
/rpgadmin quest create EventoHalloween
/rpgadmin quest assign Steve 5
/rpgadmin quest complete Steve 5
/rpgadmin quest reload
```

---

### `/rpgadmin mob` - Gestión de Mobs
Administra mobs personalizados.

**Subcomandos**:
```
/rpgadmin mob spawn <tipo> [qty]         - Spawna mob personalizado
/rpgadmin mob edit <tipo>                - Edita stats de mob
/rpgadmin mob delete <tipo>              - Elimina mob personalizado
/rpgadmin mob reload                     - Recarga mobs desde BD
/rpgadmin mob list                       - Lista todos los mobs
```

**Permisos**:
- `mmorpg.admin.mob` - Gestión de mobs

**Ejemplos**:
```
/rpgadmin mob spawn ZOMBIE_ELITE 5
/rpgadmin mob list
/rpgadmin mob reload
```

---

### `/rpgadmin economy` - Gestión Económica
Administra la economía del servidor.

**Subcomandos**:
```
/rpgadmin economy stats                  - Ver estadísticas
/rpgadmin economy top [cantidad]         - Top jugadores ricos
/rpgadmin economy reset                  - Resetear economía (peligroso)
/rpgadmin economy audit <jugador>        - Ver historial de transacciones
```

**Permisos**:
- `mmorpg.admin.economy` - Gestión económica

**Ejemplos**:
```
/rpgadmin economy stats
/rpgadmin economy top 10
/rpgadmin economy audit Steve
```

---

### `/rpgadmin invasion` - Gestión de Invasiones
Controla eventos de invasión.

**Subcomandos**:
```
/rpgadmin invasion start <nombre>        - Inicia invasión
/rpgadmin invasion stop                  - Detiene invasión activa
/rpgadmin invasion schedule <nombre> <tiempo> - Programa invasión
/rpgadmin invasion list                  - Lista invasiones configuradas
```

**Permisos**:
- `mmorpg.admin.invasion` - Gestión de invasiones

**Ejemplos**:
```
/rpgadmin invasion start ZombieApocalypse
/rpgadmin invasion stop
/rpgadmin invasion schedule ZombieApocalypse 3600
```

---

### `/rpgadmin database` - Gestión de Base de Datos
Administra la base de datos.

**Subcomandos**:
```
/rpgadmin database backup                - Backup manual
/rpgadmin database vacuum                - Optimiza BD
/rpgadmin database stats                 - Ver estadísticas de BD
/rpgadmin database query <sql>           - Ejecuta query SQL (peligroso)
```

**Permisos**:
- `mmorpg.admin.database` - Gestión de BD (muy peligroso)

**Ejemplos**:
```
/rpgadmin database backup
/rpgadmin database vacuum
/rpgadmin database stats
```

---

## Permisos Completos

### Permisos de Jugador
```yaml
mmorpg.stats: true
mmorpg.stats.others: op
mmorpg.quest: true
mmorpg.quest.start: true
mmorpg.quest.abandon: true
mmorpg.shop: true
mmorpg.shop.sell: true
mmorpg.shop.discount: false
mmorpg.skill: true
mmorpg.skill.upgrade: true
mmorpg.skill.reset: true
mmorpg.party: true
mmorpg.party.leader: true
mmorpg.class: true
mmorpg.class.change: true
mmorpg.rank: true
mmorpg.rank.ascend: true
mmorpg.pet: true
mmorpg.pet.adopt: true
mmorpg.pet.mount: true
mmorpg.achievement: true
mmorpg.bestiary: true
mmorpg.dungeon: true
mmorpg.dungeon.join: true
```

### Permisos de Administrador
```yaml
mmorpg.admin: op
mmorpg.admin.player: op
mmorpg.admin.quest: op
mmorpg.admin.mob: op
mmorpg.admin.economy: op
mmorpg.admin.invasion: op
mmorpg.admin.database: op
mmorpg.admin.bypass: op
```

---

## Aliases

Muchos comandos tienen aliases para facilitar su uso:

```yaml
/stats → /st, /status
/quest → /q, /quests, /mission
/shop → /tienda, /store
/skill → /s, /skills, /habilidad
/party → /p, /group, /grupo
/class → /clase
/rank → /rango
/pet → /mascota
/achievement → /logro, /ach
/bestiary → /bestiario, /mobs
/dungeon → /dg, /mazmorra
```

---

## Configuración en plugin.yml

```yaml
commands:
  stats:
    description: Ver estadísticas del jugador
    usage: /stats [jugador]
    aliases: [st, status]
    permission: mmorpg.stats
    
  quest:
    description: Sistema de misiones
    usage: /quest <subcomando>
    aliases: [q, quests, mission]
    permission: mmorpg.quest
    
  # ... resto de comandos ...
```

---

## Tips y Trucos

### Autocompletado
Todos los comandos soportan **Tab Completion** para facilitar el uso:

```
/quest <TAB>        → list, active, start, abandon, info, complete
/shop <TAB>         → weapons, armor, potions, sell, buy
/skill upgrade <TAB> → swordmastery, mining, archery, ...
```

### Macros Útiles
Configura macros en tu cliente para comandos frecuentes:

```
F1 → /stats
F2 → /quest active
F3 → /skill list
F4 → /party info
```

### Colores en Chat
Los mensajes del plugin usan códigos de color para mejor legibilidad:
- §a Verde: Mensajes positivos, éxito
- §c Rojo: Mensajes de error, peligro
- §e Amarillo: Información importante
- §7 Gris: Información secundaria
- §6 Dorado: Recompensas, títulos

---

## Solución de Problemas

### Comando no funciona
1. Verificar permisos: `/luckperms user <tu_nombre> permission check mmorpg.quest`
2. Revisar sintaxis: Usar Tab Completion
3. Ver logs del servidor para errores

### "You don't have permission"
Contacta a un administrador para que te otorgue el permiso necesario.

### Quest no se completa automáticamente
Usa `/quest complete <id>` cuando hayas cumplido todos los objetivos.

### Stats no se actualizan
Ejecuta `/stats` de nuevo o relog al servidor.

---

## Changelog de Comandos

### v1.0.0
- Comandos básicos: stats, quest, shop, skill, party
- Comandos admin: rpgadmin player, quest, mob

### v1.1.0 (Actual)
- Añadido: /class, /rank, /pet
- Añadido: /achievement, /bestiary
- Añadido: /dungeon
- Añadido: rpgadmin invasion, economy, database
- Mejorado: Tab completion completo
- Mejorado: Mensajes de ayuda más detallados
