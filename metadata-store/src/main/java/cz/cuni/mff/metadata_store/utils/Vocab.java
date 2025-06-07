package cz.cuni.mff.metadata_store.utils;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Utility class holding constants for RDF vocabulary terms (URIs).
 */
public final class Vocab {

    // --- Namespaces ---
    public static final String DCAT_NS = "http://www.w3.org/ns/dcat#";
    public static final String DCTERMS_NS = "http://purl.org/dc/terms/";
    public static final String PPLAN_NS = "http://purl.org/net/p-plan#";
    public static final String PROV_NS = "http://www.w3.org/ns/prov#";
    public static final String LDP_NS = "http://www.w3.org/ns/ldp#";
    public static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String DF_NS = "http://localhost:8080/ns/df#";
    public static final String DS_NS = "http://localhost:8080/ns/ds#";
    public static final String PL_NS = "http://localhost:8080/ns/pl#";
    public static final String PIPE_NS = "http://localhost:8080/ns/pipe#";

    // --- Classes ---
    public static final Resource Dataset = ResourceFactory.createResource(DCAT_NS + "Dataset");
    public static final Resource Plan = ResourceFactory.createResource(PPLAN_NS + "Plan");
    public static final Resource Plugin = ResourceFactory.createResource(DF_NS + "Plugin");
    public static final Resource BasicContainer = ResourceFactory.createResource(LDP_NS + "BasicContainer");
    public static final Resource RootContainer = ResourceFactory.createResource(DF_NS + "root");

    // --- Properties ---
    public static final Property contains = ResourceFactory.createProperty(LDP_NS + "contains");
    public static final Property type = ResourceFactory.createProperty(RDF_NS + "type");
    public static final Property specializationOf = ResourceFactory.createProperty(PROV_NS + "specializationOf");
    public static final Property title = ResourceFactory.createProperty(DCTERMS_NS + "title");

    private Vocab() {}
}