package cz.cuni.mff.arzumany.metadata_store.controller;

import cz.cuni.mff.arzumany.metadata_store.model.ScriptNode;
import cz.cuni.mff.arzumany.metadata_store.service.ScriptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/script")
public class ScriptController {

    @Autowired
    private ScriptService scriptService;

    @GetMapping
    public ResponseEntity<List<ScriptNode>> getAllScripts() {
        List<ScriptNode> scripts = scriptService.getAllScripts();
        return new ResponseEntity<>(scripts, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScriptNode> getScriptById(@PathVariable String id) {
        ScriptNode script = scriptService.getScriptById(id);
        return new ResponseEntity<>(script, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ScriptNode> createScript(@RequestBody ScriptNode script) {
        ScriptNode created = scriptService.createScript(script);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScriptNode> updateScript(@PathVariable String id, @RequestBody ScriptNode script) {
        ScriptNode updated = scriptService.updateScript(id, script);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScript(@PathVariable String id) {
        scriptService.deleteScript(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
