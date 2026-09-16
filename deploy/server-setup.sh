#!/usr/bin/env bash
set -euo pipefail

if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
  systemctl enable --now docker
fi

docker compose version
mkdir -p /opt/linkedknowledge /opt/mindmap-ui
echo "docker ready"
