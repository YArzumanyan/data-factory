package cz.cuni.mff.metadata_store.service;

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

    public static final String PIPELINES_PATH = "pipelines/";
    public static final String DATASETS_PATH = "datasets/";
    public static final String PLUGINS_PATH = "plugins/";

    @Value("${metadata-store.baseUri}")
    private String configuredBaseUri;

    private String effectiveBaseUri;

    /**
     * Post-construction validation and processing of the base URI.
     * Ensures the base URI ends with a '/' character.
     */
    @PostConstruct
    private void initialize() {
        if (configuredBaseUri == null || configuredBaseUri.isBlank()) {
            log.error("Configuration property 'metadata-store.baseUri' is not set!");
            this.effectiveBaseUri = "http://localhost:8080/metadata/";
        } else {
            this.effectiveBaseUri = configuredBaseUri.strip();
            if (!this.effectiveBaseUri.endsWith("/")) {
                this.effectiveBaseUri += "/";
            }
        }
        log.info("Effective base URI set to: {}", this.effectiveBaseUri);
    }

    /**
     * Builds the full URI for a pipeline resource.
     *
     * @param uuid The unique identifier (UUID) of the pipeline.
     * @return The full URI string.
     */
    public String buildPipelineUri(String uuid) {
        return buildGenericUri(PIPELINES_PATH, uuid);
    }

    /**
     * Builds the full URI for a dataset resource.
     *
     * @param uuid The unique identifier (UUID) of the dataset.
     * @return The full URI string.
     */
    public String buildDatasetUri(String uuid) {
        return buildGenericUri(DATASETS_PATH, uuid);
    }

    /**
     * Builds the full URI for a plugin resource.
     *
     * @param uuid The unique identifier (UUID) of the plugin.
     * @return The full URI string.
     */
    public String buildPluginUri(String uuid) {
        return buildGenericUri(PLUGINS_PATH, uuid);
    }

    /**
     * Generic method to build a resource URI.
     *
     * @param typePath The path segment identifying the resource type (e.g., "pipelines/").
     * @param uuid     The unique identifier (UUID) of the resource.
     * @return The full URI string (e.g., "http://base.uri/pipelines/uuid-123").
     */
    public String buildGenericUri(String typePath, String uuid) {
        if (uuid == null || uuid.isBlank()) {
            log.warn("Attempted to build URI with null or blank UUID for type path: {}", typePath);
            return null;
        }

        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            log.warn("Provided ID '{}' is not a valid UUID format.", uuid);
        }

        return effectiveBaseUri + typePath + uuid;
    }

    public String buildRootUri() {
        return effectiveBaseUri + "root";
    }

    /**
     * Extracts the UUID part from a resource URI string that matches the expected pattern.
     * Expected pattern: <baseUri>/<typePath>/<uuid>
     *
     * @param resourceUri The full resource URI string.
     * @return An Optional containing the extracted UUID string, or Optional.empty() if
     * the URI doesn't match the expected pattern or doesn't contain a UUID part.
     */
    public Optional<String> extractUuid(String resourceUri) {
        if (resourceUri == null || !resourceUri.startsWith(effectiveBaseUri)) {
            log.debug("URI '{}' does not start with the effective base URI '{}'", resourceUri, effectiveBaseUri);
            return Optional.empty();
        }

        int lastSlashIndex = resourceUri.lastIndexOf('/');
        if (lastSlashIndex == -1 || lastSlashIndex == resourceUri.length() - 1) {
            log.debug("URI '{}' does not contain a final path segment for UUID extraction.", resourceUri);
            return Optional.empty();
        }

        String prefix = resourceUri.substring(0, lastSlashIndex + 1); // Include the slash
        boolean knownPrefix = prefix.equals(effectiveBaseUri + PIPELINES_PATH) ||
                prefix.equals(effectiveBaseUri + DATASETS_PATH) ||
                prefix.equals(effectiveBaseUri + PLUGINS_PATH);

        if (!knownPrefix) {
            if (!prefix.startsWith(effectiveBaseUri)){
                log.debug("URI '{}' prefix doesn't match known types or base structure.", resourceUri);
                return Optional.empty();
            }
        }


        String potentialUuid = resourceUri.substring(lastSlashIndex + 1);

        try {
            UUID.fromString(potentialUuid);
            return Optional.of(potentialUuid);
        } catch (IllegalArgumentException e) {
            log.warn("Extracted segment '{}' from URI '{}' is not a valid UUID format.", potentialUuid, resourceUri);
            return Optional.empty();
        }
    }
}
