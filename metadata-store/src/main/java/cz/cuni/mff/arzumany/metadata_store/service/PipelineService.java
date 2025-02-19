package cz.cuni.mff.arzumany.metadata_store.service;

import cz.cuni.mff.arzumany.metadata_store.model.PipelineVersion;
import cz.cuni.mff.arzumany.metadata_store.model.Pipeline_Id;
import cz.cuni.mff.arzumany.metadata_store.repository.PipelineRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PipelineService {
    private final PipelineRepository repository;

    public PipelineService(PipelineRepository repository) {
        this.repository = repository;
    }

    /**
     * Saves a new version for the given pipeline.
     *
     * @param pipelineId the pipeline identifier
     * @param payload    the JSON payload representing the pipeline configuration
     * @return the newly saved PipelineVersion
     */
    public PipelineVersion saveNewVersion(String pipelineId, String payload) {
        // Determine the new version number
        Optional<PipelineVersion> latestOpt = repository.findTopByPipelineIdOrderByVersionDesc(pipelineId);
        int newVersion = latestOpt.map(pv -> pv.getVersion() + 1).orElse(1);

        PipelineVersion newPipelineVersion = new PipelineVersion();
        newPipelineVersion.setPipelineId(pipelineId);
        newPipelineVersion.setVersion(newVersion);
        newPipelineVersion.setPayload(payload);
        newPipelineVersion.setCreatedAt(LocalDateTime.now());
        newPipelineVersion.setUpdatedAt(LocalDateTime.now());

        return repository.save(newPipelineVersion);
    }

    /**
     * Retrieves the latest version for the given pipeline.
     *
     * @param pipelineId the pipeline identifier
     * @return an Optional with the latest PipelineVersion if found
     */
    public Optional<PipelineVersion> getLatestVersion(String pipelineId) {
        return repository.findTopByPipelineIdOrderByVersionDesc(pipelineId);
    }

    /**
     * Retrieves a specific version for the given pipeline.
     *
     * @param pipelineId the pipeline identifier
     * @param version    the version number to retrieve
     * @return an Optional with the PipelineVersion if found
     */
    public Optional<PipelineVersion> getVersion(String pipelineId, int version) {
        return repository.findById(new Pipeline_Id(pipelineId, version));
    }

    public Object getAllPipelines() {
        return repository.findAll();
    }
}
