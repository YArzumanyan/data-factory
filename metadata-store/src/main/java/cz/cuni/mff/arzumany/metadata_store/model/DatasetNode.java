package cz.cuni.mff.arzumany.metadata_store.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DatasetNode extends Node {
    private String pipeline;
    private List<Binding> bindings;
}
