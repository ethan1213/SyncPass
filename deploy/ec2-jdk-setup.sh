#!/bin/bash
# Instala JDK 21 (Amazon Corretto) + Apache Maven en EC2, compila el JAR y lo ejecuta como servicio systemd.
# Caddy (nativo) publica la API por HTTPS para que API Gateway la consuma. Idempotente: sirve para
# la primera instalación y para actualizar. Uso: sudo bash ec2-jdk-setup.sh [rama]
set -euo pipefail
BRANCH=${1:-main}
REPO=https://github.com/ethan1213/SyncPass.git
APP=/opt/syncpass
ENV_FILE=/etc/syncpass/syncpass.env
CADDY_VERSION=2.8.4
MAVEN_VERSION=3.9.9
exec > >(tee -a /var/log/syncpass-setup.log) 2>&1

echo "==> Paquetes: JDK 21 y git"
dnf install -y java-21-amazon-corretto-devel git tar
java -version

if [ ! -f /swapfile ]; then
  echo "==> Swap de 1 GB (Maven necesita memoria en instancias pequeñas)"
  fallocate -l 1G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

echo "==> Código fuente ($BRANCH)"
mkdir -p "$APP"
if [ -d "$APP/source/.git" ]; then
  git -C "$APP/source" fetch --depth 1 origin "$BRANCH"
  git -C "$APP/source" checkout -B "$BRANCH" FETCH_HEAD
else
  git clone --depth 1 --branch "$BRANCH" "$REPO" "$APP/source"
fi

echo "==> Compilando el JAR"
# Maven oficial de Apache: el paquete "maven" de Amazon Linux fija Corretto 17 y no compila Java 21.
export JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
export MAVEN_SKIP_RC=1
alternatives --set java "$JAVA_HOME/bin/java" || true
if [ ! -x "/opt/maven-$MAVEN_VERSION/bin/mvn" ]; then
  curl -fsSL "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz" | tar -xz -C /opt
  mv "/opt/apache-maven-$MAVEN_VERSION" "/opt/maven-$MAVEN_VERSION"
fi
MVN="/opt/maven-$MAVEN_VERSION/bin/mvn"
"$MVN" -v
"$MVN" -B -q -f "$APP/source/backend/pom.xml" -DskipTests package
install -m 644 "$APP/source/backend/target/syncpass-api-1.0.0.jar" "$APP/syncpass-api.jar"

echo "==> Usuario y variables de entorno"
id syncpass >/dev/null 2>&1 || useradd --system --no-create-home --shell /sbin/nologin syncpass
mkdir -p /etc/syncpass
if [ ! -f "$ENV_FILE" ]; then
  # Reutiliza la configuración de Supabase del despliegue anterior con Docker, si existe.
  for old in "$APP/.env" "$APP/runtime.env"; do
    if [ -f "$old" ] && grep -q '^DATABASE_URL=jdbc:postgresql://.*supabase' "$old"; then
      grep -E '^(DATABASE_URL|DATABASE_USER|DATABASE_PASSWORD|COGNITO_ISSUER|COGNITO_CLIENT_ID|CORS_ORIGINS)=' "$old" > "$ENV_FILE"
      echo "    Configuración copiada desde $old"
      break
    fi
  done
fi
if [ ! -f "$ENV_FILE" ]; then
  install -m 600 "$APP/source/deploy/ec2-supabase.env.example" "$ENV_FILE"
  echo "!! Completa $ENV_FILE con los datos de Supabase y vuelve a ejecutar este script."
  exit 1
fi
grep -q '^SERVER_ADDRESS=' "$ENV_FILE" || echo 'SERVER_ADDRESS=127.0.0.1' >> "$ENV_FILE"
grep -q '^AWS_REGION=' "$ENV_FILE" || echo 'AWS_REGION=us-east-1' >> "$ENV_FILE"
CORS_REQUIRED="http://localhost:3000,https://web-react-nu-nine.vercel.app"
grep -q '^CORS_ORIGINS=.*web-react-nu-nine.vercel.app' "$ENV_FILE" || { sed -i '/^CORS_ORIGINS=/d' "$ENV_FILE"; echo "CORS_ORIGINS=$CORS_REQUIRED" >> "$ENV_FILE"; }
chown root:syncpass "$ENV_FILE" && chmod 640 "$ENV_FILE"

