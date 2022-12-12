FROM amazoncorretto
VOLUME /tmp
EXPOSE 8080
EXPOSE 9090
RUN mkdir /service
COPY build/libs/front-end-service-1.0-SNAPSHOT.jar /service/front-end-service.jar
ENTRYPOINT ["java", "-jar", "/service/front-end-service.jar"]