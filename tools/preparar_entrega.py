"""Exporta repositorios de revisión a partir de un commit del proyecto integral."""

from io import BytesIO
from pathlib import Path
import argparse
import subprocess
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parents[1]
INTEGRAL = "https://github.com/ethan1213/SyncPass"
COMMON = (".gitignore", ".gitattributes", "docs/ARQUITECTURA.md", "docs/ALCANCE.md")
EXPORTS = {
    "SyncPass-Web": (
        "Cliente React",
        ("web-react",),
        "Desde `web-react`, ejecutar `npm ci`, `npm run dev`, `npm run build` y `npm run coverage`. "
        "Para usar el entorno publicado, configurar `VITE_API_URL` con la URL de Gateway.",
    ),
    "SyncPass-Android": (
        "Cliente Kotlin",
        ("mobile-kotlin",),
        "Abrir `mobile-kotlin` en Android Studio o ejecutar allí "
        "`gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:jacocoFilteredTestReport`. "
        "El APK queda en `app/build/outputs/apk/debug`.",
    ),
    "SyncPass-API": (
        "API Spring Boot",
        ("backend", "database", "deploy/ec2-supabase.env.example"),
        "Desde `backend`, ejecutar `mvnw.cmd test` y `mvnw.cmd -DskipTests package`. "
        "Los scripts para arrancar el conjunto con Compose y actualizar el laboratorio viven en el repositorio integral.",
    ),
    "SyncPass-Docs": (
        "Documentación y mockups",
        ("docs", "img", "css", "mockups.html", "SyncPass-Mockups-y-BD.html", "output/pdf"),
        "Empezar por [la matriz de Prueba 3](docs/ENTREGA_PRUEBA_3.md), "
        "[el índice documental](docs/README.md) y [el visor publicado](https://web-react-nu-nine.vercel.app/mockups.html).",
    ),
}


def git(*args: str) -> bytes:
    return subprocess.check_output(["git", *args], cwd=ROOT)


def exportar(ref: str, destino: Path) -> None:
    commit = git("rev-parse", "--verify", f"{ref}^{{commit}}").decode().strip()
    for name, (title, paths, commands) in EXPORTS.items():
        folder = destino / name
        folder.mkdir(parents=True, exist_ok=False)
        scope = ("docs", ".gitignore", ".gitattributes", *paths[1:]) if name == "SyncPass-Docs" else (*COMMON, *paths)
        archive = git("archive", "--format=zip", commit, "--", *scope)
        with ZipFile(BytesIO(archive)) as bundle:
            for entry in bundle.infolist():
                path = Path(entry.filename)
                if path.name == "SyncPass-Android.apk" or path.name == "caso.pdf":
                    continue
                bundle.extract(entry, folder)
        related = "\n".join(
            f"- [{other}](https://github.com/ethan1213/{other})" for other in EXPORTS if other != name
        )
        readme = f"""# {name} · {title}

**Responsable:** Ethan Astorga.

Copia de revisión de [SyncPass integral]({INTEGRAL}/tree/{commit}), commit `{commit}`.
El repositorio integral reúne el entorno Docker y la versión de referencia del proyecto.

## Revisión

{commands}

- [Arquitectura C4](docs/ARQUITECTURA.md)
- [Alcance](docs/ALCANCE.md)
- [Web publicada](https://web-react-nu-nine.vercel.app/)
- [Descarga de Android](https://web-react-nu-nine.vercel.app/descargas/SyncPass-Android.apk)

## Repositorios de la entrega

{related}
"""
        (folder / "README.md").write_text(readme, encoding="utf-8")
        print(f"{name}: {folder}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("destino", type=Path, help="Carpeta vacía para los cuatro repositorios")
    parser.add_argument("--ref", default="HEAD", help="Commit o rama del repositorio integral")
    args = parser.parse_args()
    exportar(args.ref, args.destino.resolve())
