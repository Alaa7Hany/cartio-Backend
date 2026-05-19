# Stage 1: Build the application using Gradle
FROM gradle:8-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle buildFatJar --no-daemon

# Stage 2: Run the application
FROM openjdk:17-jdk-slim
EXPOSE 8080
RUN mkdir /app
# Copies the generated fat JAR from the build stage
COPY --from=build /home/gradle/src/build/libs/*-all.jar /app/cartio-backend.jar
ENTRYPOINT ["java","-jar","/app/cartio-backend.jar"]