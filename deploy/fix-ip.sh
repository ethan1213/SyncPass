#!/bin/bash
# Reapunta Caddy a la IP publica actual de la EC2.
#
# Cada stop/start del laboratorio cambia la IPv4 publica, pero /etc/caddy/Caddyfile
# conserva el dominio nip.io de la IP anterior. Caddy queda sin certificado para el
# nombre nuevo, el handshake TLS falla y API Gateway responde 503.
#
# Este script recalcula el dominio, reinicia Caddy y actualiza API Gateway.
# No recompila el JAR. Se ejecuta tambien al arrancar EC2 mediante systemd.
#   sudo bash fix-ip.sh
set -euo pipefail
[ "$(id -u)" -eq 0 ] || { echo "Ejecutalo con sudo: sudo bash fix-ip.sh" >&2; exit 1; }
CADDYFILE=/etc/caddy/Caddyfile
API_ID=${SYNCPASS_API_ID:-hluamat8vg}
INTEGRATION_ID=${SYNCPASS_INTEGRATION_ID:-bks3ud2}
AWS_REGION=${AWS_REGION:-us-east-1}
[ -f "$CADDYFILE" ] || { echo "No existe $CADDYFILE. Corre antes ec2-jdk-setup.sh" >&2; exit 1; }

TOKEN=$(curl -fsS -X PUT -H 'X-aws-ec2-metadata-token-ttl-seconds: 300' http://169.254.169.254/latest/api/token)
PUBLIC_IP=$(curl -fsS -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/public-ipv4)
[[ "$PUBLIC_IP" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]] || { echo "IPv4 publica invalida" >&2; exit 1; }
API_DOMAIN="api.${PUBLIC_IP//./-}.nip.io"
echo "SYNCPASS_PUBLIC_IP=$PUBLIC_IP"

ANTERIOR=$(head -1 "$CADDYFILE" | awk '{print $1}')
if [ "$ANTERIOR" = "$API_DOMAIN" ]; then
  echo "El Caddyfile ya apunta a $API_DOMAIN; solo reinicio Caddy."
else
  cp "$CADDYFILE" "$CADDYFILE.bak"
  sed -i "1s|.*|$API_DOMAIN {|" "$CADDYFILE"
  echo "Dominio actualizado: $ANTERIOR -> $API_DOMAIN  (respaldo en $CADDYFILE.bak)"
fi

/usr/local/bin/caddy validate --config "$CADDYFILE" --adapter caddyfile
systemctl restart caddy
systemctl is-active --quiet syncpass-api || systemctl restart syncpass-api

echo "Esperando el certificado de Let's Encrypt..."
for i in $(seq 1 30); do
  if curl -fsS --max-time 5 "https://$API_DOMAIN/actuator/health" >/dev/null 2>&1; then
    echo "OK: https://$API_DOMAIN/actuator/health responde."
    echo
    echo "Actualizando API Gateway..."
    aws apigatewayv2 update-integration --api-id "$API_ID" \
      --integration-id "$INTEGRATION_ID" --integration-uri "https://$API_DOMAIN" \
      --region "$AWS_REGION" --query IntegrationUri --output text
    for attempt in $(seq 1 15); do
      if curl -fsS --max-time 5 "https://$API_ID.execute-api.$AWS_REGION.amazonaws.com/actuator/health" | grep -q '"UP"'; then
        echo "SYNCPASS_GATEWAY_UP=https://$API_ID.execute-api.$AWS_REGION.amazonaws.com"
        exit 0
      fi
      sleep 2
    done
    echo "La API local responde, pero API Gateway aun no confirma UP" >&2
    exit 1
  fi
  sleep 2
done

echo "El certificado aun no esta listo o la API no responde. Revisa:" >&2
echo "    journalctl -u caddy -n 50" >&2
echo "    systemctl status syncpass-api" >&2
exit 1
