package cz.cuni.mff.metadata_store.utils;

import org.apache.jena.riot.Lang;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.Optional;

public class RdfMediaType {

    public static final String TEXT_TURTLE_VALUE = "text/turtle";
    public static final String APPLICATION_LD_JSON_VALUE = "application/ld+json";
    public static final String APPLICATION_RDF_XML_VALUE = "application/rdf+xml";

    private static final Map<String, Lang> MIME_TYPE_TO_LANG = Map.of(
        TEXT_TURTLE_VALUE, Lang.TURTLE,
        APPLICATION_LD_JSON_VALUE, Lang.JSONLD,
        APPLICATION_RDF_XML_VALUE, Lang.RDFXML
    );

    /**
     * Determines the Jena Lang based on a Content-Type string.
     * @param contentType The Content-Type header value.
     * @return Optional containing the matching Lang, or empty if not supported/recognized.
     */
    public static Optional<Lang> getLangFromContentType(String contentType) {
        if (contentType == null) return Optional.empty();
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            for (Map.Entry<String, Lang> entry : MIME_TYPE_TO_LANG.entrySet()) {
                if (mediaType.isCompatibleWith(MediaType.parseMediaType(entry.getKey()))) {
                    return Optional.of(entry.getValue());
                }
            }
        } catch (InvalidMediaTypeException e) {
            // Ignore
        }
        return Optional.empty();
    }

    /**
     * Determines the Jena Lang based on an Accept header string, choosing the best match.
     * Simple implementation: checks for supported types in order of preference.
     * @param acceptHeader The Accept header value.
     * @return Optional containing the preferred matching Lang, or empty if none match.
     * Defaults to Turtle if the header is null, empty, or '*\/*'.
    */
    public static Optional<Lang> getLangFromAcceptHeader(String acceptHeader) {
        if (acceptHeader == null || acceptHeader.isBlank() || acceptHeader.trim().equals("*/*")) {
            return Optional.of(Lang.TURTLE);
        }

        try {
            String lowerCaseHeader = acceptHeader.toLowerCase();

            if (lowerCaseHeader.contains(TEXT_TURTLE_VALUE)) return Optional.of(Lang.TURTLE);
            if (lowerCaseHeader.contains(APPLICATION_LD_JSON_VALUE)) return Optional.of(Lang.JSONLD);
            if (lowerCaseHeader.contains(APPLICATION_RDF_XML_VALUE)) return Optional.of(Lang.RDFXML);

        } catch (InvalidMediaTypeException e) {
            // Ignore
        }

        return Optional.empty();
    }

    /**
     * Gets the corresponding Content-Type string for a Jena Lang.
     * @param lang The Jena Lang.
     * @return Optional containing the Content-Type string, or empty if Lang is not mapped.
     */
    public static Optional<String> getContentTypeFromLang(Lang lang) {
        if (lang == Lang.TURTLE) return Optional.of(TEXT_TURTLE_VALUE);
        if (lang == Lang.JSONLD) return Optional.of(APPLICATION_LD_JSON_VALUE);
        if (lang == Lang.RDFXML) return Optional.of(APPLICATION_RDF_XML_VALUE);
        return Optional.empty();
    }

    private RdfMediaType() {}
}