# Metadata Store

A Spring Boot service for storing and retrieving RDF metadata about datasets, plugins and pipelines using Apache Jena TDB2.  
This service is part of the [Data Factory](../README.md) microservice suite.

## Prerequisites
- Java 21 or higher
- Maven (for local development)
- Docker and Docker Compose (for containerised deployment)

## Environment variables
The following variables can be used to customise the service:

| Variable | Default | Description |
|----------|---------|-------------|
| `JENA_TDB2_LOCATION` | `./data/tdb2_metadata_store` | Directory for the TDB2 dataset |
| `METADATA_STORE_BASE_URI` | `http://localhost:8080/api/v1` | Base URI used in generated RDF |
| `SERVER_PORT` | `8080` | Port on which the application runs |

## Running with Docker Compose
1. Optionally create a `.env` file to override the variables above.
2. Start the service:

```bash
docker-compose up --build
```

The API will be available on port `8081` using the provided compose file.
