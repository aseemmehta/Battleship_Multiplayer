#!/usr/bin/env bash
# Prints the URLs to open on a phone that is on the same Wi-Fi as this computer. No sudo needed.
set -u

FRONTEND_PORT=3000
GATEWAY_PORT=9090
if [ -f "$(dirname "$0")/../.env" ]; then
  # shellcheck disable=SC1091
  . "$(dirname "$0")/../.env"
fi

ip=""
if command -v ipconfig >/dev/null 2>&1; then                      # macOS
  for iface in en0 en1 en2; do
    ip=$(ipconfig getifaddr "$iface" 2>/dev/null || true)
    [ -n "$ip" ] && break
  done
fi
if [ -z "$ip" ] && command -v hostname >/dev/null 2>&1; then      # Linux
  ip=$(hostname -I 2>/dev/null | awk '{print $1}')
fi

if [ -z "$ip" ]; then
  echo "Could not detect your LAN IP. Check Wi-Fi settings and use that address instead."
  exit 1
fi

echo "Game (open on the phone):  http://$ip:$FRONTEND_PORT"
echo "API docs / Swagger UI:     http://$ip:$GATEWAY_PORT/swagger-ui.html"
echo "Gateway health:            http://$ip:$GATEWAY_PORT/actuator/health"
