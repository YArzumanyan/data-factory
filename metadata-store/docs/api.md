# API Documentation for Metadata Store

This document provides comprehensive documentation for the Metadata Store API endpoints.

## Base URL

All API endpoints are prefixed with `/api/v1`.

## Authentication

Currently, the API does not require authentication.

## Media Types

The API supports the following RDF media types:
- `text/turtle` - Turtle format
- `application/ld+json` - JSON-LD format
- `application/rdf+xml` - RDF/XML format

## Endpoints

### Datasets

Operations related to Dataset descriptions (dcat:Dataset).

#### Create a Dataset

```
POST /api/v1/datasets
```

**Description**: Receives and persists a pre-validated RDF graph for a dataset (dcat:Dataset). Called by Middleware.

**Request Headers**:
- `Content-Type`: One of the supported RDF media types

**Request Body**: RDF graph containing a dcat:Dataset resource

**Response**:
- `201 Created`: Dataset RDF stored successfully
  - `Location`: URI of the created dataset resource
- `400 Bad Request`: Malformed RDF syntax or missing dcat:Dataset resource
- `415 Unsupported Media Type`: Unsupported RDF Content-Type

#### Get Dataset by ID

```
GET /api/v1/datasets/{datasetId}
```

**Description**: Retrieves the RDF description of a specific dataset.

**Parameters**:
- `datasetId`: UUID of the dataset (required)

**Request Headers**:
- `Accept`: One of the supported RDF media types

**Response**:
- `200 OK`: Dataset definition in requested RDF format
- `404 Not Found`: Dataset not found
- `406 Not Acceptable`: Unsupported Accept header format

#### List All Datasets

```
GET /api/v1/datasets
```

**Description**: Retrieves an RDF graph containing descriptions of all registered datasets (dcat:Dataset).

**Request Headers**:
- `Accept`: One of the supported RDF media types

**Response**:
- `200 OK`: RDF graph containing all dcat:Dataset resources
- `406 Not Acceptable`: Unsupported Accept header format

### Pipelines

Operations related to Pipeline definitions (p-plan:Plan).

#### Create a Pipeline

```
POST /api/v1/pipelines
```

**Description**: Receives and persists a complete RDF graph for a pipeline plan (p-plan:Plan). Called by Middleware.

**Request Headers**:
- `Content-Type`: One of the supported RDF media types

**Request Body**: RDF graph containing a p-plan:Plan resource

**Response**:
- `201 Created`: Pipeline RDF stored successfully
  - `Location`: URI of the created pipeline plan resource
- `400 Bad Request`: Malformed RDF syntax or missing p-plan:Plan resource
- `415 Unsupported Media Type`: Unsupported RDF Content-Type

#### Get Pipeline by ID

```
GET /api/v1/pipelines/{planId}
```

**Description**: Retrieves the RDF description of a specific pipeline plan.

**Parameters**:
- `planId`: UUID of the pipeline plan (required)

**Request Headers**:
- `Accept`: One of the supported RDF media types

**Response**:
- `200 OK`: Pipeline definition in requested RDF format
- `404 Not Found`: Pipeline plan not found
- `406 Not Acceptable`: Unsupported Accept header format

#### List All Pipelines

```
GET /api/v1/pipelines
```

**Description**: Retrieves a list of all pipeline definitions in the specified RDF format.

**Request Headers**:
- `Accept`: One of the supported RDF media types

**Response**:
- `200 OK`: List of pipelines in requested RDF format
- `406 Not Acceptable`: Unsupported Accept header format

### Plugins

Operations related to Plugin descriptions (df:Plugin).

#### Create a Plugin

```
POST /api/v1/plugins
```

**Description**: Receives and persists a pre-validated RDF graph for a plugin (df:Plugin). Called by Middleware.

**Request Headers**:
- `Content-Type`: One of the supported RDF media types

**Request Body**: RDF graph containing a df:Plugin resource

**Response**:
- `201 Created`: Plugin RDF stored successfully
  - `Location`: URI of the created plugin resource
- `400 Bad Request`: Malformed RDF syntax or missing df:Plugin resource
- `415 Unsupported Media Type`: Unsupported RDF Content-Type

#### Get Plugin by ID

```
GET /api/v1/plugins/{pluginId}
```

**Description**: Retrieves the RDF description of a specific plugin.

**Parameters**:
- `pluginId`: UUID of the plugin (required)

**Request Headers**:
- `Accept`: One of the supported RDF media types

**Response**:
- `200 OK`: Plugin definition in requested RDF format
- `404 Not Found`: Plugin not found
- `406 Not Acceptable`: Unsupported Accept header format

#### List All Plugins

```
GET /api/v1/plugins
```

**Description**: Retrieves an RDF graph containing descriptions of all registered plugins (df:Plugin).

**Request Headers**:
- `Accept`: One of the supported RDF media types

**Response**:
- `200 OK`: RDF graph containing all df:Plugin resources
- `406 Not Acceptable`: Unsupported Accept header format

### Generic Resources

Operations for retrieving any resource by its UUID.

#### Get Resource by ID

```
GET /api/v1/resources/{resourceId}
```

**Description**: Retrieves the RDF description of any resource by its UUID.

**Parameters**:
- `resourceId`: UUID of the resource to retrieve (required)

**Request Headers**:
- `Accept`: One of the supported RDF media types

**Response**:
- `200 OK`: RDF description of the resource
- `404 Not Found`: Resource not found
- `406 Not Acceptable`: Unsupported Accept header format

### Store Operations

Operations related to the entire RDF store.

#### Dump Store

```
GET /api/v1/store/dump
```

**Description**: Retrieves all triples residing in the default graph of the RDF store in the requested format. By default, prompts a download. Use the 'inline=true' query parameter to display directly in the browser.

**Parameters**:
- `inline`: Set to 'true' to display content inline instead of triggering a download (optional, default: false)

**Request Headers**:
- `Accept`: One of the supported RDF media types

**Response**:
- `200 OK`: RDF dump successful
- `406 Not Acceptable`: Unsupported Accept header format
- `500 Internal Server Error`: Internal server error during serialization

## Examples

### Creating a Dataset

**Request**:
```
POST /api/v1/datasets
Content-Type: text/turtle

@prefix dcat: <http://www.w3.org/ns/dcat#> .
@prefix dcterms: <http://purl.org/dc/terms/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

<http://example.org/datasets/1> a dcat:Dataset ;
    dcterms:title "Example Dataset" ;
    dcterms:description "This is an example dataset" ;
    dcterms:issued "2023-01-01"^^xsd:date ;
    dcterms:modified "2023-01-02"^^xsd:date .
```

**Response**:
```
HTTP/1.1 201 Created
Location: http://localhost:8080/rdfstore/datasets/1
```

### Retrieving a Dataset

**Request**:
```
GET /api/v1/datasets/1
Accept: text/turtle
```

**Response**:
```
HTTP/1.1 200 OK
Content-Type: text/turtle

@prefix dcat: <http://www.w3.org/ns/dcat#> .
@prefix dcterms: <http://purl.org/dc/terms/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

<http://localhost:8080/rdfstore/datasets/1> a dcat:Dataset ;
    dcterms:title "Example Dataset" ;
    dcterms:description "This is an example dataset" ;
    dcterms:issued "2023-01-01"^^xsd:date ;
    dcterms:modified "2023-01-02"^^xsd:date .
```

## API Documentation

The Swagger UI for this API is available at `/swagger-ui`. The OpenAPI specification is available at `/api-docs`.