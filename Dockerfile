FROM ghcr.io/navikt/baseimages/temurin:21
COPY build/libs/app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]