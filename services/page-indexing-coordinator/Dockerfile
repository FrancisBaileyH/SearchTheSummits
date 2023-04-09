FROM eclipse-temurin:11.0.18_10-jre

VOLUME /tmp
EXPOSE 9090

RUN mkdir /service
RUN mkdir -p /service/var/logs

COPY build/libs/page-indexing-coordinator-1.0-SNAPSHOT.jar /service/page-indexing-coordinator.jar
ENTRYPOINT ["java", "-jar", "/service/page-indexing-coordinator.jar", "-XX:MaxRam=300m"]