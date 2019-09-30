FROM openjdk:13-slim
COPY ./target/cifs-0.0.1-SNAPSHOT.jar /opt/cifs-0.0.1-SNAPSHOT.jar

# RUN echo 'hosts: files mdns4_minimal [NOTFOUND=return] dns mdns4' >> /etc/nsswitch.conf 

ENTRYPOINT ["java"]
CMD ["-XX:MaxDirectMemorySize=256m", "-verbose:gc", "-jar", "/opt/cifs-0.0.1-SNAPSHOT.jar"]
EXPOSE 8080:8080