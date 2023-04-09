FROM eclipse-temurin:11.0.18_10-jre

VOLUME /tmp
EXPOSE 8080
EXPOSE 9090

RUN mkdir /service
RUN mkdir -p /service/var/logs

COPY build/libs/front-end-service-1.0-SNAPSHOT.jar /service/front-end-service.jar
ENTRYPOINT ["java", "-jar", "/service/front-end-service.jar", "-XX:MaxRAMPercentage40"]