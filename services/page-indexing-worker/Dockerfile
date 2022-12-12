FROM amazoncorretto
VOLUME /tmp
EXPOSE 8080
EXPOSE 9090
RUN mkdir /service
COPY build/libs/page-indexing-worker-1.0-SNAPSHOT.jar /service/page-indexing-worker.jar
ENTRYPOINT ["java", "-jar", "/service/page-indexing-worker.jar"]