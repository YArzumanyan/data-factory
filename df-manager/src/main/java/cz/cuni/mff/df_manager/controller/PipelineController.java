package cz.cuni.mff.df_manager.controller;

import cz.cuni.mff.df_manager.model.RdfResponse;
import cz.cuni.mff.df_manager.model.pipeline.PipelineConfig;
import cz.cuni.mff.df_manager.service.MetadataStoreService;
import cz.cuni.mff.df_manager.service.RdfService;
import cz.cuni.mff.df_manager.utils.RdfMediaType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for pipeline operations.
 */
@RestController
@RequestMapping("/api/v1/pipelines")
@RequiredArgsConstructor
@Slf4j
public class PipelineController {

    private final MetadataStoreService metadataStoreService;
    private final RdfService rdfService;

    /**
     * Creates a new pipeline from a configuration.
     *
     * @param pipelineConfig The pipeline configuration
     * @return RDF data for the created pipeline
     */
    @PostMapping(produces = RdfMediaType.TEXT_TURTLE_VALUE)
    public ResponseEntity<String> createPipeline(@Valid @RequestBody PipelineConfig pipelineConfig) {
        log.info("Creating pipeline: {}", pipelineConfig.getTitle());

        try {
            // Generate RDF for pipeline
            String rdfData = rdfService.generatePipelineRdf(pipelineConfig);

            // Submit RDF to metadata store
            String response = metadataStoreService.submitRdf("pipe", rdfData);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            // This is for validation errors (e.g., referenced dataset/plugin doesn't exist)
            log.error("Validation error creating pipeline", e);
            return ResponseEntity.unprocessableEntity().build();
        } catch (Exception e) {
            log.error("Error creating pipeline", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Handles validation errors for the request body.
     *
     * @param ex The validation exception
     * @return A map of field errors
     */
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }

    /**
     * Retrieves metadata for a pipeline.
     *
     * @param uuid The UUID of the pipeline
     * @return RDF data for the pipeline
     */
    @GetMapping(value = "/{uuid}")
    public ResponseEntity<String> getPipeline(@PathVariable String uuid) {
        log.info("Retrieving pipeline: {}", uuid);

        try {
            String rdfData = metadataStoreService.getResourceRdf("pipe", uuid);
            return ResponseEntity.ok(rdfData);
        } catch (Exception e) {
            log.error("Error retrieving pipeline", e);
            return ResponseEntity.notFound().build();
        }
    }
}
