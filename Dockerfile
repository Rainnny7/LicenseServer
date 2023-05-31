FROM maven:3.8.5-openjdk-17-slim AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -T12

FROM openjdk:17.0.1-jdk-slim
RUN java --version
WORKDIR /usr/local/app
COPY --from=builder /app/target/LicenseServer.jar .
EXPOSE 7500
CMD ["java", "-jar", "LicenseServer.jar"]