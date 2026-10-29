# Operación del laboratorio AWS Academy

## Qué se mantiene y qué cambia

La URL pública de API Gateway es estable: `https://hluamat8vg.execute-api.us-east-1.amazonaws.com`. La instancia `i-01d851d6163897cb1` recibe una IPv4 pública distinta cada vez que el laboratorio se detiene y vuelve a iniciar. Caddy usa `api.<IP-con-guiones>.nip.io` como dominio HTTPS; por eso la integración de Gateway debe seguir la IP nueva. La instancia no puede permanecer disponible mientras el laboratorio está apagado.

## Instalación única del arranque automático

Con el laboratorio activo, abrir **EC2 → Instancias → syncpass-api → Conectar → Session Manager**. Ejecutar:

```bash
curl -fsSLo /tmp/syncpass-setup.sh https://raw.githubusercontent.com/ethan1213/SyncPass/main/deploy/ec2-jdk-setup.sh
sudo bash /tmp/syncpass-setup.sh main
```

El instalador conserva `/etc/syncpass/syncpass.env`, actualiza el JAR desde este repositorio y habilita tres servicios `systemd`:

| Servicio | Función |
| --- | --- |
| `syncpass-api` | Inicia Spring Boot con JDK 21 y la configuración privada del servidor. |
| `caddy` | Recibe HTTPS en 443 y reenvía a `127.0.0.1:8080`. |
| `syncpass-gateway-sync` | Detecta la IPv4 por IMDSv2, ajusta Caddy, espera el certificado y actualiza la integración de API Gateway. |

El último servicio se ejecuta en cada inicio de EC2. Usa el rol de la instancia para AWS CLI; no guarda claves AWS en archivos. Si AWS o el certificado tardan en estar disponibles, `systemd` vuelve a intentarlo. El servicio registra `SYNCPASS_PUBLIC_IP=<IP>` y `SYNCPASS_GATEWAY_UP=<URL>` cuando termina correctamente.

## Cada vez que se enciende el laboratorio

1. Iniciar el laboratorio AWS Academy y comprobar que la instancia figure **En ejecución** con las tres comprobaciones aprobadas.
2. Esperar a que el servicio termine. En Session Manager, ejecutar:

```bash
sudo systemctl status syncpass-gateway-sync --no-pager
sudo journalctl -u syncpass-gateway-sync -n 30 --no-pager
```

3. La línea `SYNCPASS_PUBLIC_IP` da la IP actual. También aparece en **EC2 → Instancias → Dirección IPv4 pública**. Para pedirla en este chat, mantener abierto el panel EC2 o copiar solo la IP; no hace falta enviar claves.
4. Abrir `https://hluamat8vg.execute-api.us-east-1.amazonaws.com/actuator/health`: debe responder `{"status":"UP"}`. Después comprobar login y ambos mantenedores en Vercel.

La web de Vercel conserva su URL durante los apagados, pero no puede leer datos nuevos ni iniciar sesión mientras la API está fuera de servicio. La IP de EC2 cambia; compartir la URL estable de API Gateway para Postman y móvil.

## Publicar una nueva versión del JAR

Con Session Manager abierto y el laboratorio activo, después de subir los cambios a `main`:

```bash
curl -fsSLo /tmp/syncpass-update.sh https://raw.githubusercontent.com/ethan1213/SyncPass/main/deploy/update-lab.sh
sudo bash /tmp/syncpass-update.sh
```

El script descarga `main`, compila, instala el JAR, reinicia la API y comprueba la salud local y la de Gateway. Flyway aplica las migraciones al iniciar, incluida V3 de geocercas. Conserva `/etc/syncpass/syncpass.env` sin mostrar sus valores.

## Si Gateway responde 503

```bash
sudo systemctl status syncpass-api caddy syncpass-gateway-sync --no-pager
sudo journalctl -u syncpass-gateway-sync -n 80 --no-pager
sudo /usr/local/sbin/syncpass-fix-ip
```

Si `/usr/local/sbin/syncpass-fix-ip` aún no existe, ejecutar la instalación única. Si falla el acceso a AWS, comprobar que la instancia conserva su rol IAM del laboratorio. Si falla el certificado, revisar el estado de Caddy, los puertos 80/443 del grupo de seguridad y la resolución del nombre `nip.io`. No abrir el puerto 8080 al público ni colocar credenciales en el repositorio.

## Alcance del costo

El servicio de recuperación no mantiene EC2 encendida ni crea una IP elástica. Solo trabaja cuando el laboratorio inicia. Detener el laboratorio según las indicaciones del curso evita consumir crédito innecesario. Un laboratorio apagado no puede servir la API; no existe una configuración gratuita dentro de esta EC2 que elimine esa dependencia.
