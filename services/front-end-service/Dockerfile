FROM amazoncorretto
VOLUME /tmp
EXPOSE 8080
RUN mkdir /service
COPY build/libs/front-end-service-1.0-SNAPSHOT.jar /service/front-end-service.jar
ENTRYPOINT ["java", "-jar", "/service/front-end-service.jar"]