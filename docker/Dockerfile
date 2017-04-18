FROM alpine:latest

RUN apk update && \
    apk add --update bash curl python openjdk8-jre && \
    mkdir -p /opt


COPY web-*.zip /opt

RUN cd /opt && \
    unzip web-*.zip && \
    rm -rf web-*.zip && \
    mv web-* prom-tsdb-exporter

COPY start.sh /opt

WORKDIR /opt
ENTRYPOINT ["./start.sh"] 

