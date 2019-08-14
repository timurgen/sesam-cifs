FROM openjdk:8-jre-alpine
COPY ./target/cifs-0.0.1-SNAPSHOT.jar /opt/cifs-0.0.1-SNAPSHOT.jar

# RUN echo 'hosts: files mdns4_minimal [NOTFOUND=return] dns mdns4' >> /etc/nsswitch.conf 

ENTRYPOINT ["/usr/bin/java"]
CMD ["-jar", "/opt/cifs-0.0.1-SNAPSHOT.jar"]
EXPOSE 8080:8080