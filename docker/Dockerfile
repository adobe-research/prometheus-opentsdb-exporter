FROM mcplatform/reloader:1

ENV ENTRYPOINT="/prom-tsdb-exporter/bin/web -Dhttp.port=9000"

RUN apk update && \
    apk add --update bash curl python openjdk8-jre && \
    mkdir -p /etc/prom-tsdb-exporter/conf

COPY web-*.zip /

RUN unzip web-*.zip && \
    rm -rf /web-*.zip && \
    mv /web-* /prom-tsdb-exporter && \
    mkdir -p /prom-tsdb-exporter/metrics

COPY init_program /init_program
COPY application.conf.template /etc/prom-tsdb-exporter/conf
COPY init.sh /init.sh

RUN chmod +x /init_program
RUN chmod +x /init.sh

EXPOSE 9000

ENTRYPOINT ["./init.sh"]

