FROM amazoncorretto:8u352
VOLUME /tmp
EXPOSE 8080
EXPOSE 9090
EXPOSE 8849

RUN mkdir /service
RUN mkdir -p /service/var/logs
RUN yum install -y wget
RUN yum install -y tar
RUN yum install -y gzip

RUN wget https://download.ej-technologies.com/jprofiler/jprofiler_agent_linux-x86_13_0.tar.gz -P /tmp/ && \
  tar -xzf /tmp/jprofiler_agent_linux-x86_13_0.tar.gz -C /tmp &&\
  rm /tmp/jprofiler_agent_linux-x86_13_0.tar.gz

COPY build/libs/page-indexing-worker-1.0-SNAPSHOT.jar /service/page-indexing-worker.jar
ENTRYPOINT ["java", "-jar", "/service/page-indexing-worker.jar", "-agentpath:/tmp/jprofiler13/bin/linux-x64/libjprofilerti.so=port=8849"]