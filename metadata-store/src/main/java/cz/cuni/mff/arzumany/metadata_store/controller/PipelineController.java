package cz.cuni.mff.arzumany.metadata_store.controller;

import cz.cuni.mff.arzumany.metadata_store.model.Pipeline;
import cz.cuni.mff.arzumany.metadata_store.service.PipelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pipeline")
public class PipelineController {

    @Autowired
    private PipelineService pipelineService;

    @GetMapping
    public ResponseEntity<List<Pipeline>> getAllPipelines() {
        List<Pipeline> pipelines = pipelineService.getAllPipelines();
        return new ResponseEntity<>(pipelines, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pipeline> getPipelineById(@PathVariable String id) {
        Pipeline pipeline = pipelineService.getPipelineById(id);
        return new ResponseEntity<>(pipeline, HttpStatus.OK);
    }

//    @GetMapping("/{url}")
//    public ResponseEntity<Pipeline> getPipelineByUrl(@PathVariable String url) {
//        Pipeline pipeline = pipelineService.getPipelineById(url);
//        return new ResponseEntity<>(pipeline, HttpStatus.OK);
//    }

    @PostMapping
    public ResponseEntity<Pipeline> createPipeline(@RequestBody Pipeline pipeline) {
        Pipeline created = pipelineService.createPipeline(pipeline);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pipeline> updatePipeline(@PathVariable String id, @RequestBody Pipeline pipeline) {
        Pipeline updated = pipelineService.updatePipeline(id, pipeline);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePipeline(@PathVariable String id) {
        pipelineService.deletePipeline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
