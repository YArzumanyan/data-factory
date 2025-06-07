# Architecture Overview

This document provides an overview of the Metadata Store application architecture.

## High-Level Architecture

The Metadata Store is built using the following technologies and architectural patterns:

- **Spring Boot**: Provides the foundation for the application, including dependency injection, web server, and configuration management.
- **Apache Jena**: Provides the RDF processing capabilities and TDB2 triple store.
- **REST API**: Exposes the functionality through a RESTful interface.
- **MVC Pattern**: Separates the application into Model (RDF data), View (API responses), and Controller (request handling) components.

## Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Applications                    │
└───────────────────────────────┬─────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                         REST API Layer                      │
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │    Store    │  │  Resource   │  │ Dataset/Pipeline/   │  │
│  │ Controller  │  │ Controller  │  │ Plugin Controllers  │  │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘  │
└─────────┼────────────────┼────────────────────┼─────────────┘
          │                │                    │
          ▼                ▼                    ▼
┌─────────────────────────────────────────────────────────────┐
│                       Service Layer                         │
│                                                             │
│               ┌─────────────────────────────┐               │
│               │      RdfStorageService      │               │
│               └──────────────┬──────────────┘               │
│                              │                              │
│               ┌──────────────┴──────────────┐               │
│               │         UriService          │               │
│               └─────────────────────────────┘               │
└─────────────────────────────┬───────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       Data Access Layer                     │
│                                                             │
│               ┌─────────────────────────────┐               │
│               │      Apache Jena TDB2       │               │
│               └─────────────────────────────┘               │
└─────────────────────────────────────────────────────────────┘
```

## Key Components

### Controllers

The controllers handle HTTP requests and responses. They are responsible for:

1. **Request Validation**: Ensuring that incoming requests are valid.
2. **Content Negotiation**: Supporting different RDF serialization formats.
3. **Response Formatting**: Converting RDF models to the requested format.

Key controller classes:
- `RdfController`: Interface defining common RDF handling methods
- `StoreController`: Operations on the entire RDF store
- `ResourceController`: Generic resource operations
- `DatasetController`: Dataset-specific operations
- `PipelineController`: Pipeline-specific operations
- `PluginController`: Plugin-specific operations

### Services

The services implement the business logic of the application. They are responsible for:

1. **RDF Data Management**: Storing and retrieving RDF data.
2. **Resource Identification**: Managing URIs for resources.
3. **Data Validation**: Ensuring data integrity.

Key service classes:
- `RdfStorageService`: Interface defining RDF storage operations
- `RdfStorageServiceImpl`: Implementation of the RDF storage service
- `UriService`: Service for managing URIs

### Configuration

The configuration components set up the application and its dependencies. They are responsible for:

1. **Jena Configuration**: Setting up the TDB2 dataset.
2. **Vocabulary Loading**: Loading initial RDF vocabulary data.

Key configuration classes:
- `JenaConfig`: Configures the Apache Jena TDB2 dataset
- `VocabularyLoader`: Loads initial vocabulary data

### Exception Handling

The exception handling components provide consistent error responses. They are responsible for:

1. **Error Translation**: Converting exceptions to HTTP responses.
2. **Error Formatting**: Providing consistent error messages.

Key exception handling classes:
- `ResourceNotFoundException`: Exception for resources not found
- `RestExceptionHandler`: Global exception handler

### Utilities

The utility components provide common functionality used throughout the application. They are responsible for:

1. **Media Type Handling**: Managing RDF media types.
2. **Vocabulary Constants**: Providing RDF vocabulary constants.

Key utility classes:
- `RdfMediaType`: Constants and utilities for RDF media types
- `Vocab`: Constants for RDF vocabulary terms

## Data Flow

1. **Client Request**: A client sends an HTTP request to one of the API endpoints.
2. **Controller Processing**: The appropriate controller validates the request and extracts parameters.
3. **Service Invocation**: The controller calls the appropriate service method.
4. **Data Access**: The service interacts with the Jena TDB2 dataset to store or retrieve data.
5. **Response Creation**: The service returns data to the controller, which formats it as an HTTP response.
6. **Client Response**: The formatted response is sent back to the client.

## Design Patterns

The Metadata Store application uses several design patterns:

1. **Dependency Injection**: Spring's DI container manages component dependencies.
2. **Strategy Pattern**: The RDF serialization format handling uses a strategy pattern.
3. **Repository Pattern**: The RDF storage service acts as a repository for RDF data.
4. **Factory Pattern**: Used for creating RDF models and resources.
5. **Adapter Pattern**: Used to adapt between Jena's API and the application's API.