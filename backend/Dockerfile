FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -B -q -DskipTests package
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S syncpass && adduser -S -G syncpass syncpass
WORKDIR /app
COPY --from=build /build/target/syncpass-api-1.0.0.jar app.jar
USER syncpass
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=65","-jar","app.jar"]
