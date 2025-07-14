# Extending the Metadata Store

This guide explains how to extend the Metadata Store application with new features.

## Overview

The Metadata Store is designed to be extensible. You can add new features by:

1. Adding new resource types
2. Adding new API endpoints
3. Extending the RDF storage service
4. Implementing additional serialization formats

## Adding a New Resource Type

To add a new resource type (e.g., a "Report" type alongside Datasets, Pipelines, and Plugins):

### 1. Define the Vocabulary

Add the new resource type to the `Vocab.java` class:

```java
public class Vocab {
    // Existing code...
    
    // New resource type
    public static final Resource Report = ResourceFactory.createResource(NS + "Report");
    
    // Properties for the new resource type
    public static final Property hasReportDate = ResourceFactory.createProperty(NS + "hasReportDate");
    public static final Property hasReportStatus = ResourceFactory.createProperty(NS + "hasReportStatus");
}
```

### 2. Create a Controller

Create a new controller class for the resource type:

```java
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Operations for managing report resources")
public class ReportController implements RdfController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);
    
    private final RdfStorageService rdfStorageService;
    
    private static final String[] SUPPORTED_RDF_MEDIA_TYPES = {
        RdfMediaType.TEXT_TURTLE_VALUE,
        RdfMediaType.APPLICATION_LD_JSON_VALUE,
        RdfMediaType.APPLICATION_RDF_XML_VALUE
    };
    
    @Override
    public String[] getSupportedRdfMediaTypes() {
        return SUPPORTED_RDF_MEDIA_TYPES;
    }
    
    @Autowired
    public ReportController(RdfStorageService rdfStorageService) {
        this.rdfStorageService = rdfStorageService;
    }
    
    // Implement endpoints for listing, retrieving, and creating reports
    // Similar to DatasetController, PipelineController, etc.
}
```

### 3. Extend the Service

Add methods to the `RdfStorageService` interface and its implementation:

```java
// In RdfStorageService.java
Model getReportDescription(String reportUuid) throws ResourceNotFoundException;

// In RdfStorageServiceImpl.java
@Override
public Model getReportDescription(String reportUuid) throws ResourceNotFoundException {
    Optional<Model> reportModelOpt = getGenericResourceDescription(reportUuid);
    
    if (reportModelOpt.isEmpty()) {
        throw new ResourceNotFoundException("Report with UUID " + reportUuid + " not found.");
    }
    
    Model reportModel = reportModelOpt.get();
    
    // Verify it's a report
    Resource reportResource = getResourceByUuid(reportModel, reportUuid);
    if (!reportModel.contains(reportResource, RDF.type, Vocab.Report)) {
        throw new ResourceNotFoundException("Resource with UUID " + reportUuid + " is not a Report.");
    }
    
    return reportModel;
}
```

## Adding New API Endpoints

To add a new API endpoint to an existing controller:

### 1. Define the Endpoint Method

Add a new method to the controller class:

```java
@GetMapping(value = "/search", produces = {RdfMediaType.TEXT_TURTLE_VALUE, RdfMediaType.APPLICATION_LD_JSON_VALUE, RdfMediaType.APPLICATION_RDF_XML_VALUE})
@Operation(summary = "Search for datasets by keyword",
        parameters = @Parameter(name = "keyword", description = "Keyword to search for", required = true),
        responses = {
                @ApiResponse(responseCode = "200", description = "Search results"),
                @ApiResponse(responseCode = "406", description = "Unsupported Accept header format", content = @Content)
        })
public ResponseEntity<String> searchDatasets(
        @RequestParam String keyword,
        @RequestHeader(HttpHeaders.ACCEPT) String acceptHeader) {
    
    // Implement search functionality
    Model resultsModel = rdfStorageService.searchDatasetsByKeyword(keyword);
    
    return formatRdfResponse(resultsModel, acceptHeader);
}
```

### 2. Implement the Service Method

Add a corresponding method to the service:

