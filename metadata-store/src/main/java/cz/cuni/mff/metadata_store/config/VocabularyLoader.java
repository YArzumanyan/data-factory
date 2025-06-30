package cz.cuni.mff.metadata_store.config;

import cz.cuni.mff.metadata_store.utils.Vocab;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Component responsible for programmatically generating and loading initial RDF vocabulary data 
 * into the Jena Dataset after the Spring application context has been refreshed.
 */
@Component
public class VocabularyLoader implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(VocabularyLoader.class);
    private final Dataset dataset;

    @Autowired
    public VocabularyLoader(Dataset dataset) {
        this.dataset = dataset;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Application context refreshed. Generating and loading vocabulary.");
        generateVocabulary();
    }

    private void generateVocabulary() {
        log.info("Generating vocabulary with namespace URI: {}", Vocab.DF_NS);

        dataset.executeWrite(() -> {
            Model defaultModel = dataset.getDefaultModel();

            if (defaultModel.isEmpty()) {
                // Create namespaces/prefixes
                String pPlanNs = "http://purl.org/net/p-plan#";
                String ldpNs = "http://www.w3.org/ns/ldp#";

                // Create resources
                Resource dfOntology = defaultModel.createResource(Vocab.DF_NS);
                Resource dfPlugin = defaultModel.createResource(Vocab.DF_NS + "Plugin");
                Property dfUsesPlugin = defaultModel.createProperty(Vocab.DF_NS + "usesPlugin");
                Resource dfRoot = defaultModel.createResource(Vocab.DF_NS + "root");

                // Define the ontology
                dfOntology.addProperty(RDF.type, OWL.Ontology)
                          .addProperty(RDFS.label, defaultModel.createLiteral("Custom Data Flow Vocabulary", "en"))
                          .addProperty(RDFS.comment, defaultModel.createLiteral("Defines concepts specific to the data processing flow, primarily Plugins.", "en"))
                          .addProperty(OWL.versionInfo, "0.1");

                // Define the Plugin class
                dfPlugin.addProperty(RDF.type, RDFS.Class)
                        .addProperty(RDFS.label, defaultModel.createLiteral("Plugin", "en"))
                        .addProperty(RDFS.comment, defaultModel.createLiteral("Represents a configurable and reusable software component (plugin) used within a data processing pipeline step.", "en"))
                        .addProperty(RDFS.subClassOf, DCAT.Resource)
                        .addProperty(RDFS.isDefinedBy, dfOntology);

                // Define the usesPlugin property
                dfUsesPlugin.addProperty(RDF.type, RDF.Property)
                           .addProperty(RDFS.label, defaultModel.createLiteral("uses plugin", "en"))
                           .addProperty(RDFS.comment, defaultModel.createLiteral("Relates a pipeline step to the specific plugin implementation it utilizes.", "en"))
                           .addProperty(RDFS.domain, defaultModel.createResource(pPlanNs + "Step"))
                           .addProperty(RDFS.range, dfPlugin)
                           .addProperty(RDFS.isDefinedBy, dfOntology);

                // Define the root resource
                dfRoot.addProperty(RDF.type, defaultModel.createResource(ldpNs + "BasicContainer"))
                      .addProperty(RDF.type, DCAT.Catalog)
                      .addProperty(DCTerms.title, defaultModel.createLiteral("Metadata Store Root (df:root)", "en"))
                      .addProperty(DCTerms.description, defaultModel.createLiteral("Root of the metadata store, containing all metadata related to data processing.", "en"));

                // Add prefixes to the model
                defaultModel.setNsPrefix("df", Vocab.DF_NS);
                defaultModel.setNsPrefix("dcat", DCAT.getURI());
                defaultModel.setNsPrefix("dcterms", DCTerms.getURI());
                defaultModel.setNsPrefix("owl", OWL.getURI());
                defaultModel.setNsPrefix("rdfs", RDFS.getURI());
                defaultModel.setNsPrefix("rdf", RDF.getURI());
                defaultModel.setNsPrefix("p-plan", pPlanNs);
                defaultModel.setNsPrefix("ldp", ldpNs);

                log.info("Successfully generated vocabulary with df URI: {}", Vocab.DF_NS);
            } else {
                log.info("Model is not empty, vocabulary seems to be already present in the default model, skipping generation.");
            }
        });
    }
}
