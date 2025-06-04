package cz.cuni.mff.df_manager.controller;

import cz.cuni.mff.df_manager.model.RdfResponse;
import cz.cuni.mff.df_manager.service.ArtifactRepositoryService;
import cz.cuni.mff.df_manager.service.MetadataStoreService;
import cz.cuni.mff.df_manager.service.RdfService;
import cz.cuni.mff.df_manager.utils.RdfMediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<RdfResponse> uploadPlugin(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description) {

        log.info("Uploading plugin: {}", title);

        try {
            // Upload file to artifact repository
            String artifactId = artifactRepositoryService.uploadArtifact(file);
            log.info("Artifact uploaded with ID: {}", artifactId);

            // Determine file extension
            String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);

            // Generate RDF for plugin
            String rdfData = rdfService.generatePluginRdf(title, description, artifactId, fileExtension);

            // Submit RDF to metadata store
            metadataStoreService.submitRdf("pl", rdfData);

            return ResponseEntity.ok(new RdfResponse(rdfData));
        } catch (Exception e) {
            log.error("Error uploading plugin", e);
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
    public ResponseEntity<RdfResponse> getPlugin(@PathVariable String uuid) {
        log.info("Retrieving plugin: {}", uuid);

        try {
            String rdfData = metadataStoreService.getResourceRdf("pl", uuid);
            return ResponseEntity.ok(new RdfResponse(rdfData));
        } catch (Exception e) {
            log.error("Error retrieving plugin", e);
            return ResponseEntity.notFound().build();
        }
    }
}
