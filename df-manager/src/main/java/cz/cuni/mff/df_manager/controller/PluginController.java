package cz.cuni.mff.df_manager.controller;

import cz.cuni.mff.df_manager.service.ArtifactRepositoryService;
import cz.cuni.mff.df_manager.service.MetadataStoreService;
import cz.cuni.mff.df_manager.service.RdfService;
import cz.cuni.mff.df_manager.utils.RdfMediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

/**
 * REST controller for plugin operations.
 */
@RestController
@RequestMapping("/api/v1/plugins")
@RequiredArgsConstructor
@Slf4j
public class PluginController {

    private final ArtifactRepositoryService artifactRepositoryService;
    private final MetadataStoreService metadataStoreService;
    private final RdfService rdfService;

    /**
     * Uploads a plugin file and creates metadata for it.
     *
     * @param file The plugin file
     * @param title The title of the plugin
     * @param description The description of the plugin
     * @return RDF data for the created plugin
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = RdfMediaType.TEXT_TURTLE_VALUE)
    public ResponseEntity<String> uploadPlugin(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description) {

        log.info("Uploading plugin: {}", title);

        try {
            // Upload the file to artifact repository
            String artifactId = artifactRepositoryService.uploadArtifact(file);
            log.info("Artifact uploaded with ID: {}", artifactId);

            // Determine file extension
            String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);

            // Generate RDF for plugin
            String rdfData = rdfService.generatePluginRdf(title, description, artifactId);

            // Submit RDF to metadata store
            String response = metadataStoreService.submitRdf("pl", rdfData, null);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error uploading plugin", e);
            return ResponseEntity.badRequest().build();
        }
    }

    private ResponseEntity<String> updatePluginDist(String uuid, MultipartFile file) {
        // Validate that plugin exists
        if (!metadataStoreService.resourceExists("pl", uuid)) {
            log.error("Plugin with UUID {} not found", uuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Upload the file to the artifact repository
        String artifactId;
        try {
            artifactId = artifactRepositoryService.uploadArtifact(file);
            log.info("Artifact uploaded with ID: {}", artifactId);
        } catch (Exception e) {
            log.error("Error uploading plugin file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Update the plugin's distribution
        String rdfData;
        try {
            rdfData = rdfService.updatePluginDistribution(uuid, artifactId);
            log.info("Updated RDF for plugin: {}", rdfData);
        } catch (Exception e) {
            log.error("Error updating plugin distribution", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Submit updated RDF to metadata store
        String response;
        try {
            response = metadataStoreService.submitRdf("pl", rdfData, uuid);
            log.info("Plugin distribution updated successfully, response: {}", response);
        } catch (Exception e) {
            log.error("Error submitting updated RDF to metadata store", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates the distribution of an existing plugin with a new file.
     *
     * @param uuid The UUID of the plugin
     * @param file The new plugin file
     * @return RDF data for the updated plugin
     */
    @PostMapping(value = "/{uuid}/distribution", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = RdfMediaType.TEXT_TURTLE_VALUE)
    public ResponseEntity<String> updatePluginDistribution(
            @PathVariable String uuid,
            @RequestParam("file") MultipartFile file) {

        log.info("Updating distribution for plugin: {}, file: {}", uuid, file.getOriginalFilename());

        try {
            return updatePluginDist(uuid, file);
        } catch (Exception e) {
            log.error("Error updating plugin distribution", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Retrieves metadata for a plugin.
     *
     * @param uuid The UUID of the plugin
     * @return RDF data for the plugin
     */
    @GetMapping(value = "/{uuid}", produces = RdfMediaType.TEXT_TURTLE_VALUE)
    public ResponseEntity<String> getPlugin(@PathVariable String uuid) {
        log.info("Retrieving plugin: {}", uuid);

        try {
            String rdfData = metadataStoreService.getResourceRdf("pl", uuid);
            return ResponseEntity.ok(rdfData);
        } catch (Exception e) {
            log.error("Error retrieving plugin", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Lists all plugins as an RDF graph.
     *
     * @return RDF data containing all plugins
     */
    @GetMapping(produces = RdfMediaType.TEXT_TURTLE_VALUE)
    public ResponseEntity<String> listPlugins() {
        log.info("Listing all plugins");

        try {
            String rdfData = metadataStoreService.getResourceRdf("pl", null);
            return ResponseEntity.ok(rdfData);
        } catch (Exception e) {
            log.error("Error listing plugins", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
