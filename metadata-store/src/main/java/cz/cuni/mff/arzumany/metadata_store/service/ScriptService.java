package cz.cuni.mff.arzumany.metadata_store.service;

import cz.cuni.mff.arzumany.metadata_store.exception.ResourceNotFoundException;
import cz.cuni.mff.arzumany.metadata_store.model.ScriptNode;
import cz.cuni.mff.arzumany.metadata_store.repository.ScriptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScriptService {

    @Autowired
    private ScriptRepository scriptRepository;

    public List<ScriptNode> getAllScripts() {
        return scriptRepository.findAll();
    }

    public ScriptNode getScriptById(String id) {
        return scriptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Script not found with id: " + id));
    }

    public ScriptNode createScript(ScriptNode scriptNode) {
        return scriptRepository.save(scriptNode);
    }

    public ScriptNode updateScript(String id, ScriptNode scriptDetails) {
        ScriptNode existingScript = getScriptById(id);
        existingScript.setLabel(scriptDetails.getLabel());
        existingScript.setDependsOn(scriptDetails.getDependsOn());
        existingScript.setPreviousVersion(scriptDetails.getPreviousVersion());
        existingScript.setDistribution(scriptDetails.getDistribution());
        existingScript.setArguments(scriptDetails.getArguments());
        return scriptRepository.save(existingScript);
    }

    public void deleteScript(String id) {
        ScriptNode existingScript = getScriptById(id);
        scriptRepository.delete(existingScript);
    }
}
