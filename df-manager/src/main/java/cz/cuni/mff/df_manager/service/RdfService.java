package cz.cuni.mff.df_manager.service;

import cz.cuni.mff.df_manager.model.pipeline.PipelineConfig;

/**
 * Service for generating RDF data.
 */
public interface RdfService {
    
    /**
     * Generates RDF data for a dataset.
     *
     * @param title The title of the dataset
     * @param description The description of the dataset
     * @param artifactId The ID of the artifact in the artifact repository
     * @return The generated RDF data in Turtle format
     */
    String generateDatasetRdf(String title, String description, String artifactId);
    
    /**
     * Generates RDF data for a plugin.
     *
     * @param title The title of the plugin
     * @param description The description of the plugin
     * @param artifactId The ID of the artifact in the artifact repository
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