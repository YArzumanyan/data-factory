# Use an official OpenJDK runtime as a parent image
FROM openjdk:21-jdk-slim

# Create a directory for the app
WORKDIR /app

# Copy the jar file from target
ARG JAR_FILE=target/metadata-store-1.0.0.jar
COPY ${JAR_FILE} app.jar

# Expose the application port
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
