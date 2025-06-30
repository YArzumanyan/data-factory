package cz.cuni.mff.metadata_store.service;

import cz.cuni.mff.metadata_store.utils.Vocab;
import org.springframework.stereotype.Service;

/**
 * Service responsible for generating and parsing resource URIs based on the
 * configured base URI and resource types.
 */
@Service
public class UriService {
    /**
     * Builds the full URI for a pipeline resource.
     *
     * @param uuid The unique identifier (UUID) of the pipeline.
     * @return The full URI string.
     */
    public String buildPipelineUri(String uuid) {
        String namespace = Vocab.PIPE_NS;
        return namespace + uuid;
    }

    /**
     * Builds the full URI for a dataset resource.
     *
     * @param uuid The unique identifier (UUID) of the dataset.
     * @return The full URI string.
     */
    public String buildDatasetUri(String uuid) {
        String namespace = Vocab.DS_NS;
        return namespace + uuid;
    }

    /**
     * Builds the full URI for a plugin resource.
     *
     * @param uuid The unique identifier (UUID) of the plugin.
     * @return The full URI string.
     */
    public String buildPluginUri(String uuid) {
        String namespace = Vocab.PL_NS;
        return namespace + uuid;
    }
}
