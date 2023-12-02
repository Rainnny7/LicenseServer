# Stage 1: Building
FROM maven:3.8.5-openjdk-17-slim AS builder

# Set the work dir inside the container
WORKDIR /app

# Copy the POM file to the work dir
COPY pom.xml .

# Copy src to the work dir
COPY src ./src

# Run Maven to clean and package the app
RUN mvn clean package -T12

# Stage 2: Running
FROM openjdk:17.0.2-jdk-slim

# Set the work dir inside the container
WORKDIR /usr/local/app

# Copy the compiled JAR file from the builder stage to the work dir
COPY --from=builder /app/target/LicenseServer.jar .

# Expose the port
EXPOSE 7500

# Set the command to run the app when the container starts
CMD ["java", "-jar", "LicenseServer.jar"]