package cz.cuni.mff.arzumany.metadata_store.service;

import cz.cuni.mff.arzumany.metadata_store.exception.ResourceNotFoundException;
import cz.cuni.mff.arzumany.metadata_store.model.DatasetNode;
import cz.cuni.mff.arzumany.metadata_store.repository.DatasetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatasetService {

    @Autowired
    private DatasetRepository datasetRepository;

    public List<DatasetNode> getAllDatasets() {
        return datasetRepository.findAll();
    }

    public DatasetNode getDatasetById(String id) {
        return datasetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset not found with id: " + id));
    }

    public DatasetNode createDataset(DatasetNode datasetNode) {
        return datasetRepository.save(datasetNode);
    }

    public DatasetNode updateDataset(String id, DatasetNode datasetDetails) {
        DatasetNode existingDataset = getDatasetById(id);
        existingDataset.setLabel(datasetDetails.getLabel());
        existingDataset.setDependsOn(datasetDetails.getDependsOn());
        existingDataset.setPreviousVersion(datasetDetails.getPreviousVersion());
        existingDataset.setPipeline(datasetDetails.getPipeline());
        existingDataset.setBindings(datasetDetails.getBindings());
        return datasetRepository.save(existingDataset);
    }

    public void deleteDataset(String id) {
        DatasetNode existingDataset = getDatasetById(id);
        datasetRepository.delete(existingDataset);
    }
}
