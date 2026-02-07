# Contributing to Minecraft MMORPG

¡Gracias por tu interés en contribuir al proyecto! Este documento proporciona guías y estándares para contribuir.

## 📋 Tabla de Contenidos

- [Código de Conducta](#código-de-conducta)
- [¿Cómo Puedo Contribuir?](#cómo-puedo-contribuir)
- [Guía de Estilo](#guía-de-estilo)
- [Proceso de Pull Request](#proceso-de-pull-request)
- [Reporte de Bugs](#reporte-de-bugs)
- [Sugerencias de Features](#sugerencias-de-features)

## 📜 Código de Conducta

Este proyecto y todos los participantes están gobernados por el Código de Conducta. Al participar, se espera que mantengas este código. Por favor reporta comportamiento inaceptable.

### Nuestros Estándares

**Comportamientos que contribuyen a crear un ambiente positivo**:
- Usar lenguaje acogedor e inclusivo
- Respetar puntos de vista y experiencias diferentes
- Aceptar crítica constructiva de manera amigable
- Enfocarse en lo que es mejor para la comunidad
- Mostrar empatía hacia otros miembros de la comunidad

**Comportamientos inaceptables**:
- Uso de lenguaje o imágenes sexualizadas
- Trolling, comentarios insultantes/despectivos
- Acoso público o privado
- Publicar información privada de otros sin permiso
- Otras conductas que puedan considerarse inapropiadas

## 🤝 ¿Cómo Puedo Contribuir?

### Reportar Bugs

Antes de crear un reporte de bug:
- Verifica que no exista ya un issue sobre el mismo problema
- Recopila información sobre el bug (logs, pasos para reproducir, versiones)

Cuando crees un reporte de bug, incluye:
- **Título descriptivo**
- **Pasos para reproducir** el problema
- **Comportamiento esperado** vs **comportamiento actual**
- **Screenshots** o logs si es aplicable
- **Información del entorno** (OS, Java version, Paper version)

### Sugerir Mejoras

Las sugerencias son bienvenidas. Cuando sugieras una mejora:
- Usa un título claro y descriptivo
- Proporciona una descripción detallada de la mejora sugerida
- Explica por qué esta mejora sería útil
- Incluye ejemplos o mockups si es aplicable

### Tu Primera Contribución de Código

¿No sabes por dónde empezar? Busca issues etiquetados como:
- `good first issue` - Issues que requieren pocas líneas de código
- `help wanted` - Issues que necesitan atención

### Pull Requests

1. **Fork** el repositorio
2. **Crea una rama** desde `main`:
   ```bash
   git checkout -b feature/mi-nueva-feature
   ```
3. **Haz tus cambios** siguiendo la guía de estilo
4. **Commit** tus cambios:
   ```bash
   git commit -m "feat: añadir nueva feature X"
   ```
5. **Push** a tu fork:
   ```bash
   git push origin feature/mi-nueva-feature
   ```
6. **Abre un Pull Request** en GitHub

## 🎨 Guía de Estilo

### Código Java

#### Convenciones
- **Indentación**: 4 espacios (no tabs)
- **Nombres de clases**: `PascalCase`
- **Nombres de métodos**: `camelCase`
- **Nombres de constantes**: `UPPER_SNAKE_CASE`
- **Nombres de paquetes**: `lowercase`

#### Ejemplo
```java
package com.nightslayer.mmorpg.quests;

import org.bukkit.entity.Player;
import java.util.UUID;

/**
 * Manages quest operations for players.
 */
public class QuestManager {
    private static final int MAX_ACTIVE_QUESTS = 5;
    private final Map<UUID, List<Quest>> activeQuests;
    
    /**
     * Assigns a quest to a player.
     *
     * @param player The player to assign the quest to
     * @param questId The ID of the quest
     * @return true if quest was assigned successfully
     */
    public boolean assignQuest(Player player, String questId) {
        // Implementation
    }
}
```

#### Best Practices
- Usar JavaDoc para métodos públicos
- Evitar campos públicos (usar getters/setters)
- Usar `Optional` en lugar de null cuando sea apropiado
- Usar try-with-resources para AutoCloseable
- **CRÍTICO**: NO cerrar Connection en DatabaseManager (es singleton)

### Código Python

#### Convenciones
- **Indentación**: 4 espacios
- **Nombres de funciones**: `snake_case`
- **Nombres de clases**: `PascalCase`
- **Nombres de constantes**: `UPPER_SNAKE_CASE`
- Seguir [PEP 8](https://pep8.org/)

#### Ejemplo
```python
from typing import Optional, List
from flask import Flask, jsonify

class PlayerManager:
    """Manages player data operations."""
    
    MAX_PLAYERS = 100
    
    def __init__(self, db_path: str):
        """
        Initialize the PlayerManager.
        
        Args:
            db_path: Path to the SQLite database
        """
        self.db_path = db_path
    
    def get_player(self, uuid: str) -> Optional[dict]:
        """
        Retrieve player data by UUID.
        
        Args:
            uuid: The player's UUID
            
        Returns:
            Player data dictionary or None if not found
        """
        # Implementation
```

#### Best Practices
- Usar type hints cuando sea posible
- Usar docstrings (Google style)
- Usar f-strings para formateo
- Seguir principios SOLID

### Scripts Bash

#### Convenciones
```bash
#!/bin/bash
set -euo pipefail  # Exit on error, undefined vars, pipe failures

# Constants
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly LOG_FILE="/var/log/mmorpg.log"

# Functions
log_info() {
    echo "[INFO] $*" | tee -a "$LOG_FILE"
}

log_error() {
    echo "[ERROR] $*" >&2 | tee -a "$LOG_FILE"
}

# Main
main() {
    log_info "Starting installation..."
    # Implementation
}

main "$@"
```

#### Best Practices
- Usar `set -euo pipefail`
- Validar todas las entradas
- Usar funciones para código reutilizable
- Mensajes de error claros
- Documentar argumentos esperados

### Mensajes de Commit

Usar [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

#### Tipos
- `feat`: Nueva feature
- `fix`: Corrección de bug
- `docs`: Cambios en documentación
- `style`: Cambios de formato (no afectan código)
- `refactor`: Refactorización de código
- `perf`: Mejoras de rendimiento
- `test`: Añadir o modificar tests
- `chore`: Tareas de mantenimiento

#### Ejemplos
```
feat(quests): add daily quest system

fix(database): resolve connection pool leak

docs(readme): update installation instructions

refactor(economy): simplify transaction handling
```

### Documentación

- Documentar todas las funciones/métodos públicos
- Incluir ejemplos de uso cuando sea apropiado
- Mantener README.md actualizado
- Documentar cambios en CHANGELOG.md

## 🔄 Proceso de Pull Request

### Antes de Enviar

1. **Ejecutar tests**:
   ```bash
   # Java
   cd mmorpg-plugin
   mvn test
   
   # Python
   cd test
   ./run-tests.sh
   ```

2. **Verificar estilo**:
   ```bash
   # Java (opcional - checkstyle)
   mvn checkstyle:check
   
   # Python
   flake8 web/
   ```

3. **Actualizar documentación** si es necesario

### Checklist del PR

- [ ] El código sigue la guía de estilo del proyecto
- [ ] He comentado mi código, especialmente en áreas complejas
- [ ] He actualizado la documentación correspondiente
- [ ] Mis cambios no generan nuevos warnings
- [ ] He añadido tests que prueban que mi fix es efectivo o que mi feature funciona
- [ ] Los tests unitarios nuevos y existentes pasan localmente
- [ ] Cambios dependientes han sido mergeados y publicados

### Template de PR

```markdown
## Descripción
Breve descripción de los cambios

## Tipo de Cambio
- [ ] Bug fix (cambio que corrige un issue)
- [ ] Nueva feature (cambio que añade funcionalidad)
- [ ] Breaking change (fix o feature que causa que funcionalidad existente no funcione como se esperaba)
- [ ] Este cambio requiere actualización de documentación

## ¿Cómo se ha Testeado?
Descripción de los tests realizados

## Screenshots (si aplica)

## Checklist
- [ ] Mi código sigue la guía de estilo
- [ ] He realizado auto-review de mi código
- [ ] He comentado código complejo
- [ ] He actualizado la documentación
- [ ] Mis cambios no generan warnings
- [ ] He añadido tests
- [ ] Tests pasan localmente
```

### Revisión

El proceso de revisión incluye:
1. **Automated checks**: Tests, linters, builds
2. **Code review**: Al menos 1 aprobación requerida
3. **Testing**: Verificación manual si es necesario
4. **Documentation**: Verificar que la documentación esté actualizada

### Merge

Una vez aprobado:
- Usaremos **Squash and Merge** para mantener historial limpio
- El título del PR será el mensaje de commit
- Branch será eliminado después del merge

## 🐛 Reporte de Bugs

### Template de Bug Report

```markdown
**Descripción del Bug**
Descripción clara y concisa del bug

**Pasos para Reproducir**
1. Ir a '...'
2. Hacer click en '....'
3. Scroll hasta '....'
4. Ver error

**Comportamiento Esperado**
Descripción clara de qué esperabas que ocurriera

**Comportamiento Actual**
Descripción de qué ocurrió en realidad

**Screenshots**
Si aplica, añade screenshots

**Entorno**
- OS: [e.g. Ubuntu 22.04]
- Java Version: [e.g. 21.0.1]
- Paper Version: [e.g. 1.20.6 build 151]
- Plugin Version: [e.g. 1.0.0]

**Logs**
```
Pegar logs relevantes aquí
```

**Contexto Adicional**
Cualquier otro contexto sobre el problema
```

## ✨ Sugerencias de Features

### Template de Feature Request

```markdown
**¿Tu feature request está relacionada a un problema?**
Descripción clara del problema. Ej: Siempre me frustra cuando [...]

**Describe la solución que te gustaría**
Descripción clara y concisa de qué quieres que ocurra

**Describe alternativas que has considerado**
Descripción clara de soluciones o features alternativas

**Contexto Adicional**
Añade cualquier otro contexto o screenshots sobre el feature request
```

## 🏆 Reconocimiento

Los contribuidores serán reconocidos en:
- README.md (sección de contribuidores)
- Release notes
- CHANGELOG.md

## 📞 ¿Necesitas Ayuda?

- **GitHub Issues**: Para bugs y feature requests
- **GitHub Discussions**: Para preguntas y discusiones
- **Discord**: [Enlace al servidor de Discord]

## 📚 Recursos Adicionales

- [Paper API Documentation](https://jd.papermc.io/paper/1.20/)
- [Bukkit/Spigot API](https://hub.spigotmc.org/javadocs/bukkit/)
- [Flask Documentation](https://flask.palletsprojects.com/)
- [SQLite Documentation](https://www.sqlite.org/docs.html)

---

¡Gracias por contribuir! 🎉
