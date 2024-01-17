FROM gradle:7.4.0-jdk17 AS build
WORKDIR /app
COPY . /app
RUN gradle clean build

FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]

