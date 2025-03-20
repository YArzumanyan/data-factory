package cz.cuni.mff.arzumany.metadata_store.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import java.util.List;

@Data
@JsonDeserialize(using = NodeDeserializer.class)
public abstract class Node {
    private String id;
    private String label;
    private List<String> dependsOn;
    private String previousVersion;
}
