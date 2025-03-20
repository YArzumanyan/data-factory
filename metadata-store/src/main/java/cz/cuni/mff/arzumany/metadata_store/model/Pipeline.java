package cz.cuni.mff.arzumany.metadata_store.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.util.List;

@Data
@Document(collection = "pipelines")
public class Pipeline {
    @Id
    private String id;
    private String url;
    private List<String> type;
    private List<Node> nodes;
}