```java
// In RdfStorageService.java
Model searchDatasetsByKeyword(String keyword);

// In RdfStorageServiceImpl.java
@Override
public Model searchDatasetsByKeyword(String keyword) {
    Model resultModel = ModelFactory.createDefaultModel();
    
    // Execute a SPARQL query to find datasets with the keyword
    String queryString = "...";
    
    dataset.executeRead(() -> {
        try (QueryExecution qexec = QueryExecutionFactory.create(queryString, dataset)) {
            qexec.execConstruct(resultModel);
        }
    });
    
    return resultModel;
}
```

## Extending the RDF Storage Service

To add more complex functionality to the RDF storage service:

### 1. Add Methods to the Interface

Define new methods in the `RdfStorageService` interface:

```java
/**
 * Executes a SPARQL query against the RDF store.
 *
 * @param queryString The SPARQL query string
 * @return A Model containing the query results
 */
Model executeSparqlQuery(String queryString);

/**
 * Updates the RDF store with a SPARQL UPDATE operation.
 *
 * @param updateString The SPARQL UPDATE string
 */
void executeSparqlUpdate(String updateString);
```

### 2. Implement the Methods

Implement the methods in `RdfStorageServiceImpl`:

```java
@Override
public Model executeSparqlQuery(String queryString) {
    Model resultModel = ModelFactory.createDefaultModel();
    
    dataset.executeRead(() -> {
        try (QueryExecution qexec = QueryExecutionFactory.create(queryString, dataset)) {
            if (qexec.getQuery().isConstructType()) {
                resultModel.add(qexec.execConstruct());
            } else if (qexec.getQuery().isDescribeType()) {
                resultModel.add(qexec.execDescribe());
            } else {
                throw new IllegalArgumentException("Only CONSTRUCT and DESCRIBE queries are supported");
            }
        }
    });
    
    return resultModel;
}

@Override
public void executeSparqlUpdate(String updateString) {
    dataset.executeWrite(() -> {
        try (UpdateProcessor processor = UpdateExecutionFactory.create(
                UpdateFactory.create(updateString), dataset)) {
            processor.execute();
        }
    });
}
```

## Implementing Additional Serialization Formats

To add support for a new RDF serialization format:

### 1. Update the RdfMediaType Class

```java
public class RdfMediaType {
    // Existing media types...
    
    // New media type
    public static final String APPLICATION_N_TRIPLES_VALUE = "application/n-triples";
    
    // Update the mapping methods
    public static Optional<Lang> getLangFromContentType(String contentType) {
        if (contentType == null) {
            return Optional.empty();
        }
        
        String normalizedContentType = contentType.split(";")[0].trim().toLowerCase();
        
        switch (normalizedContentType) {
            // Existing cases...
            case APPLICATION_N_TRIPLES_VALUE:
                return Optional.of(Lang.NTRIPLES);
            default:
                return Optional.empty();
        }
    }
    
    // Update other methods similarly...
}
```

### 2. Update the Controllers

Update the `produces` attribute in controller methods to include the new format:

```java
@GetMapping(value = "/{datasetId}", produces = {
    RdfMediaType.TEXT_TURTLE_VALUE,
    RdfMediaType.APPLICATION_LD_JSON_VALUE,
    RdfMediaType.APPLICATION_RDF_XML_VALUE,
    RdfMediaType.APPLICATION_N_TRIPLES_VALUE
})
public ResponseEntity<String> getDataset(
        @PathVariable String datasetId,
        @RequestHeader(HttpHeaders.ACCEPT) String acceptHeader) {
    // Existing code...
}
```

### 3. Update the Supported Media Types

Update the `SUPPORTED_RDF_MEDIA_TYPES` array in each controller:

```java
private static final String[] SUPPORTED_RDF_MEDIA_TYPES = {
    RdfMediaType.TEXT_TURTLE_VALUE,
    RdfMediaType.APPLICATION_LD_JSON_VALUE,
    RdfMediaType.APPLICATION_RDF_XML_VALUE,
    RdfMediaType.APPLICATION_N_TRIPLES_VALUE
};
```
