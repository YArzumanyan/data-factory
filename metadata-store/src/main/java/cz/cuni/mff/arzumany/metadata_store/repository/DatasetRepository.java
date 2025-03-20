package cz.cuni.mff.arzumany.metadata_store.repository;

import cz.cuni.mff.arzumany.metadata_store.model.DatasetNode;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DatasetRepository extends MongoRepository<DatasetNode, String> {
    // For derived datasets, find all datasets linked to a specific sub-pipeline URL
    List<DatasetNode> findByPipeline(String pipeline);
}
