package cz.cuni.mff.arzumany.metadata_store.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScriptNode extends Node {
    private Distribution distribution;
    private Map<String, Object> arguments;
}
