package cz.cuni.mff.arzumany.metadata_store.controller;

import cz.cuni.mff.arzumany.metadata_store.model.PipelineVersion;
import cz.cuni.mff.arzumany.metadata_store.service.PipelineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pipelines")
public class PipelineController {

    private final PipelineService service;

    public PipelineController(PipelineService service) {
        this.service = service;
    }

    /**
     * Retrieve all pipelines.
     * Endpoint: GET /pipelines
     */
    @GetMapping
    public ResponseEntity<?> getAllPipelines() {
        return ResponseEntity.ok(service.getAllPipelines());
    }

    /**
     * Create a new version for a given pipeline.
     * Endpoint: POST /pipelines/{pipelineId}
     */
    @PostMapping("/{pipelineId}")
    public ResponseEntity<?> createNewVersion(@PathVariable String pipelineId, @RequestBody String payload) {
        try {
            PipelineVersion savedVersion = service.saveNewVersion(pipelineId, payload);
            return ResponseEntity.ok(savedVersion);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating new version: " + e.getMessage());
        }
    }

    /**
     * Retrieve the latest version of a pipeline.
     * Endpoint: GET /pipelines/{pipelineId}
     */
    @GetMapping("/{pipelineId}")
    public ResponseEntity<?> getLatestVersion(@PathVariable String pipelineId) {
        return service.getLatestVersion(pipelineId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieve a specific version of a pipeline.
     * Endpoint: GET /pipelines/{pipelineId}/versions/{version}
     */
    @GetMapping("/{pipelineId}/versions/{version}")
    public ResponseEntity<?> getSpecificVersion(@PathVariable String pipelineId, @PathVariable int version) {
        return service.getVersion(pipelineId, version)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
