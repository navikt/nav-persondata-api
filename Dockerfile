FROM ghcr.io/navikt/baseimages/temurin:21
COPY build/libs/app.jar app.jar
EXPOSE 5005
ENTRYPOINT ["sh","-lc","exec java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar app.jar"]
