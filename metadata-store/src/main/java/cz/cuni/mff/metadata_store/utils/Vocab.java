package cz.cuni.mff.metadata_store.utils;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Utility class holding constants for RDF vocabulary terms (URIs).
 */
public final class Vocab {
    private static final String namespaceUri = System.getenv().getOrDefault("RDF_NAMESPACE_BASE",
            "http://localhost:8080/ns/");

    private static final String dfNamespace = System.getenv().getOrDefault("RDF_NAMESPACE_DF", "df");

    private static final String pipeNamespace = System.getenv().getOrDefault("RDF_NAMESPACE_PIPE", "pipe");

    private static final String stepNamespace = System.getenv().getOrDefault("RDF_NAMESPACE_STEP", "step");

    private static final String varNamespace = System.getenv().getOrDefault("RDF_NAMESPACE_VAR", "var");

    private static final String dsNamespace = System.getenv().getOrDefault("RDF_NAMESPACE_DS", "ds");

    private static final String plNamespace = System.getenv().getOrDefault("RDF_NAMESPACE_PL", "pl");

    // --- Namespaces ---
    public static final String DCAT_NS = "http://www.w3.org/ns/dcat#";
    public static final String PPLAN_NS = "http://purl.org/net/p-plan#";
    public static final String DCTERMS_NS = "http://purl.org/dc/terms/";
    public static final String LDP_NS = "http://www.w3.org/ns/ldp#";
    public static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String RDF_NAMESPACE_BASE = ensureNamespace(namespaceUri);
    public static final String DF_NS = ensureNamespaceWithHash(RDF_NAMESPACE_BASE + dfNamespace);
    public static final String DS_NS = ensureNamespaceWithHash(RDF_NAMESPACE_BASE + dsNamespace);
    public static final String PL_NS = ensureNamespaceWithHash(RDF_NAMESPACE_BASE + plNamespace);
    public static final String PIPE_NS = ensureNamespaceWithHash(RDF_NAMESPACE_BASE + pipeNamespace);
    public static final String STEP_NS = ensureNamespaceWithHash(RDF_NAMESPACE_BASE + stepNamespace);
    public static final String VAR_NS = ensureNamespaceWithHash(RDF_NAMESPACE_BASE + varNamespace);

    // --- Classes ---
    public static final Resource Dataset = ResourceFactory.createResource(DCAT_NS + "Dataset");
    public static final Resource Plan = ResourceFactory.createResource(PPLAN_NS + "Plan");
    public static final Resource Plugin = ResourceFactory.createResource(DF_NS + "Plugin");
    public static final Resource RootContainer = ResourceFactory.createResource(DF_NS + "root");

    // --- Properties ---
    public static final Property contains = ResourceFactory.createProperty(LDP_NS + "contains");
    public static final Property type = ResourceFactory.createProperty(RDF_NS + "type");

    private Vocab() {
    }

    private static String ensureNamespace(String namespace) {
        return namespace.endsWith("/") ? namespace : namespace + "/";
    }

    private static String ensureNamespaceWithHash(String namespace) {
        return namespace.endsWith("#") ? namespace : namespace + "#";
    }
}