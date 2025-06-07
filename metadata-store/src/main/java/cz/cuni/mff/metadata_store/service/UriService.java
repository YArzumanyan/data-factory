package cz.cuni.mff.metadata_store.service;

import cz.cuni.mff.metadata_store.utils.Vocab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for generating and parsing resource URIs based on the
 * configured base URI and resource types.
 */
@Service
public class UriService {

    private static final Logger log = LoggerFactory.getLogger(UriService.class);

    @Value("${metadata-store.baseUri}")
    private String configuredBaseUri;

    /**
     * Initializes the UriService by validating the configured base URI.
     */
    @PostConstruct
    public void init() {
        if (configuredBaseUri == null || configuredBaseUri.isBlank()) {
            log.error("Configured base URI is null or empty. Please check your configuration.");
            throw new IllegalStateException("Base URI must be configured.");
        }
        if (!configuredBaseUri.endsWith("/")) {
            configuredBaseUri += "/";
        }
        log.info("UriService initialized with base URI: {}", configuredBaseUri);
    }

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
