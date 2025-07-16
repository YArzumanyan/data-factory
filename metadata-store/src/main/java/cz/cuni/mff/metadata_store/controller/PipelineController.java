package cz.cuni.mff.metadata_store.controller;

import cz.cuni.mff.metadata_store.service.RdfStorageService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;
import cz.cuni.mff.metadata_store.utils.RdfMediaType;
import cz.cuni.mff.metadata_store.utils.Vocab;

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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;

@RestController
@RequestMapping("/api/v1/pipelines")
@Tag(name = "Pipelines", description = "Operations related to Pipeline definitions (p-plan:Plan)")
public class PipelineController implements RdfController {

    private static final Logger log = LoggerFactory.getLogger(PipelineController.class);

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
    public PipelineController(RdfStorageService rdfStorageService) {
        this.rdfStorageService = rdfStorageService;
    }

    /**
     * Creates a new pipeline definition by storing the provided RDF graph.
     *
     * @param requestBody InputStream containing RDF data
     * @param contentType Content-Type of the request
     * @return Parsed Jena Model containing the RDF data
     */
    @PostMapping(consumes = {RdfMediaType.TEXT_TURTLE_VALUE, RdfMediaType.APPLICATION_LD_JSON_VALUE, RdfMediaType.APPLICATION_RDF_XML_VALUE})
    @Operation(summary = "Store a pipeline definition RDF graph",
            description = "Receives and persists a complete RDF graph for a pipeline plan (p-plan:Plan). Called by Middleware.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Pipeline RDF stored successfully", headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Location", description = "URI of the created pipeline plan resource")),
                    @ApiResponse(responseCode = "400", description = "Malformed RDF syntax or missing p-plan:Plan resource", content = @Content),
                    @ApiResponse(responseCode = "415", description = "Unsupported RDF Content-Type", content = @Content)
            })
    public ResponseEntity<String> createPipeline(
            @Parameter(description = "Input stream containing RDF data for the pipeline") InputStream requestBody,
            @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType) throws URISyntaxException {

        log.debug("Attempting to parse request body with content type: {}", contentType);
        Model pipelineModel = parseRdfData(requestBody, contentType);

        try {
            String resourceUri = rdfStorageService.storeRdfGraph(pipelineModel, Vocab.Plan);
            log.info("Pipeline stored successfully with URI: {}", resourceUri);
            HttpHeaders headers = ldpHeaders();
            headers.setLocation(new URI(resourceUri));
            return new ResponseEntity<>(resourceUri, headers, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            // Handles cases where storeRdfGraph determines the input is invalid (e.g., no Plan)
            log.warn("Invalid pipeline graph provided: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * Retrieves a pipeline definition by its ID in the requested RDF format.
     *
     * @param planId UUID of the pipeline plan
     * @param full If true, includes dependencies in the response
     * @param acceptHeader Accept header specifying the desired RDF format
     * @return ResponseEntity containing the RDF representation of the pipeline
     */
    @GetMapping(value = "/{planId}", produces = {RdfMediaType.TEXT_TURTLE_VALUE, RdfMediaType.APPLICATION_LD_JSON_VALUE, RdfMediaType.APPLICATION_RDF_XML_VALUE})
    @Operation(summary = "Get pipeline definition RDF by ID",
            parameters = @Parameter(name = "planId", description = "UUID of the pipeline plan", required = true, example = "d290f1ee-6c54-4b01-90e6-d701748f0851"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pipeline definition in requested RDF format"),
                    @ApiResponse(responseCode = "404", description = "Pipeline plan not found", content = @Content),
                    @ApiResponse(responseCode = "406", description = "Unsupported Accept header format", content = @Content)
            })
    public ResponseEntity<String> getPipeline(
            @PathVariable String planId,
            @RequestParam(required = false, defaultValue = "true") boolean full,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false, defaultValue = RdfMediaType.TEXT_TURTLE_VALUE) String acceptHeader) {

        try {
            Model pipelineModel = full
                    ? rdfStorageService.getPipelineDescriptionWithDependencies(planId)
                    : rdfStorageService.getPipelineDescription(planId);
            return formatRdfResponse(pipelineModel, acceptHeader);
        } catch (NoSuchElementException e) {
            log.warn("Pipeline not found for ID: {}", planId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    /**
     * Lists all pipeline definitions in the specified RDF format.
     *
     * @param acceptHeader Accept header specifying the desired RDF format
     * @return ResponseEntity containing the RDF representation of all pipelines
     */
    @GetMapping(produces = {RdfMediaType.TEXT_TURTLE_VALUE, RdfMediaType.APPLICATION_LD_JSON_VALUE, RdfMediaType.APPLICATION_RDF_XML_VALUE})
    @Operation(summary = "List all pipelines",
            description = "Retrieves a list of all pipeline definitions in the specified RDF format.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of pipelines in requested RDF format"),
                    @ApiResponse(responseCode = "406", description = "Unsupported Accept header format", content = @Content)
            })
    public ResponseEntity<String> listPipelines(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false, defaultValue = RdfMediaType.TEXT_TURTLE_VALUE) String acceptHeader) {

        Model pipelinesModel = rdfStorageService.listResources(Vocab.Plan);
        return formatRdfResponse(pipelinesModel, acceptHeader);
    }

    /**
     * Checks if a pipeline plan exists by its ID.
     *
     * @param planId UUID of the pipeline plan
     * @return ResponseEntity with status 200 if exists, 404 if not found
     */
    @RequestMapping(method = RequestMethod.HEAD, value = "/{planId}")
    public ResponseEntity<Void> headPipeline(@PathVariable String planId) {
        try {
            rdfStorageService.getPipelineDescription(planId);
            return ResponseEntity.ok().headers(ldpHeaders()).build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().headers(ldpHeaders()).build();
        }
    }

    /**
     * Returns the allowed HTTP methods for this endpoint.
     *
     * @return ResponseEntity with allowed methods in headers
     */
    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> options() {
        HttpHeaders headers = ldpHeaders();
        headers.add(HttpHeaders.ALLOW, "GET, HEAD, OPTIONS");
        return ResponseEntity.ok().headers(headers).build();
    }
}
