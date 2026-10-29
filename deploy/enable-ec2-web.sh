#!/bin/bash
set -euo pipefail

TOKEN=$(curl -fsS -X PUT -H 'X-aws-ec2-metadata-token-ttl-seconds: 21600' http://169.254.169.254/latest/api/token)
PUBLIC_IP=$(curl -fsS -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/public-ipv4)
DASHED_IP=${PUBLIC_IP//./-}
WEB_DOMAIN="syncpass.$DASHED_IP.nip.io"
API_DOMAIN="api.$DASHED_IP.nip.io"

docker image inspect syncpass-web:local >/dev/null
if ! docker container inspect syncpass-web >/dev/null 2>&1; then
  docker run -d --name syncpass-web --network syncpass --network-alias web --restart unless-stopped --memory 128m syncpass-web:local
fi

sed -i "s|^CORS_ORIGINS=.*|CORS_ORIGINS=http://localhost:3000,https://$WEB_DOMAIN|" /opt/syncpass/runtime.env
docker rm -f syncpass-api
docker run -d --name syncpass-api --network syncpass --network-alias api --restart unless-stopped --memory 700m --env-file /opt/syncpass/runtime.env syncpass-api:local

cat > /opt/syncpass/Caddyfile <<EOF
$API_DOMAIN {
  encode gzip
  @preflight {
    method OPTIONS
    path /api/*
  }
  @api path /api/* /actuator/health
  route {
    respond @preflight "" 204
    handle @api {
      reverse_proxy api:8080
    }
    handle {
      respond "Not found" 404
    }
  }
  header X-Content-Type-Options nosniff
}
$WEB_DOMAIN {
  encode gzip
  reverse_proxy web:80
  header X-Content-Type-Options nosniff
}
EOF

docker exec syncpass-caddy caddy validate --config /etc/caddy/Caddyfile
docker restart syncpass-caddy
echo "SYNC_PASS_WEB=https://$WEB_DOMAIN/"
echo "SYNC_PASS_API_GATEWAY=https://hluamat8vg.execute-api.us-east-1.amazonaws.com"
