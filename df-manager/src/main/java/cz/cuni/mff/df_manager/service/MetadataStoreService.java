package cz.cuni.mff.df_manager.service;

/**
 * Service for interacting with the metadata store.
 */
public interface MetadataStoreService {

    /**
     * Submits RDF data to the metadata store.
     *
     * @param resourceType The type of resource (ds, pl, pipe)
     * @param rdfData The RDF data in Turtle format
     * @return The response from the metadata store
     */
    String submitRdf(String resourceType, String rdfData);

    /**
     * Retrieves RDF data for a resource from the metadata store.
     *
     * @param resourceType The type of resource (ds, pl, pipe)
     * @param uuid The UUID of the resource
     * @return The RDF data in Turtle format
     */
    String getResourceRdf(String resourceType, String uuid);

    /**
     * Checks if a resource exists in the metadata store.
     *
     * @param resourceType The type of resource (ds, pl, pipe)
     * @param uuid The UUID of the resource
     * @return true if the resource exists, false otherwise
     */
    boolean resourceExists(String resourceType, String uuid);
}
