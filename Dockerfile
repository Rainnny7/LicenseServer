FROM maven:3.8.5-openjdk-17-slim AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn package -DskipTests

FROM openjdk:11-ea-17-jre-slim

WORKDIR /app

COPY --from=builder /app/target/LicenseServer.jar .

EXPOSE 8080

CMD ["java", "-jar", "LicenseServer.jar"]