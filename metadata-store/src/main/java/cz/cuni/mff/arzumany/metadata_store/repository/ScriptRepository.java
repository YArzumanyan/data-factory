package cz.cuni.mff.arzumany.metadata_store.repository;

import cz.cuni.mff.arzumany.metadata_store.model.ScriptNode;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ScriptRepository extends MongoRepository<ScriptNode, String> {
    // find scripts by the distribution URL
    List<ScriptNode> findByDistributionUrl(String url);
}
