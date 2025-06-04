package cz.cuni.mff.df_manager.model.pipeline;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a variable in a pipeline configuration.
 * Maps to p-plan:Variable in RDF.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Variable {
    /**
     * Unique logical identifier for this variable within the pipeline configuration.
     */
    @NotBlank(message = "Variable ID is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Variable ID must contain only alphanumeric characters, underscores, and hyphens")
    private String id;

    /**
     * Human-readable title for this variable.
     */
    @NotBlank(message = "Variable title is required")
    private String title;

    /**
     * Optional UUID of a pre-existing dcat:Dataset this variable represents.
     * Typically used for initial inputs.
     */
    @Pattern(regexp = "^$|^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", 
             message = "Dataset UUID must be a valid UUID or empty")
    private String datasetUuid;
}
