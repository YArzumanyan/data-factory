package cz.cuni.mff.df_manager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model containing RDF data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RdfResponse {
    /**
     * RDF data in Turtle format.
     */
    private String rdfData;
}