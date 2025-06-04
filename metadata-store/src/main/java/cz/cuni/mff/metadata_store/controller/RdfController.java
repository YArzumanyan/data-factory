package cz.cuni.mff.metadata_store.controller;

import cz.cuni.mff.metadata_store.utils.RdfMediaType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.io.StringWriter;

/**
 * Interface for controllers that handle RDF data.
 * Provides common methods for parsing, validating, and formatting RDF data.
 */
public interface RdfController {

    /**
     * Get the supported RDF media types for this controller.
     * @return Array of supported RDF media types
     */
    String[] getSupportedRdfMediaTypes();

    /**
     * Parse RDF data from an input stream using the specified content type.
     * @param requestBody The input stream containing RDF data
     * @param contentType The content type of the RDF data
     * @return The parsed RDF model
     * @throws ResponseStatusException if the content type is not supported or the RDF data is malformed
     */
    default Model parseRdfData(InputStream requestBody, String contentType) {
        Lang lang = RdfMediaType.getLangFromContentType(contentType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, 
                        "Unsupported Content-Type: " + contentType));

        Model model = ModelFactory.createDefaultModel();
        try {
            RDFDataMgr.read(model, requestBody, lang);
        } catch (org.apache.jena.riot.RiotException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Malformed RDF syntax: " + e.getMessage(), e);
        }

        if (model.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Received empty RDF graph.");
        }

        return model;
    }

    /**
     * Format an RDF model as a string using the specified accept header.
     * @param model The RDF model to format
     * @param acceptHeader The accept header specifying the desired format
     * @return A ResponseEntity containing the formatted RDF data
     * @throws ResponseStatusException if the accept header is not supported
     */
    default ResponseEntity<String> formatRdfResponse(Model model, String acceptHeader) {
        Lang requestedLang = RdfMediaType.getLangFromAcceptHeader(acceptHeader)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, 
                        "Unsupported Accept header: " + acceptHeader + ". Supported types: " + 
                        String.join(", ", getSupportedRdfMediaTypes())));

        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, model, requestedLang);

        String contentType = RdfMediaType.getContentTypeFromLang(requestedLang)
                .orElse(RdfMediaType.TEXT_TURTLE_VALUE); // Fallback

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, contentType).body(writer.toString());
    }
}