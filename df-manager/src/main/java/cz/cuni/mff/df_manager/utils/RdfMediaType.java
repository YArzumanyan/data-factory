package cz.cuni.mff.df_manager.utils;

import org.springframework.http.MediaType;

/**
 * Utility class for RDF media types.
 */
public class RdfMediaType {

    /**
     * Media type for Turtle RDF format.
     */
    public static final String TEXT_TURTLE_VALUE = "text/turtle";
    
    /**
     * MediaType object for Turtle RDF format.
     */
    public static final MediaType TEXT_TURTLE = MediaType.parseMediaType(TEXT_TURTLE_VALUE);
    
    /**
     * Media type for JSON-LD RDF format.
     */
    public static final String APPLICATION_LD_JSON_VALUE = "application/ld+json";
    
    /**
     * MediaType object for JSON-LD RDF format.
     */
    public static final MediaType APPLICATION_LD_JSON = MediaType.parseMediaType(APPLICATION_LD_JSON_VALUE);
    
    /**
     * Media type for RDF/XML format.
     */
    public static final String APPLICATION_RDF_XML_VALUE = "application/rdf+xml";
    
    /**
     * MediaType object for RDF/XML format.
     */
    public static final MediaType APPLICATION_RDF_XML = MediaType.parseMediaType(APPLICATION_RDF_XML_VALUE);
    
    private RdfMediaType() {
        // Utility class, no instances
    }
}