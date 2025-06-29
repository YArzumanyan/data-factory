package cz.cuni.mff.df_manager.service;

import cz.cuni.mff.df_manager.model.pipeline.PipelineConfig;

import java.util.List;

/**
 * Service for generating RDF data.
 */
public interface RdfService {

    /**
     * Generates RDF data for a dataset.
     *
     * @param title       The title of the dataset
     * @param description The description of the dataset
     * @param artifactId  The ID of the artifact in the artifact repository
     * @return The generated RDF data in Turtle format
     */
    String generateDatasetRdf(String title, String description, String artifactId);

    /**
     * Generates RDF data for a dataset with multiple artifacts.
     *
     * @param title       The title of the dataset
     * @param description The description of the dataset
     * @param artifactIds The list of artifact IDs in the artifact repository
     * @return The generated RDF data in Turtle format
     */
    String generateDatasetRdf(String title, String description, List<String> artifactIds);

    /**
     * Updates the distributions of an existing dataset.
     *
     * @param datasetUuid The UUID of the dataset to update
     * @param artifactIds The list of artifact IDs for the new distributions
     * @return The updated RDF data in Turtle format
     */
    String updateDatasetDistributions(String datasetUuid, List<String> artifactIds);

    /**
     * Generates RDF data for a plugin.
     *
     * @param title       The title of the plugin
     * @param description The description of the plugin
     * @param artifactId  The ID of the artifact in the artifact repository
     * @return The generated RDF data in Turtle format
     */
    String generatePluginRdf(String title, String description, String artifactId);

    /**
     * Generates RDF data for a pipeline.
     *
     * @param pipelineConfig The pipeline configuration
     * @return The generated RDF data in Turtle format
     */
    String generatePipelineRdf(PipelineConfig pipelineConfig);
}
