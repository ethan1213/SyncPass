# Colaboración en SyncPass

`main` contiene la versión integrada que se presenta y publica. Las ramas personales parten de la misma versión:

| Rama | Uso |
| --- | --- |
| `developer/ethan` | Trabajo de Ethan antes de revisión. |
| `developer/jorge` | Trabajo de Jorge antes de revisión. |

Ramas de funcionalidad disponibles: `feature/autenticacion`, `feature/proyectos`, `feature/talleres`, `feature/mockups`, `feature/mobile` y `feature/despliegue`. Se crean como puntos de partida desde `main`; no representan autores ni una historia de cambios independiente. Las ramas antiguas permanecen como historial del repositorio.

Para colaborar: crear cambios en la rama personal o en una rama `feature/*`, subirla y abrir un pull request hacia `main`. La otra persona revisa el cambio antes de integrarlo. No subir archivos `.env`, claves, contraseñas ni tokens.
