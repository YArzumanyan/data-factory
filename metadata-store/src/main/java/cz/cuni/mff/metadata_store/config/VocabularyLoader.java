package cz.cuni.mff.metadata_store.config; // Or a suitable service/component package

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Component responsible for loading initial RDF vocabulary data into the
 * Jena Dataset after the Spring application context has been refreshed.
 */
@Component
public class VocabularyLoader implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(VocabularyLoader.class);
    private static final String VOCAB_PATH = "vocab/df.ttl";

    private final Dataset dataset;

    @Autowired
    public VocabularyLoader(Dataset dataset) {
        this.dataset = dataset;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Application context refreshed. Checking and loading vocabulary.");
        loadVocabulary();
    }

    private void loadVocabulary() {
        Resource vocabResource = new ClassPathResource(VOCAB_PATH);
        if (!vocabResource.exists()) {
            log.warn("Vocabulary file not found at classpath:{}, skipping loading.", VOCAB_PATH);
            return;
        }

        log.info("Attempting to load vocabulary from classpath:{}", VOCAB_PATH);
        dataset.executeWrite(() -> {
            try (InputStream vocabStream = vocabResource.getInputStream()) {
                // add
                Model defaultModel = dataset.getDefaultModel();
                if (defaultModel.isEmpty()) {
                    RDFDataMgr.read(defaultModel, vocabStream, RDFLanguages.TURTLE);
                    log.info("Successfully loaded vocabulary from {} into the default model.", VOCAB_PATH);
                } else {
                    log.info("Model is not empty, vocabulary seems to be already present in the default model, skipping load.");
                }
            } catch (IOException e) {
                log.error("Failed to read vocabulary file from classpath:{}", VOCAB_PATH, e);
            } catch (Exception e) {
                log.error("Error processing vocabulary file {}", VOCAB_PATH, e);
            }
        });
    }
}
