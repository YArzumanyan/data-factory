package cz.cuni.mff.arzumany.metadata_store.repository;

import cz.cuni.mff.arzumany.metadata_store.model.Pipeline;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface PipelineRepository extends MongoRepository<Pipeline, String> {
    // Find a pipeline by its unique URL
    Optional<Pipeline> findByUrl(String url);

    // Find pipelines that contain one or more of the given types
    List<Pipeline> findByTypeIn(List<String> types);
}
