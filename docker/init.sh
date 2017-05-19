#!/bin/bash

export ENTRYPOINT="/prom-tsdb-exporter/bin/web -Dhttp.port=9000"
[[ -z "${CONFIG_SUPERVISOR_ENABLED}" ]] && touch /etc/services.d/config-supervisor/down
[[ -f /init_program ]] && /init_program

exec /init