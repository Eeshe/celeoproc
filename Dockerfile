# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew gradlew
COPY gradle gradle/
COPY settings.gradle.kts gradle.properties ./
COPY app/build.gradle.kts app/build.gradle.kts
RUN chmod +x gradlew && ./gradlew --no-daemon :app:dependencies
COPY app/src app/src
RUN ./gradlew --no-daemon :app:shadowJar

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --create-home --uid 1001 celeoproc
COPY --from=build /src/app/build/libs/app-all.jar /app/app.jar
COPY app/src/main/resources/settings.json /app/config/settings.json
COPY app/src/main/resources/messages.json /app/config/messages.json
RUN mkdir -p /app/data/graphs && chown -R celeoproc:celeoproc /app
USER celeoproc
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
