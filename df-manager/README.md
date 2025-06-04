# Data Factory Manager (df-manager)

A Spring Boot-based middleware application that acts as a central communication hub between external applications and a backend data management ecosystem.

## Overview

The **df-manager** simplifies interactions for external applications by providing a unified REST API. It abstracts the complexities of direct communication with the `metadata-store` (RDF graph manipulation) and the `artifact-repository` (file uploads/management).

## Key Functionalities

- **Artifact Upload (Datasets & Plugins):** Receives artifact files and their basic metadata, uploads them to the artifact repository, and generates RDF metadata for the metadata store.
- **Pipeline Configuration:** Accepts JSON configurations for data processing pipelines, validates them, and translates them into RDF graphs.
- **Metadata Retrieval:** Provides endpoints for retrieving RDF metadata for existing datasets, plugins, and pipelines.

## Technology Stack

- Spring Boot 3.4.5
- Java 21
- Apache Jena (for RDF manipulation)
- Maven (for build and dependency management)
- RESTful APIs (for communication)

## API Endpoints

### Datasets

- **POST /api/v1/datasets**: Upload a dataset file and create metadata for it.
  - Request: `multipart/form-data` with `file`, `title`, and optional `description`.
  - Response: RDF data for the created dataset.

- **GET /api/v1/datasets/{uuid}**: Retrieve metadata for a dataset.
  - Response: RDF data for the dataset.

### Plugins

- **POST /api/v1/plugins**: Upload a plugin file and create metadata for it.
  - Request: `multipart/form-data` with `file`, `title`, and optional `description`.
  - Response: RDF data for the created plugin.

- **GET /api/v1/plugins/{uuid}**: Retrieve metadata for a plugin.
  - Response: RDF data for the plugin.

### Pipelines

- **POST /api/v1/pipelines**: Create a new pipeline from a configuration.
  - Request: JSON payload conforming to the pipeline configuration schema.
  - Response: RDF data for the created pipeline.

- **GET /api/v1/pipelines/{uuid}**: Retrieve metadata for a pipeline.
  - Response: RDF data for the pipeline.

## Configuration

The application is configured through `application.properties` and can be customized using environment variables:

- **Server Configuration**: Port, etc.
- **Metadata Store Configuration**: Base URL and endpoints for the metadata store.
- **Artifact Repository Configuration**: Base URL and endpoints for the artifact repository.
- **RDF Configuration**: Namespace definitions for RDF generation.

### Environment Variables

You can override the default configuration by setting environment variables. A sample `.env.sample` file is provided as a template:

1. Copy `.env.sample` to `.env` (this file is not tracked by git)
2. Modify the values in `.env` according to your environment
3. Use your preferred method to load these environment variables before starting the application

Available environment variables:

| Environment Variable | Description | Default Value |
|---------------------|-------------|---------------|
| SERVER_PORT | Application server port | 8080 |
| METADATA_STORE_BASE_URL | Base URL for the metadata store API | http://metadata-store-host/api/v1 |
| ARTIFACT_REPOSITORY_BASE_URL | Base URL for the artifact repository API | http://artifact-repo/api/v1 |
| RDF_NAMESPACE_DF | Namespace for Data Factory RDF resources | http://localhost:8080/ns/df# |
| RDF_NAMESPACE_PIPE | Namespace for pipeline resources | urn:pipe: |
| RDF_NAMESPACE_STEP | Namespace for pipeline step resources | urn:step: |
| RDF_NAMESPACE_VAR | Namespace for variable resources | urn:var: |
| RDF_NAMESPACE_DS | Namespace for dataset resources | urn:ds: |
| RDF_NAMESPACE_PL | Namespace for plugin resources | urn:pl: |

## Building and Running

### Prerequisites

- Java 21 or higher
- Maven 3.6 or higher

### Build

```bash
mvn clean package
```

### Run

```bash
java -jar target/df-manager-0.0.1-SNAPSHOT.jar
```

With environment variables:

```bash
SERVER_PORT=9090 METADATA_STORE_BASE_URL=http://custom-metadata-store/api/v1 java -jar target/df-manager-0.0.1-SNAPSHOT.jar
```

Or using Maven:

```bash
mvn spring-boot:run
```

With environment variables using Maven:

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-DSERVER_PORT=9090 -DMETADATA_STORE_BASE_URL=http://custom-metadata-store/api/v1"
```

## Example Usage

See the [workflow.md](workflow.md) file for detailed examples of how to use the API.
