package cz.cuni.mff.df_manager.controller;

import cz.cuni.mff.df_manager.model.RdfResponse;
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
 * REST controller for dataset operations.
 */
@RestController
@RequestMapping("/api/v1/datasets")
@RequiredArgsConstructor
@Slf4j
public class DatasetController {

    private final ArtifactRepositoryService artifactRepositoryService;
    private final MetadataStoreService metadataStoreService;
    private final RdfService rdfService;

    /**
     * Uploads a dataset file and creates metadata for it.
     *
     * @param file The dataset file
     * @param title The title of the dataset
     * @param description The description of the dataset
     * @return RDF data for the created dataset
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = RdfMediaType.TEXT_TURTLE_VALUE)
    public ResponseEntity<String> uploadDataset(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description) {

        log.info("Uploading dataset: {}", title);

        try {
            // Upload the file to artifact repository
            String artifactId = artifactRepositoryService.uploadArtifact(file);
            log.info("Artifact uploaded with ID: {}", artifactId);

            // Generate RDF for dataset
            String rdfData = rdfService.generateDatasetRdf(title, description, artifactId);
            log.info("Generated RDF for dataset: {}", rdfData);

            // Submit RDF to metadata store
            String response = metadataStoreService.submitRdf("ds", rdfData);

            log.info("Dataset RDF stored successfully, response: {}", response);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error uploading dataset", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Retrieves metadata for a dataset.
     *
     * @param uuid The UUID of the dataset
     * @return RDF data for the dataset
     */
    @GetMapping(value = "/{uuid}", produces = RdfMediaType.TEXT_TURTLE_VALUE)
    public ResponseEntity<RdfResponse> getDataset(@PathVariable String uuid) {
        log.info("Retrieving dataset: {}", uuid);

        try {
            String rdfData = metadataStoreService.getResourceRdf("ds", uuid);
            return ResponseEntity.ok(new RdfResponse(rdfData));
        } catch (Exception e) {
            log.error("Error retrieving dataset", e);
            return ResponseEntity.notFound().build();
        }
    }
}
