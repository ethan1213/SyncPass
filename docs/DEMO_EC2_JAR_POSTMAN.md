# Despliegue en EC2 con JDK y demostración

```text
React (Vercel) ──HTTPS──► API Gateway ──HTTPS──► Caddy :443 ──► JAR Spring Boot :8080 (EC2)
                                                                    ├──► Amazon Cognito
                                                                    └──► Supabase PostgreSQL
```

Vercel solo sirve el frontend; el JAR corre en EC2 con **Amazon Corretto 21 (JDK)** como
servicio `systemd`, sin Docker.

| Recurso | Valor |
| --- | --- |
| Instancia EC2 | `i-01d851d6163897cb1` (`us-east-1`) |
| API Gateway | `https://hluamat8vg.execute-api.us-east-1.amazonaws.com` |
| Frontend | `https://web-react-nu-nine.vercel.app` |

## 1. Antes de empezar

1. Iniciar el laboratorio de AWS Academy y arrancar la instancia si está detenida.
2. Anotar la **IPv4 pública**: cambia después de cada stop/start.
3. El grupo de seguridad debe permitir **80** y **443** (el 8080 no se abre).
4. En Cognito, el cliente de la app debe tener `ALLOW_USER_PASSWORD_AUTH`
   (ver [`COGNITO.md`](COGNITO.md)).

## 2. Instalar o actualizar la API

Conectarse a la instancia (EC2 Instance Connect o Session Manager) y ejecutar:

```bash
curl -fsSLO https://raw.githubusercontent.com/ethan1213/SyncPass/main/deploy/ec2-jdk-setup.sh
sudo bash ec2-jdk-setup.sh main
```

El script [`deploy/ec2-jdk-setup.sh`](../deploy/ec2-jdk-setup.sh) es idempotente:

1. Instala `java-21-amazon-corretto-devel` (JDK 21), Apache Maven 3.9 y git.
2. Clona o actualiza el repositorio en `/opt/syncpass/source` y compila
   `syncpass-api-1.0.0.jar`.
3. Crea `/etc/syncpass/syncpass.env`. Si existía un `.env` de Supabase del despliegue
   anterior con Docker, lo reutiliza; si no, deja la plantilla y se detiene para que
   la completes.
4. Registra el servicio `syncpass-api` y lo deja escuchando solo en `127.0.0.1:8080`.
5. Detiene los contenedores antiguos `syncpass-api` y `syncpass-caddy`, instala Caddy
   nativo y publica `https://api.<IP-con-guiones>.nip.io`.
6. Espera a que `/actuator/health` responda `UP` e imprime el dominio.

### Variables de `/etc/syncpass/syncpass.env`

```ini
DATABASE_URL=jdbc:postgresql://<pooler>.supabase.com:5432/postgres?sslmode=require&currentSchema=syncpass
DATABASE_USER=postgres.<project-ref>
DATABASE_PASSWORD=<clave de Supabase>
COGNITO_ISSUER=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_2C9bbmKGd
COGNITO_CLIENT_ID=677bhbj1qg892bmlk2pr1h6had
CORS_ORIGINS=http://localhost:3000,https://web-react-nu-nine.vercel.app
```

El archivo queda con permisos `640` (root:syncpass). **Nunca se sube al repositorio.**

### Si cambió la IP pública

Actualizar la integración de API Gateway para que apunte al nuevo
`https://api.<IP-con-guiones>.nip.io` (lo imprime el script al terminar).

### Comandos útiles

```bash
systemctl status syncpass-api           # estado del JAR
journalctl -u syncpass-api -n 100 -f    # logs de Spring Boot
java -version                           # JDK instalado
sudo systemctl restart syncpass-api     # reiniciar tras cambiar el .env
```

## 3. Vercel

En **Settings → Environment Variables** solo hace falta:

| Variable | Valor |
| --- | --- |
| `VITE_API_URL` | `https://hluamat8vg.execute-api.us-east-1.amazonaws.com` |

Las antiguas `VITE_COGNITO_*` ya no se usan y se pueden borrar. Tras cambiarlas,
volver a desplegar.

## 4. Demostración en el navegador

1. Abrir `https://web-react-nu-nine.vercel.app/registro` con las **DevTools → Red** abiertas.
2. Completar usuario, correo, apodo y contraseña, y pulsar **Crear cuenta**.
3. Ingresar el código que llega al correo y pulsar **Confirmar e ingresar**.
4. Se muestra **Sesión iniciada correctamente** con el correo del usuario.

En la pestaña Red solo aparecen peticiones a `execute-api.us-east-1.amazonaws.com`
(`/api/auth/register`, `/api/auth/confirm`, `/api/auth/login`). **Ninguna va a
`cognito`**: la API en EC2 es la que se comunica con Cognito.

## 5. Demostración en Postman

Importar [`SyncPass-EC2.postman_collection.json`](postman/SyncPass-EC2.postman_collection.json)
y definir como variables locales `nuevo_usuario`, `nuevo_correo`, `nuevo_apodo`,
`nueva_clave` y `codigo_verificacion`.

| Request | Qué demuestra | Esperado |
| --- | --- | --- |
| 00A Registrar usuario | La API crea la cuenta en Cognito | `201` |
| 00B Confirmar código | La API confirma el correo | `204` |
| 00C Iniciar sesión | La API obtiene los tokens y los guarda en la colección | `200` |
| 00D Renovar token | Refresh a través de la API | `200` |
| 01 Salud del JAR | Spring Boot corriendo en EC2 | `200` `UP` |
| 02 Talleres públicos | Lectura desde Supabase | `200` |
| 03 Ruta protegida sin token | La API exige token | `401` |
| 04 Mi perfil | El token de Cognito es aceptado | `200` |
| 05 Crear taller | Escritura en Supabase (requiere grupo `admin` o `coordinador`) | `201` |
| 06 Leer taller creado | El dato quedó persistido | `200` |

No guardar contraseñas, códigos ni tokens en el repositorio.

## 6. Al terminar

Detener la instancia para ahorrar crédito del laboratorio. Mientras esté detenida,
API Gateway responde error y la parte dinámica de Vercel no funciona.

