package cz.cuni.mff.df_manager.model.pipeline;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a complete pipeline configuration.
 * Maps to p-plan:Plan in RDF.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PipelineConfig {
    /**
     * Human-readable title for the pipeline.
     */
    @NotBlank(message = "Pipeline title is required")
    private String title;

    /**
     * Optional longer description of the pipeline's purpose.
     */
    private String description;

    /**
     * List of logical variables representing data flow within the pipeline scope.
     */
    @NotEmpty(message = "At least one variable is required")
    @Valid
    private List<Variable> variables;

    /**
     * List of processing steps in the pipeline.
     */
    @NotEmpty(message = "At least one step is required")
    @Valid
    private List<Step> steps;
}
