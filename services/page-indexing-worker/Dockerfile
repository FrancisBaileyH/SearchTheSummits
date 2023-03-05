FROM adoptopenjdk/openjdk11-openj9:jdk-11.0.11_9_openj9-0.26.0

VOLUME /tmp
EXPOSE 8080
EXPOSE 9090

RUN mkdir /service
RUN mkdir -p /service/var/logs

COPY build/libs/page-indexing-worker-1.0-SNAPSHOT.jar /service/page-indexing-worker.jar
ENTRYPOINT java $JAVA_OPTS -jar /service/page-indexing-worker.jar