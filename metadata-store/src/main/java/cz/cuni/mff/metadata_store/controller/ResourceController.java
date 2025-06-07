package cz.cuni.mff.metadata_store.controller;

import cz.cuni.mff.metadata_store.service.RdfStorageService;
import cz.cuni.mff.metadata_store.utils.RdfMediaType;

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

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/resources")
@Tag(name = "Generic Resources", description = "Operations for retrieving any resource by its UUID")
public class ResourceController implements RdfController {

    private static final Logger log = LoggerFactory.getLogger(ResourceController.class);

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
    public ResourceController(RdfStorageService rdfStorageService) {
        this.rdfStorageService = rdfStorageService;
    }

    @GetMapping(value = "/{resourceId}", produces = {RdfMediaType.TEXT_TURTLE_VALUE, RdfMediaType.APPLICATION_LD_JSON_VALUE, RdfMediaType.APPLICATION_RDF_XML_VALUE})
    @Operation(summary = "Get any resource RDF by its UUID",
            parameters = @Parameter(name = "resourceId", description = "UUID of the resource to retrieve", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "RDF description of the resource"),
                    @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
                    @ApiResponse(responseCode = "406", description = "Unsupported Accept header format", content = @Content)
            })
    public ResponseEntity<String> getResource(
            @PathVariable String resourceId,
            @RequestHeader(HttpHeaders.ACCEPT) String acceptHeader) {

        Optional<Model> resourceModelOpt = rdfStorageService.getGenericResourceDescription(resourceId);

        if (resourceModelOpt.isEmpty()) {
            log.warn("Generic resource not found for ID: {}", resourceId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource with UUID " + resourceId + " not found.");
        }

        Model resourceModel = resourceModelOpt.get();
        return formatRdfResponse(resourceModel, acceptHeader);
    }
}
