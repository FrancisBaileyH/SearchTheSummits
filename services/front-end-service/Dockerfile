FROM adoptopenjdk/openjdk11-openj9:jdk-11.0.11_9_openj9-0.26.0

VOLUME /tmp
EXPOSE 8080
EXPOSE 9090

RUN mkdir /service
RUN mkdir -p /service/var/logs

COPY build/libs/front-end-service-1.0-SNAPSHOT.jar /service/front-end-service.jar
ENTRYPOINT ["java", "-jar", "/service/front-end-service.jar", "-XX:MaxRAMPercentage40"]