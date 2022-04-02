FROM adoptopenjdk/openjdk11:alpine-jre

RUN mkdir /code
WORKDIR /code

ARG JAR_FILE=server.jar

COPY ${JAR_FILE} server.jar

ENTRYPOINT ["java", "-jar", "server.jar"]