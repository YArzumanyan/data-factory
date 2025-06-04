package cz.cuni.mff.df_manager.model.pipeline;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a processing step in a pipeline configuration.
 * Maps to p-plan:Step in RDF.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Step {
    /**
     * Unique logical identifier for this step within the pipeline configuration.
     */
    @NotBlank(message = "Step ID is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Step ID must contain only alphanumeric characters, underscores, and hyphens")
    private String id;

    /**
     * Human-readable title for this step.
     */
    @NotBlank(message = "Step title is required")
    private String title;

    /**
     * UUID of the registered plugin (df:Plugin) used by this step.
     */
    @NotBlank(message = "Plugin UUID is required")
    @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", 
             message = "Plugin UUID must be a valid UUID")
    private String pluginUuid;

    /**
     * List of logical variable IDs that are inputs to this step.
     */
    private List<String> inputs;

    /**
     * List of logical variable IDs that are outputs of this step.
     */
    private List<String> outputs;

    /**
     * Optional list of logical step IDs that must complete before this step starts.
     */
    private List<String> precededBy;
}
