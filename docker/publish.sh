#!/usr/bin/env bash
set -eo pipefail

: ${DOCKER_REPO:?"not defined"}


DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT=${DIR}/..

VERSION=$(head -n 1 ${PROJECT_ROOT}/VERSION)
GIT_TAG=$(git rev-parse --short HEAD)

DOCKER_IMAGE=${DOCKER_REPO}/prom-tsdb-exporter-${VERSION}:${GIT_TAG}

docker build -t ${DOCKER_IMAGE} .
docker push ${DOCKER_IMAGE}

