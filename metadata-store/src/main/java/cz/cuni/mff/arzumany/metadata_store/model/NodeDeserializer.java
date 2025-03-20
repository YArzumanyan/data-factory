package cz.cuni.mff.arzumany.metadata_store.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.ArrayList;

public class NodeDeserializer extends StdDeserializer<Node> {
    private static final ArrayList<String> NODE_TYPES_SCRIPT = new ArrayList<String>() {{
        add("script-file");
        add("script-reference");
    }};

    private static final ArrayList<String> NODE_TYPES_DATASET = new ArrayList<String>() {{
        add("dataset-file");
        add("dataset-derived");
        add("dataset-reference");
    }};

    private static final ArrayList<String> NODE_TYPES = new ArrayList<String>() {{
        addAll(NODE_TYPES_SCRIPT);
        addAll(NODE_TYPES_DATASET);
    }};

    public NodeDeserializer() {
        this(null);
    }

    public NodeDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Node deserialize(JsonParser jp, DeserializationContext ctx)
            throws IOException, JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        JsonNode root = mapper.readTree(jp);

        JsonNode typeNode = root.get("type");
        String resolvedType = null;
        if (typeNode != null && typeNode.isArray()) {
            for (JsonNode t : typeNode) {
                String tStr = t.asText();
                if (NODE_TYPES.contains(tStr)) {
                    resolvedType = tStr;
                    break;
                }
            }
        } else if (typeNode != null && typeNode.isTextual()){
            resolvedType = typeNode.asText();
        }

        if (resolvedType == null) {
            throw new JsonMappingException(jp, "Could not determine node type from 'type' field.");
        }

        Class<? extends Node> targetClass;
        if (NODE_TYPES_SCRIPT.contains(resolvedType)) {
            targetClass = DatasetNode.class;
        } else if (NODE_TYPES_DATASET.contains(resolvedType)) {
            targetClass = ScriptNode.class;
        } else {
            throw new JsonMappingException(jp, "Unknown node type: " + resolvedType);
        }

        return mapper.treeToValue(root, targetClass);
    }
}
