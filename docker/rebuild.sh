#!/usr/bin/env bash
set -eo pipefail

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT=${DIR}/..


clean() {
    echo "Cleaning up"
    rm -f ${DIR}/web-*.zip
}

build() {
    echo "Building artifacts"
    pushd ${PROJECT_ROOT}
    sbt clean web/dist
    popd
}

gather() {
    echo "Gathering artifacts:"

    echo "  - web"
    cp ${PROJECT_ROOT}/web/target/universal/web-*.zip ${DIR}
}

main() {
    clean
    build
    gather
}

main