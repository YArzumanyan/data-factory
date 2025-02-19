package cz.cuni.mff.arzumany.metadata_store.repository;

import cz.cuni.mff.arzumany.metadata_store.model.PipelineVersion;
import cz.cuni.mff.arzumany.metadata_store.model.Pipeline_Id;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface PipelineRepository extends JpaRepository<PipelineVersion, Pipeline_Id> {
    @Query("SELECT pv FROM PipelineVersion pv WHERE pv.pipelineId = ?1 ORDER BY pv.version DESC")
    Optional<PipelineVersion> findTopByPipelineIdOrderByVersionDesc(String pipelineId);
}
