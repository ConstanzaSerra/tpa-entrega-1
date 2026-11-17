#!/bin/sh
# Disparador de la tarea programada del servicio de logistica.
# Lo arranca Railway a la hora del "Cron Schedule"; corre y el contenedor se apaga.

set -u

if [ -z "${LOGISTICA_URL:-}" ] || [ -z "${CRON_API_KEY:-}" ]; then
  echo "Faltan LOGISTICA_URL o CRON_API_KEY" >&2
  exit 1
fi

echo "[cron] planificacion de rutas"
curl -fsS -X POST "$LOGISTICA_URL/api/internal/cron/planificacion" \
     -H "X-API-KEY: $CRON_API_KEY"
