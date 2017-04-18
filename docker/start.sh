#!/bin/sh
set -eo pipefail

: ${OPEN_TSDB_URL:?"not defined"}
: ${METRICS_DIR:="/opt/conf"}
: ${PORT_WEB:=9000}


start_web() {
    export OPEN_TSDB_URL=${OPEN_TSDB_URL}
    export PORT_WEB=${PORT_WEB}
    export METRICS_DIR=${METRICS_DIR}

    echo "Starting the exporter web-app"
    cd /opt/prom-tsdb-exporter

    ./bin/web -Dhttp.port=${PORT_WEB}
}

env

start_web
