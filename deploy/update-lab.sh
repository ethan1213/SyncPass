#!/usr/bin/env bash
# Actualiza el JAR existente de SyncPass sin tocar la configuración privada.
set -euo pipefail
[ "$(id -u)" -eq 0 ] || { echo "Ejecuta con sudo" >&2; exit 1; }
SOURCE=/opt/syncpass/source
REPO=https://github.com/ethan1213/SyncPass.git
MAVEN=/opt/maven-3.9.9/bin/mvn
[ -d "$SOURCE/.git" ] && [ -x "$MAVEN" ] && [ -f /etc/syncpass/syncpass.env ] || {
  echo "Falta la instalación inicial; usa deploy/ec2-jdk-setup.sh" >&2; exit 1;
}
git -C "$SOURCE" remote set-url origin "$REPO"
git -C "$SOURCE" fetch --depth 1 origin main
git -C "$SOURCE" checkout -B main FETCH_HEAD
export JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
"$MAVEN" -B -q -f "$SOURCE/backend/pom.xml" -DskipTests package
install -m 644 "$SOURCE/backend/target/syncpass-api-1.0.0.jar" /opt/syncpass/syncpass-api.jar
systemctl restart syncpass-api
for attempt in $(seq 1 60); do
  if curl -fsS --max-time 3 http://127.0.0.1:8080/actuator/health | grep -q '"UP"'; then
    echo "SYNCPASS_COMMIT=$(git -C "$SOURCE" rev-parse --short HEAD)"
    echo 'SYNCPASS_LOCAL_API_UP=true'
    /usr/local/sbin/syncpass-fix-ip
    exit 0
  fi
  sleep 3
done
echo 'La API no inició; consulta journalctl -u syncpass-api -n 100' >&2
exit 1
