package cz.cuni.mff.metadata_store.service;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Interface for the service layer responsible for interacting with the RDF triple store (Jena TDB2).
 */
public interface RdfStorageService {

    /**
     * Stores the provided RDF graph (Model) into the triple store.
     * Also updates the LDP root container if the graph contains a new dataset or plugin.
     *
     * @param rdfModel The Jena Model representing the RDF graph to store.
     * @param expectedResourceType The primary RDF type expected within the model (e.g., Vocab.Plan).
     * @return The URI of the primary resource created/identified within the stored graph.
     * @throws IllegalArgumentException If the input model is empty or lacks the expected primary resource.
     */
    String storeRdfGraph(Model rdfModel, Resource expectedResourceType);

    /**
     * Retrieves the RDF description of a specific resource identified by its UUID.
     *
     * @param resourceUuid The UUID of the resource.
     * @return An Optional containing the Jena Model describing the resource,
     * or Optional.empty() if not found.
     */
    Optional<Model> getGenericResourceDescription(String resourceUuid);


    /**
     * Retrieves the RDF description of a specific Pipeline resource identified by its UUID.
     *
     * @param pipelineUuid The UUID of the pipeline resource (Vocab.Plan).
     * @return An Optional containing the Jena Model describing the pipeline.
     * @throws NoSuchElementException if the resource with the given UUID is not found.
     */
    Model getPipelineDescription(String pipelineUuid) throws NoSuchElementException;

    /**
     * Retrieves the RDF description of a specific Pipeline resource identified by its UUID,
     * including all dependencies (e.g., datasets, plugins) referenced by the pipeline.
     *
     * @param pipelineUuid The UUID of the pipeline resource (Vocab.Plan).
     * @return A Jena Model containing the pipeline description and its dependencies.
     * @throws NoSuchElementException if the resource with the given UUID is not found.
     */
    Model getPipelineDescriptionWithDependencies(String pipelineUuid) throws NoSuchElementException;

    /**
     * Retrieves the RDF description of a specific Dataset resource identified by its UUID.
     *
     * @param datasetUuid The UUID of the dataset resource (Vocab.Dataset).
     * @return An Optional containing the Jena Model describing the dataset.
     * @throws NoSuchElementException if the resource with the given UUID is not found.
     */
    Model getDatasetDescription(String datasetUuid) throws NoSuchElementException;


    /**
     * Retrieves the RDF description of a specific Plugin resource identified by its UUID.
     *
     * @param pluginUuid The UUID of the plugin resource (Vocab.Plugin).
     * @return An Optional containing the Jena Model describing the plugin.
     * @throws NoSuchElementException if the resource with the given UUID is not found.
     */
    Model getPluginDescription(String pluginUuid) throws NoSuchElementException;


    /**
     * Retrieves an RDF graph containing descriptions of all resources of a specific type
     * (e.g., all dcat:Dataset or df:Plugin).
     *
     * @param resourceType The RDF class (Resource) of the resources to list (e.g., Vocab.Dataset).
     * @return A Jena Model containing the descriptions of all matching resources. Can be empty if none found.
     */
    Model listResources(Resource resourceType);

    /**
     * Retrieves an RDF graph containing descriptions of all resources of a specific type
     * (e.g., all dcat:Dataset or df:Plugin), including their distributions.
     *
     * @param resourceType The RDF class (Resource) of the resources to list (e.g., Vocab.Dataset).
     * @return A Jena Model containing the descriptions of all matching resources with their distributions.
     */
    Model listResourcesWithDistributions(Resource resourceType);

    /**
     * Retrieves a copy of the entire default graph from the RDF store.
     *
     * @return A Jena Model containing all triples in the default graph.
     */
    Model getEntireStoreModel();

    /**
     * Overwrites dataset
     * with the given UUID with the provided RDF data.
     *
     * @param datasetUuid The UUID of the dataset to update.
     * @param rdfData The RDF data in Turtle format to update the dataset with.
     * @return The updated RDF data in Turtle format.
     */
    String updateDataset(String datasetUuid, Model rdfData) throws NoSuchElementException;

    /**
     * Overwrites plugin
     * with the given UUID with the provided RDF data.
     *
     * @param pluginUuid The UUID of the plugin to update.
     * @param rdfData The RDF data in Turtle format to update the plugin with.
     * @return The updated RDF data in Turtle format.
     */
    String updatePlugin(String pluginUuid, Model rdfData) throws NoSuchElementException;
}
