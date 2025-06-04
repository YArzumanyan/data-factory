# Artifact Repository

A Spring Boot application that provides a REST API for storing, retrieving, and managing binary artifacts using MinIO as the underlying object storage system.

## Overview

The Artifact Repository service allows you to:

- Upload binary files and get a unique ID for later retrieval
- Download artifacts by their ID
- Get pre-signed URLs for temporary access to artifacts
- Delete artifacts when they are no longer needed

The service uses MinIO, a high-performance, S3-compatible object storage system, to store the binary artifacts.

## Prerequisites

- Java 21 or higher
- Docker and Docker Compose (for containerized deployment)
- Maven (for local development)

## Setup and Deployment

### Environment Configuration

Create a `.env` file based on the provided `.env.template`:

```
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=artifact-repository

MINIO_API_PORT=9000
MINIO_CONSOLE_PORT=9001
APP_PORT=8080
```

Adjust the values as needed for your environment.

### Docker Deployment

To deploy the application with Docker Compose:

1. Ensure Docker and Docker Compose are installed
2. Create the `.env` file as described above
3. Run the following command:

```bash
docker-compose up -d
```

This will start both the MinIO server and the Artifact Repository application.

### Local Development

To run the application locally:

1. Ensure you have Java 21 and Maven installed
2. Start a MinIO server (or use an existing one)
3. Configure the application properties in `src/main/resources/application.properties` or use environment variables
4. Run the application:

```bash
mvn spring-boot:run
```

## API Documentation

### Upload an Artifact

```
POST /objects
```

**Request:**
- Content-Type: multipart/form-data
- Body: file (binary data)

**Response:**
- Status: 201 Created
- Body: String (the generated object ID)

### Download an Artifact

```
GET /objects/{objectId}
```

**Response:**
- Status: 200 OK
- Body: Binary data (the artifact)
- Headers: Content-Type, Content-Length, Content-Disposition

### Get a Pre-signed URL

```
GET /objects/{objectId}/url
```

**Response:**
- Status: 200 OK
- Body: String (the pre-signed URL, valid for 1 hour)

### Delete an Artifact

```
DELETE /objects/{objectId}
```

**Response:**
- Status: 204 No Content

## Configuration Options

The application can be configured using environment variables or by modifying the `application.properties` file:

| Property | Environment Variable | Default Value | Description |
|----------|----------------------|---------------|-------------|
| minio.endpoint | MINIO_ENDPOINT | http://localhost:9000 | MinIO server URL |
| minio.accessKey | MINIO_ACCESS_KEY | minioadmin | MinIO access key |
| minio.secretKey | MINIO_SECRET_KEY | minioadmin | MinIO secret key |
| minio.bucketName | MINIO_BUCKET | artifact-repository | MinIO bucket name |
| server.port | APP_PORT | 8080 | Application port |

## Project Structure

- `src/main/java/cz/cuni/mff/artifactrepo/controller/ObjectStorageController.java`: REST API endpoints
- `src/main/java/cz/cuni/mff/artifactrepo/service/ObjectStorageService.java`: Service layer for MinIO operations
- `src/main/java/cz/cuni/mff/artifactrepo/config/MinioConfig.java`: MinIO client configuration
- `src/main/resources/application.properties`: Application configuration
- `compose.yaml`: Docker Compose configuration
- `Dockerfile`: Docker image definition

## Logging

The application uses SLF4J for logging with the following log levels:

- `cz.cuni.mff.artifactrepo`: DEBUG
- `io.minio`: INFO

## License

This project is part of the Data Factory at Charles University, Faculty of Mathematics and Physics.