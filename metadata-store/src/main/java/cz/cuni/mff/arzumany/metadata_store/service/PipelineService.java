package cz.cuni.mff.arzumany.metadata_store.service;

import cz.cuni.mff.arzumany.metadata_store.exception.ResourceNotFoundException;
import cz.cuni.mff.arzumany.metadata_store.model.Pipeline;
import cz.cuni.mff.arzumany.metadata_store.repository.PipelineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PipelineService {

    @Autowired
    private PipelineRepository pipelineRepository;

    public List<Pipeline> getAllPipelines() {
        return pipelineRepository.findAll();
    }

    public Pipeline getPipelineById(String id) {
        return pipelineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found with id: " + id));
    }

    public Pipeline createPipeline(Pipeline pipeline) {
        return pipelineRepository.save(pipeline);
    }

    public Pipeline updatePipeline(String id, Pipeline pipelineDetails) {
        Pipeline existingPipeline = getPipelineById(id);
        existingPipeline.setType(pipelineDetails.getType());
        existingPipeline.setUrl(pipelineDetails.getUrl());
        existingPipeline.setNodes(pipelineDetails.getNodes());
        return pipelineRepository.save(existingPipeline);
    }

    public void deletePipeline(String id) {
        Pipeline existingPipeline = getPipelineById(id);
        pipelineRepository.delete(existingPipeline);
    }
}
