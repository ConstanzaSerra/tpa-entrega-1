#!/bin/sh
# Disparador de las tareas programadas del servicio de donaciones.
#
# Este script es el equivalente a la linea que iria en un crontab de Linux.
# La diferencia es que no mide el tiempo: lo arranca Railway a la hora
# configurada en "Cron Schedule", corre, y el contenedor se apaga.
#
# -f  hace que un 404 o un 500 devuelvan error en vez de imprimir el cuerpo y
#     salir con exito. Sin esto, si el endpoint no existiera el cron figuraria
#     como exitoso todos los dias sin ejecutar nada.
# -S  muestra el error cuando falla; -s calla la barra de progreso.

set -u

if [ -z "${DONACIONES_URL:-}" ] || [ -z "${CRON_API_KEY:-}" ]; then
  echo "Faltan DONACIONES_URL o CRON_API_KEY" >&2
  exit 1
fi

estado=0

echo "[cron] matchmaking nocturno"
curl -fsS -X POST "$DONACIONES_URL/api/internal/cron/matchmaking" \
     -H "X-API-KEY: $CRON_API_KEY" || estado=1

# Separadas a proposito: si el matchmaking falla, igual queremos intentar la
# deteccion de inactividad. Con && la segunda nunca se ejecutaria.
echo "[cron] deteccion de inactividad"
curl -fsS -X POST "$DONACIONES_URL/api/internal/cron/inactividad" \
     -H "X-API-KEY: $CRON_API_KEY" || estado=1

exit $estado
