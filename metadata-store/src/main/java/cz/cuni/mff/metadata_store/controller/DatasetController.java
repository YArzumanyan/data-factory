package cz.cuni.mff.metadata_store.controller;

import cz.cuni.mff.metadata_store.service.RdfStorageService;
import cz.cuni.mff.metadata_store.utils.RdfMediaType;
import cz.cuni.mff.metadata_store.utils.Vocab;

import java.util.NoSuchElementException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/api/v1/datasets")
@Tag(name = "Datasets", description = "Operations related to Dataset descriptions (dcat:Dataset)")
public class DatasetController implements RdfController {

    private static final Logger log = LoggerFactory.getLogger(DatasetController.class);

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
    public DatasetController(RdfStorageService rdfStorageService) {
        this.rdfStorageService = rdfStorageService;
    }

    @PutMapping(value = "/{datasetId}", consumes = {RdfMediaType.TEXT_TURTLE_VALUE, RdfMediaType.APPLICATION_LD_JSON_VALUE, RdfMediaType.APPLICATION_RDF_XML_VALUE, MediaType.TEXT_PLAIN_VALUE})
    @Operation(summary = "Update an existing dataset RDF graph",
            description = "Updates an existing dataset (dcat:Dataset) with a new RDF graph. The provided graph must contain the complete updated state of the dataset.",
            parameters = @Parameter(name = "datasetId", description = "UUID of the dataset to update", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dataset updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Dataset not found", content = @Content),
                    @ApiResponse(responseCode = "415", description = "Unsupported RDF Content-Type", content = @Content)
            })
    public ResponseEntity<String> updateDataset(
            @Parameter(description = "Input stream containing RDF data for the dataset") InputStream requestBody,
            @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType, @PathVariable String datasetId) {

        log.debug("Attempting to update dataset with ID: {} and content type: {}", datasetId, contentType);
        Model datasetModel = parseRdfData(requestBody, contentType);

        try {
            rdfStorageService.updateDataset(datasetId, datasetModel);
            log.info("Dataset updated successfully with ID: {}", datasetId);
            return ResponseEntity.status(HttpStatus.OK).headers(ldpHeaders()).body(datasetId);
        } catch (NoSuchElementException e) {
            log.warn("Dataset not found for ID: {}", datasetId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid dataset graph provided for update: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping(consumes = {RdfMediaType.TEXT_TURTLE_VALUE, RdfMediaType.APPLICATION_LD_JSON_VALUE, RdfMediaType.APPLICATION_RDF_XML_VALUE, MediaType.TEXT_PLAIN_VALUE})
    @Operation(summary = "Store a dataset RDF graph",
            description = "Receives and persists a pre-validated RDF graph for a dataset (dcat:Dataset). Called by Middleware.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Dataset RDF stored successfully", headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Location", description = "URI of the created dataset resource")),
                    @ApiResponse(responseCode = "400", description = "Malformed RDF syntax or missing dcat:Dataset resource", content = @Content),
                    @ApiResponse(responseCode = "415", description = "Unsupported RDF Content-Type", content = @Content)
            })
    public ResponseEntity<String> createDataset(
            @Parameter(description = "Input stream containing RDF data for the dataset") InputStream requestBody,
            @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType) throws URISyntaxException {

        log.debug("Attempting to parse request body with content type: {}", contentType);
        Model datasetModel = parseRdfData(requestBody, contentType);

        try {
            String resourceUri = rdfStorageService.storeRdfGraph(datasetModel, Vocab.Dataset);
            log.info("Dataset stored successfully with URI: {}", resourceUri);
            HttpHeaders headers = ldpHeaders();
            headers.setLocation(new URI(resourceUri));
            return new ResponseEntity<>(resourceUri, headers, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid dataset graph provided: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping(value = "/{datasetId}", produces = {RdfMediaType.TEXT_TURTLE_VALUE, RdfMediaType.APPLICATION_LD_JSON_VALUE, RdfMediaType.APPLICATION_RDF_XML_VALUE})
    @Operation(summary = "Get dataset definition RDF by ID",
            parameters = @Parameter(name = "datasetId", description = "UUID of the dataset", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dataset definition in requested RDF format"),
                    @ApiResponse(responseCode = "404", description = "Dataset not found", content = @Content),
                    @ApiResponse(responseCode = "406", description = "Unsupported Accept header format", content = @Content)
            })
    public ResponseEntity<String> getDataset(
            @PathVariable String datasetId,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false, defaultValue = RdfMediaType.TEXT_TURTLE_VALUE) String acceptHeader) {

        try {
            Model datasetModel = rdfStorageService.getDatasetDescription(datasetId); // Throws NoSuchElementException
            return formatRdfResponse(datasetModel, acceptHeader);
        } catch (NoSuchElementException e) {
            log.warn("Dataset not found for ID: {}", datasetId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @GetMapping(produces = {RdfMediaType.TEXT_TURTLE_VALUE, RdfMediaType.APPLICATION_LD_JSON_VALUE, RdfMediaType.APPLICATION_RDF_XML_VALUE})
    @Operation(summary = "List all datasets as an RDF graph",
            description = "Retrieves an RDF graph containing descriptions of all registered datasets (dcat:Dataset).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "RDF graph containing all dcat:Dataset resources"),
                    @ApiResponse(responseCode = "406", description = "Unsupported Accept header format", content = @Content)
            })
    public ResponseEntity<String> listDatasets(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false, defaultValue = RdfMediaType.TEXT_TURTLE_VALUE) String acceptHeader) {

        Model listModel = rdfStorageService.listResourcesWithDistributions(Vocab.Dataset);
        return formatRdfResponse(listModel, acceptHeader);
    }

    @RequestMapping(method = RequestMethod.HEAD, value = "/{datasetId}")
    public ResponseEntity<Void> headDataset(@PathVariable String datasetId) {
        try {
            rdfStorageService.getDatasetDescription(datasetId);
            return ResponseEntity.ok().headers(ldpHeaders()).build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().headers(ldpHeaders()).build();
        }
    }

    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> options() {
        HttpHeaders headers = ldpHeaders();
        headers.add(HttpHeaders.ALLOW, "GET, HEAD, OPTIONS, PUT");
        return ResponseEntity.ok().headers(headers).build();
    }
}
