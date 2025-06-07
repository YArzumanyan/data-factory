package cz.cuni.mff.metadata_store.controller;

import cz.cuni.mff.metadata_store.service.RdfStorageService;
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
import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/api/v1/plugins")
@Tag(name = "Plugins", description = "Operations related to Plugin descriptions (df:Plugin)")
public class PluginController implements RdfController {

    private static final Logger log = LoggerFactory.getLogger(PluginController.class);

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
    public PluginController(RdfStorageService rdfStorageService) {
        this.rdfStorageService = rdfStorageService;
    }

    @PostMapping(consumes = {RdfMediaType.TEXT_TURTLE_VALUE, RdfMediaType.APPLICATION_LD_JSON_VALUE, RdfMediaType.APPLICATION_RDF_XML_VALUE})
    @Operation(summary = "Store a plugin RDF graph",
            description = "Receives and persists a pre-validated RDF graph for a plugin (df:Plugin). Called by Middleware.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Plugin RDF stored successfully", headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Location", description = "URI of the created plugin resource")),
                    @ApiResponse(responseCode = "400", description = "Malformed RDF syntax or missing df:Plugin resource", content = @Content),
                    @ApiResponse(responseCode = "415", description = "Unsupported RDF Content-Type", content = @Content)
            })
    public ResponseEntity<String> createPlugin(
            InputStream requestBody,
            @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType) {

        log.debug("Attempting to parse request body with content type: {}", contentType);
        Model pluginModel = parseRdfData(requestBody, contentType);

        try {
            String resourceUri = rdfStorageService.storeRdfGraph(pluginModel, Vocab.Plugin);
            log.info("Plugin stored successfully with URI: {}", resourceUri);
            return ResponseEntity.status(HttpStatus.CREATED).body(resourceUri);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid plugin graph provided: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping(value = "/{pluginId}", produces = {RdfMediaType.TEXT_TURTLE_VALUE, RdfMediaType.APPLICATION_LD_JSON_VALUE, RdfMediaType.APPLICATION_RDF_XML_VALUE})
    @Operation(summary = "Get plugin definition RDF by ID",
            parameters = @Parameter(name = "pluginId", description = "UUID of the plugin", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Plugin definition in requested RDF format"),
                    @ApiResponse(responseCode = "404", description = "Plugin not found", content = @Content),
                    @ApiResponse(responseCode = "406", description = "Unsupported Accept header format", content = @Content)
            })
    public ResponseEntity<String> getPlugin(
            @PathVariable String pluginId,
            @RequestHeader(HttpHeaders.ACCEPT) String acceptHeader) {

        try {
            Model pluginModel = rdfStorageService.getPluginDescription(pluginId); // Throws NoSuchElementException
            return formatRdfResponse(pluginModel, acceptHeader);
        } catch (NoSuchElementException e) {
            log.warn("Plugin not found for ID: {}", pluginId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @GetMapping(produces = {RdfMediaType.TEXT_TURTLE_VALUE, RdfMediaType.APPLICATION_LD_JSON_VALUE, RdfMediaType.APPLICATION_RDF_XML_VALUE})
    @Operation(summary = "List all plugins as an RDF graph",
            description = "Retrieves an RDF graph containing descriptions of all registered plugins (df:Plugin).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "RDF graph containing all df:Plugin resources"),
                    @ApiResponse(responseCode = "406", description = "Unsupported Accept header format", content = @Content)
            })
    public ResponseEntity<String> listPlugins(
            @RequestHeader(HttpHeaders.ACCEPT) String acceptHeader) {

        Model listModel = rdfStorageService.listResources(Vocab.Plugin);
        return formatRdfResponse(listModel, acceptHeader);
    }
}
