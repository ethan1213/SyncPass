#!/bin/bash
# User data para una instancia nueva (Amazon Linux 2023): descarga el repo y ejecuta la instalación con JDK.
# La primera ejecución deja /etc/syncpass/syncpass.env para completar con Supabase; luego se vuelve a correr
# el script: sudo bash /opt/syncpass/source/deploy/ec2-jdk-setup.sh
set -euo pipefail
dnf install -y git
git clone --depth 1 https://github.com/ethan1213/SyncPass.git /opt/syncpass/source || true
bash /opt/syncpass/source/deploy/ec2-jdk-setup.sh main || true

