package cz.cuni.mff.metadata_store.controller;

import cz.cuni.mff.metadata_store.service.RdfStorageService;
import cz.cuni.mff.metadata_store.utils.RdfMediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;


import java.io.StringWriter;

@RestController
@RequestMapping("/api/v1/store")
@Tag(name = "Store Operations", description = "Operations related to the entire RDF store")
public class StoreController implements RdfController {

    private static final Logger log = LoggerFactory.getLogger(StoreController.class);

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
    public StoreController(RdfStorageService rdfStorageService) {
        this.rdfStorageService = rdfStorageService;
    }

    /**
     * Dumps the entire default graph of the RDF store in the requested format.
     *
     * @param acceptHeader the requested RDF media type (Accept header), defaults to Turtle if not specified
     * @param inline       whether to display content inline or trigger a download
     * @return a {@link ResponseEntity} containing the RDF dump in the requested format
     */
    @GetMapping(value = "/dump", produces = {RdfMediaType.TEXT_TURTLE_VALUE, RdfMediaType.APPLICATION_LD_JSON_VALUE, RdfMediaType.APPLICATION_RDF_XML_VALUE})
    @Operation(summary = "Dump the entire default graph of the RDF store",
            description = "Retrieves all triples residing in the default graph of the RDF store in the requested format. " +
                      "By default, prompts a download. Use the 'inline=true' query parameter to display directly in the browser.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "RDF dump successful"),
                    @ApiResponse(responseCode = "406", description = "Unsupported Accept header format", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal server error during serialization", content = @Content)
            },
            parameters = {
                @Parameter(name = "inline", in = ParameterIn.QUERY, description = "Set to 'true' to display content inline instead of triggering a download.", schema = @Schema(type = "boolean", defaultValue = "false"))
        })
    public ResponseEntity<String> dumpStore(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false, defaultValue = RdfMediaType.TEXT_TURTLE_VALUE) String acceptHeader,
            @RequestParam(value = "inline", required = false, defaultValue = "false") boolean inline
    ) {

        Lang requestedLang = RdfMediaType.getLangFromAcceptHeader(acceptHeader)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header: " + acceptHeader + ". Supported types: " + String.join(", ", SUPPORTED_RDF_MEDIA_TYPES)));

        try {
            Model storeModel = rdfStorageService.getEntireStoreModel();

            if (storeModel.isEmpty()) {
                log.info("Store dump requested, but the default graph is empty.");
            } else {
                log.info("Serializing store dump with {} triples as {}", storeModel.size(), requestedLang.getName());
            }

            StringWriter writer = new StringWriter();
            RDFDataMgr.write(writer, storeModel, requestedLang);

            String contentType = RdfMediaType.getContentTypeFromLang(requestedLang)
                    .orElse(RdfMediaType.TEXT_TURTLE_VALUE); // Fallback shouldn't be needed

            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType);

            if (!inline) {
                String filename = "store_dump." + requestedLang.getFileExtensions().getFirst();
                responseBuilder.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
                log.debug("Setting Content-Disposition to attachment for download.");
            } else {
                log.debug("Content-Disposition not set, allowing inline display.");
            }

            return responseBuilder.body(writer.toString());

        } catch (Exception e) {
            log.error("Error generating store dump: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate store dump", e);
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
        headers.add(HttpHeaders.ALLOW, "GET");
        return ResponseEntity.ok().headers(headers).build();
    }
}
