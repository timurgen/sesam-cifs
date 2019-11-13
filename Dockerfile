FROM openjdk:13-slim
COPY ./target/cifs-0.0.1-SNAPSHOT.jar /opt/cifs-0.0.1-SNAPSHOT.jar

# RUN echo 'hosts: files mdns4_minimal [NOTFOUND=return] dns mdns4' >> /etc/nsswitch.conf 

ENTRYPOINT ["java"]
CMD ["-XX:MaxDirectMemorySize=1024m", "-XX:MinRAMPercentage=50", "-XX:MaxRAMPercentage=80", "-verbose:gc", "-XshowSettings:vm", "-XX:+UnlockExperimentalVMOptions",  "-XX:+UseZGC", "-Xlog:gc*,stats*=off", "-jar", "/opt/cifs-0.0.1-SNAPSHOT.jar"]
EXPOSE 8080:8080