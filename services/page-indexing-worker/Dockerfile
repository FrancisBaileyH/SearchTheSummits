FROM amazoncorretto
VOLUME /tmp
RUN mkdir /service
COPY build/libs/page-indexing-worker-1.0-SNAPSHOT.jar /service/page-indexing-worker.jar
ENTRYPOINT ["java", "-jar", "/service/page-indexing-worker.jar"]