package cz.cuni.mff.arzumany.metadata_store.controller;

import cz.cuni.mff.arzumany.metadata_store.model.DatasetNode;
import cz.cuni.mff.arzumany.metadata_store.service.DatasetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dataset")
public class DatasetController {

    @Autowired
    private DatasetService datasetService;

    @GetMapping
    public ResponseEntity<List<DatasetNode>> getAllDatasets() {
        List<DatasetNode> datasets = datasetService.getAllDatasets();
        return new ResponseEntity<>(datasets, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DatasetNode> getDatasetById(@PathVariable String id) {
        DatasetNode dataset = datasetService.getDatasetById(id);
        return new ResponseEntity<>(dataset, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<DatasetNode> createDataset(@RequestBody DatasetNode dataset) {
        DatasetNode created = datasetService.createDataset(dataset);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DatasetNode> updateDataset(@PathVariable String id, @RequestBody DatasetNode dataset) {
        DatasetNode updated = datasetService.updateDataset(id, dataset);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDataset(@PathVariable String id) {
        datasetService.deleteDataset(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
