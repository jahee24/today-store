FROM gradle:8.14.3-jdk21 AS build
WORKDIR /workspace

COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY gradle gradle
COPY settings.gradle build.gradle ./
COPY src src

RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre-noble
WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