echo "==> Servicio systemd syncpass-api"
cat > /etc/systemd/system/syncpass-api.service <<EOF
[Unit]
Description=SyncPass API (Spring Boot)
After=network-online.target
Wants=network-online.target

[Service]
User=syncpass
EnvironmentFile=$ENV_FILE
ExecStart=$JAVA_HOME/bin/java -XX:MaxRAMPercentage=65 -jar $APP/syncpass-api.jar
Restart=on-failure
RestartSec=5
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
EOF

# El despliegue anterior usaba contenedores en los mismos puertos: se detienen para liberar 80/443.
if command -v docker >/dev/null 2>&1; then
  for c in syncpass-api syncpass-caddy; do
    docker update --restart=no "$c" >/dev/null 2>&1 && docker stop "$c" >/dev/null 2>&1 && echo "    Contenedor $c detenido" || true
  done
fi

echo "==> Caddy $CADDY_VERSION (HTTPS para API Gateway)"
if ! /usr/local/bin/caddy version 2>/dev/null | grep -q "v$CADDY_VERSION"; then
  ARCH=$(uname -m); [ "$ARCH" = aarch64 ] && ARCH=arm64 || ARCH=amd64
  curl -fsSL "https://github.com/caddyserver/caddy/releases/download/v$CADDY_VERSION/caddy_${CADDY_VERSION}_linux_$ARCH.tar.gz" | tar -xz -C /usr/local/bin caddy
fi
id caddy >/dev/null 2>&1 || useradd --system --home-dir /var/lib/caddy --create-home --shell /sbin/nologin caddy
TOKEN=$(curl -fsS -X PUT -H 'X-aws-ec2-metadata-token-ttl-seconds: 300' http://169.254.169.254/latest/api/token)
PUBLIC_IP=$(curl -fsS -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/public-ipv4)
API_DOMAIN="api.${PUBLIC_IP//./-}.nip.io"
mkdir -p /etc/caddy
cat > /etc/caddy/Caddyfile <<EOF
$API_DOMAIN {
  encode gzip
  # API Gateway (HTTP API) agrega las cabeceras CORS; el origen solo confirma el preflight.
  @preflight {
    method OPTIONS
    path /api/*
  }
  @api path /api/* /actuator/health
  route {
    respond @preflight "" 204
    handle @api {
      reverse_proxy 127.0.0.1:8080
    }
    handle {
      respond "Not found" 404
    }
  }
  header {
    X-Content-Type-Options nosniff
    Referrer-Policy strict-origin-when-cross-origin
  }
}
EOF
/usr/local/bin/caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
cat > /etc/systemd/system/caddy.service <<'EOF'
[Unit]
Description=Caddy
After=network-online.target
Wants=network-online.target

[Service]
User=caddy
ExecStart=/usr/local/bin/caddy run --config /etc/caddy/Caddyfile --adapter caddyfile
ExecReload=/usr/local/bin/caddy reload --config /etc/caddy/Caddyfile --adapter caddyfile
AmbientCapabilities=CAP_NET_BIND_SERVICE
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable syncpass-api caddy
systemctl restart syncpass-api caddy

echo "==> Esperando a Spring Boot"
for i in $(seq 1 60); do
  if curl -fsS http://127.0.0.1:8080/actuator/health 2>/dev/null | grep -q UP; then
    echo "SYNCPASS_API_UP=https://$API_DOMAIN (origen de la integración de API Gateway)"
    exit 0
  fi
  sleep 3
done
echo "!! La API no respondió. Revisa: journalctl -u syncpass-api -n 100"
exit 1

