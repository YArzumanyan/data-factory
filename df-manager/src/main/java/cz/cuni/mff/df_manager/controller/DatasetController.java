package cz.cuni.mff.df_manager.controller;

import cz.cuni.mff.df_manager.service.ArtifactRepositoryService;
import cz.cuni.mff.df_manager.service.MetadataStoreService;
import cz.cuni.mff.df_manager.service.RdfService;
import cz.cuni.mff.df_manager.utils.RdfMediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    private ResponseEntity<String> createDataset(String title, String description, List<MultipartFile> files) {
        // Upload the files to artifact repository
        List<String> artifactIds = artifactRepositoryService.uploadArtifacts(files);
        log.info("Artifacts uploaded with IDs: {}", artifactIds);

        // Generate RDF for dataset with multiple distributions
        String rdfData = rdfService.generateDatasetRdf(title, description, artifactIds);
        log.info("Generated RDF for multi-file dataset: {}", rdfData);

        // Submit RDF to metadata store
        String response = metadataStoreService.submitRdf("ds", rdfData, null, HttpMethod.POST);

        log.info("Multi-file dataset RDF stored successfully, response: {}", response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Uploads dataset files and creates metadata for them.
     *
     * @param files The list of dataset files
     * @param title The title of the dataset
     * @param description The description of the dataset
     * @return RDF data for the created dataset
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = RdfMediaType.TEXT_TURTLE_VALUE)
    public ResponseEntity<String> uploadDataset(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description) {

        log.info("Uploading dataset: {}, file count: {}", title, files.size());

        try {
            return createDataset(title, description, files);
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
    public ResponseEntity<String> getDataset(@PathVariable String uuid) {
        log.info("Retrieving dataset: {}", uuid);

        try {
            String rdfData = metadataStoreService.getResourceRdf("ds", uuid);
            return ResponseEntity.ok(rdfData);
        } catch (Exception e) {
            log.error("Error retrieving dataset", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Lists all datasets as an RDF graph.
     *
     * @return RDF data containing descriptions of all datasets
     */
    @GetMapping(produces = RdfMediaType.TEXT_TURTLE_VALUE)
    public ResponseEntity<String> listDatasets() {
        log.info("Listing all datasets");

        try {
            String rdfData = metadataStoreService.getResourceRdf("ds", null);
            return ResponseEntity.ok(rdfData);
        } catch (Exception e) {
            log.error("Error listing datasets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<String> updateDatasetDist(String uuid, List<MultipartFile> files) {
        // Validate that the dataset exists
        if (!metadataStoreService.resourceExists("ds", uuid)) {
            log.error("Dataset not found: {}", uuid);
            return ResponseEntity.notFound().build();
        }

        // Upload the files to the artifact repository
        List<String> artifactIds;
        try {
            artifactIds = artifactRepositoryService.uploadArtifacts(files);
            log.info("Artifacts uploaded with IDs: {}", artifactIds);
        } catch (Exception e) {
            log.error("Error uploading dataset files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Update the dataset's distributions
        String rdfData;
        try {
            rdfData = rdfService.updateDatasetDistributions(uuid, artifactIds);
            log.info("Updated RDF for dataset: {}", rdfData);
        } catch (Exception e) {
            log.error("Error updating dataset distributions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Submit updated RDF to metadata store
        String response;
        try {
            response = metadataStoreService.submitRdf("ds", rdfData, uuid, HttpMethod.PUT);
            log.info("Dataset distributions updated successfully, response: {}", response);
        } catch (Exception e) {
            log.error("Error submitting updated RDF to metadata store", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates the distributions of an existing dataset with files.
     *
     * @param uuid The UUID of the dataset
     * @param files The list of files for the new distributions
     * @return RDF data for the updated dataset
     */
    @PostMapping(value = "/{uuid}/distribution", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = RdfMediaType.TEXT_TURTLE_VALUE)
    public ResponseEntity<String> updateDatasetDistributions(
            @PathVariable String uuid,
            @RequestParam("files") List<MultipartFile> files) {

        log.info("Updating distributions for dataset: {}, file count: {}", uuid, files.size());

        try {
            return updateDatasetDist(uuid, files);
        } catch (Exception e) {
            log.error("Error updating dataset distributions", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
