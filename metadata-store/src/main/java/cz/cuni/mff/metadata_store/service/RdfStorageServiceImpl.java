package cz.cuni.mff.metadata_store.service;

import cz.cuni.mff.metadata_store.utils.Vocab;

import java.util.NoSuchElementException;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of RdfStorageService using Jena TDB2 Dataset.
 */
@Service
public class RdfStorageServiceImpl implements RdfStorageService {

    private static final Logger log = LoggerFactory.getLogger(RdfStorageServiceImpl.class);

    private final Dataset dataset;
    private final UriService uriService;

    @Autowired
    public RdfStorageServiceImpl(Dataset dataset, UriService uriService) {
        this.dataset = dataset;
        this.uriService = uriService;
    }

//    @PostConstruct
//    private void initialize() {
//        ensureRootContainerExists();
//    }
//
//    /**
//     * Ensures the LDP root container resource (Vocab.RootContainer) exists in the dataset.
//     */
//    private void ensureRootContainerExists() {
//        // Use the constant Resource from Vocab
//        Resource root = Vocab.RootContainer;
//        String rootUri = root.getURI(); // Get URI string for logging/comparison if needed
//
//        dataset.executeWrite(() -> {
//            Model model = dataset.getDefaultModel();
//            // Re-fetch resource within the transaction model context
//            Resource rootInModel = model.createResource(rootUri);
//            if (!model.contains(rootInModel, Vocab.type, Vocab.BasicContainer)) {
//                log.info("Creating LDP root container: {}", rootUri);
//                rootInModel.addProperty(Vocab.type, Vocab.BasicContainer);
//                // Use a label specific to the df:root identifier
//                rootInModel.addProperty(Vocab.title, model.createLiteral("Metadata Store Root (df:root)", "en"));
//            }
//        });
//    }

    @Override
    public String storeRdfGraph(Model rdfModel, Resource expectedResourceType) {
        if (rdfModel == null || rdfModel.isEmpty()) {
            throw new IllegalArgumentException("Input RDF model cannot be null or empty.");
        }

        List<Resource> potentialSubjects = rdfModel.listSubjectsWithProperty(Vocab.type, expectedResourceType).toList();

        if (potentialSubjects.isEmpty()) {
            throw new IllegalArgumentException("Input RDF model does not contain a resource of type: " + expectedResourceType.getURI());
        }

        Resource primaryResource = potentialSubjects.getFirst();
        if (!primaryResource.isURIResource()) {
            throw new IllegalArgumentException("Primary resource found is not a URI resource: " + primaryResource);
        }
        String primaryResourceUri = primaryResource.getURI();
        log.debug("Identified primary resource URI: {}", primaryResourceUri);


        dataset.executeWrite(() -> {
            log.info("Storing RDF graph for resource: {}", primaryResourceUri);
            Model defaultModel = dataset.getDefaultModel();
            defaultModel.add(rdfModel);

            if (expectedResourceType.equals(Vocab.Dataset) || expectedResourceType.equals(Vocab.Plugin)) {
                Resource rootInModel = defaultModel.getResource(Vocab.RootContainer.getURI());
                Resource primaryResInModel = defaultModel.getResource(primaryResourceUri);

                if (!defaultModel.contains(rootInModel, Vocab.contains, primaryResInModel)) {
                    log.debug("Adding ldp:contains triple for {} to root container {}", primaryResourceUri, Vocab.RootContainer.getURI());
                    rootInModel.addProperty(Vocab.contains, primaryResInModel);
                }
            }
        });

        log.info("Successfully stored RDF graph for: {}", primaryResourceUri);
        return primaryResourceUri;
    }

    private Model sparqlConstruct(String queryString) {
        log.debug("Executing SPARQL CONSTRUCT query: {}", queryString);
        final Model resultModel = ModelFactory.createDefaultModel();

        dataset.executeRead(() -> {
            try (QueryExecution qExec = QueryExecutionFactory.create(queryString, dataset)) {
                qExec.execConstruct(resultModel);
                resultModel.setNsPrefixes(dataset.getDefaultModel().getNsPrefixMap()); // Copy prefixes
            } catch (Exception e) {
                log.error("Error executing SPARQL CONSTRUCT query", e);
            }
        });

        log.debug("Constructed model contains {} triples", resultModel.size());
        return resultModel;
    }

    @Override
    public Optional<Model> getGenericResourceDescription(String resourceUuid) {
        log.debug("Searching for resource with UUID: {}", resourceUuid);

        String pipelineUri = uriService.buildPipelineUri(resourceUuid);
        String datasetUri = uriService.buildDatasetUri(resourceUuid);
        String pluginUri = uriService.buildPluginUri(resourceUuid);

        Model resultModel = ModelFactory.createDefaultModel();

        dataset.executeRead(() -> {
            Model defaultModel = dataset.getDefaultModel();
            Resource pipelineRes = defaultModel.getResource(pipelineUri);
            Resource datasetRes = defaultModel.getResource(datasetUri);
            Resource pluginRes = defaultModel.getResource(pluginUri);

            if (defaultModel.containsResource(pipelineRes)) {
                resultModel.add(describeResource(pipelineRes.getURI()));
                log.debug("Found resource as pipeline: {}", pipelineUri);
            } else if (defaultModel.containsResource(datasetRes)) {
                resultModel.add(describeResource(datasetRes.getURI()));
                log.debug("Found resource as dataset: {}", datasetUri);
            } else if (defaultModel.containsResource(pluginRes)) {
                resultModel.add(describeResource(pluginRes.getURI()));
                log.debug("Found resource as plugin: {}", pluginUri);
            }
        });

        if (resultModel.isEmpty()) {
            log.debug("Resource not found with known types, trying general UUID search");

            String queryString = """
                    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                    CONSTRUCT { ?s ?p ?o }
                    WHERE {
                      ?s ?p ?o .
                      FILTER(STRENDS(STR(?s), "%s"))
                    }
                    """.formatted(resourceUuid);

            dataset.executeRead(() -> {
                try (QueryExecution qExec = QueryExecutionFactory.create(queryString, dataset)) {
                    Model queryResult = qExec.execConstruct();
                    if (!queryResult.isEmpty()) {
                        resultModel.add(queryResult);
                        log.debug("Found resource with general UUID search: {}", resourceUuid);
                    }
                } catch (Exception e) {
                    log.error("Error executing general UUID search for {}", resourceUuid, e);
                }
            });
        }

        return resultModel.isEmpty() ? Optional.empty() : Optional.of(resultModel);
    }

    @Override
    public Model getPipelineDescription(String pipelineUuid) throws NoSuchElementException {
        String resourceUri = uriService.buildPipelineUri(pipelineUuid);
        return describeResourceOrThrow(resourceUri);
    }

    @Override
    public Model getPipelineDescriptionWithDependencies(String pipelineUuid) throws NoSuchElementException {
        String resourceUri = uriService.buildPipelineUri(pipelineUuid);
        Model pipelineModel = describeResourceOrThrow(resourceUri);

        String queryString = """
                PREFIX dcat:    <http://www.w3.org/ns/dcat#>
                PREFIX dcterms: <http://purl.org/dc/terms/>
                PREFIX df:      <http://localhost:8080/ns/df#>
                PREFIX ds:      <http://localhost:8080/ns/ds#>
                PREFIX p-plan:  <http://purl.org/net/p-plan#>
                PREFIX prov:    <http://www.w3.org/ns/prov#>
                PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>
                
                # This query combines several targeted CONSTRUCT queries to build a complete
                # graph of the pipeline and its specific, known dependency types.
                CONSTRUCT {
                  # Core pipeline properties
                  ?pipeline ?p ?o .
                  ?s ?p_inv ?pipeline .
                
                  # Steps, their properties, and their plugins/distributions
                  ?step ?step_p ?step_o .
                  ?plugin ?plugin_p ?plugin_o .
                  ?plugin_dist ?plugin_dist_p ?plugin_dist_o .
                
                  # Variables, their properties, and their specialized datasets/distributions
                  ?variable ?var_p ?var_o .
                  ?dataset ?dataset_p ?dataset_o .
                  ?dataset_dist ?dataset_dist_p ?dataset_dist_o .
                
                  # Datasets generated by the pipeline and their distributions
                  ?generated_dataset ?gds_p ?gds_o .
                  ?gds_dist ?gds_dist_p ?gds_dist_o .
                }
                WHERE {
                  # Bind the specific pipeline IRI from the Java code.
                  VALUES ?pipeline { <%s> }
                
                  {
                    # 1. Get all triples for the pipeline itself (outgoing and incoming).
                    { ?pipeline ?p ?o . }
                    UNION
                    { ?s ?p_inv ?pipeline . }
                  }
                  UNION
                  {
                    # 2. Get all steps in the plan, their properties, their plugins, and distributions.
                    # The step is the subject of the isStepOfPlan property.
                    ?step p-plan:isStepOfPlan ?pipeline ;
                          ?step_p ?step_o .
                    OPTIONAL {
                      ?step df:usesPlugin ?plugin .
                      ?plugin ?plugin_p ?plugin_o .
                      OPTIONAL {
                        ?plugin dcat:distribution ?plugin_dist .
                        ?plugin_dist ?plugin_dist_p ?plugin_dist_o .
                      }
                    }
                  }
                  UNION
                  {
                    # 3. Get all variables in the plan, their properties, their specialized datasets, and distributions.
                    # The variable is the subject of the isVariableOfPlan property.
                    ?variable p-plan:isVariableOfPlan ?pipeline ;
                              ?var_p ?var_o .
                    OPTIONAL {
                      ?variable prov:specializationOf ?dataset .
                      ?dataset ?dataset_p ?dataset_o .
                      OPTIONAL {
                        ?dataset dcat:distribution ?dataset_dist .
                        ?dataset_dist ?dataset_dist_p ?dataset_dist_o .
                      }
                    }
                  }
                  UNION
                  {
                    # 4. Get any datasets that were generated by this pipeline and their properties/distributions.
                    ?generated_dataset prov:wasGeneratedBy ?pipeline ;
                                       ?gds_p ?gds_o .
                    OPTIONAL {
                      ?generated_dataset dcat:distribution ?gds_dist .
                      ?gds_dist ?gds_dist_p ?gds_dist_o .
                    }
                  }
                }
                """.formatted(resourceUri);

        log.debug("Executing CONSTRUCT query to find dependencies for pipeline: {}", resourceUri);
        dataset.executeRead(() -> {
            try (QueryExecution qExec = QueryExecutionFactory.create(queryString, dataset)) {
                Model dependenciesModel = qExec.execConstruct();
                pipelineModel.add(dependenciesModel);
                log.debug("Found {} dependencies for pipeline: {}", dependenciesModel.size(), resourceUri);
            } catch (Exception e) {
                log.error("Error executing dependency query for pipeline {}", resourceUri, e);
            }
        });

        return pipelineModel;
    }

    @Override
    public Model getDatasetDescription(String datasetUuid) throws NoSuchElementException {
        String resourceUri = uriService.buildDatasetUri(datasetUuid);
        return describeResourceOrThrow(resourceUri);
    }

    @Override
    public Model getPluginDescription(String pluginUuid) throws NoSuchElementException {
        String resourceUri = uriService.buildPluginUri(pluginUuid);
        return describeResourceOrThrow(resourceUri);
    }

    // Helper for typed get methods
    private Model describeResourceOrThrow(String resourceUri) throws NoSuchElementException {
        final Model resourceModel = ModelFactory.createDefaultModel();
        dataset.executeRead(() -> {
            Model model = dataset.getDefaultModel();
            Resource resource = model.getResource(resourceUri);
            if (resource == null) {
                log.warn("Resource not found: {}", resourceUri);
                throw new NoSuchElementException("Resource with URI " + resourceUri + " not found.");
            }
            resourceModel.add(describeResource(resourceUri));
        });

        log.debug("Found {} triples describing resource: {}", resourceModel.size(), resourceUri);
        return resourceModel;
    }


    /**
     * Helper method to get the description of a resource using SPARQL CONSTRUCT.
     * Retrieves triples where the resource is the subject.
     * Includes outbound links but limited depth for blank nodes by default CONSTRUCT behavior.
     *
     * @param resourceUri The full URI of the resource.
     * @return A Model containing the resource description, empty if not found.
     */
    private Model describeResource(String resourceUri) {
        String queryString = """
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                CONSTRUCT {
                  ?s ?p ?o .
                  ?o ?p2 ?o2 .
                }
                WHERE {
                  BIND(<%s> AS ?s)
                  ?s ?p ?o .
                  OPTIONAL {
                    FILTER(ISBLANK(?o))
                    ?o ?p2 ?o2 .
                  }
                }
                """.formatted(resourceUri);

        return sparqlConstruct(queryString);
    }


    @Override
    public Model listResources(Resource resourceType) {
        String queryString = """
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX dcat: <http://www.w3.org/ns/dcat#>
                PREFIX df: <http://example.org/ns/df#>
                
                CONSTRUCT { ?s ?p ?o }
                WHERE {
                  ?s rdf:type <%s> .
                  ?s ?p ?o .
                }
                """.formatted(resourceType.getURI());

        return sparqlConstruct(queryString);
    }

    public Model listResourcesWithDistributions(Resource resourceType) {
        String queryString = """
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX dcat: <http://www.w3.org/ns/dcat#>
                PREFIX df: <http://example.org/ns/df#>
                
                CONSTRUCT { ?s ?p ?o . ?s dcat:distribution ?dist . ?dist ?dp ?do }
                WHERE {
                  ?s rdf:type <%s> .
                  ?s ?p ?o .
                  OPTIONAL {
                    ?s dcat:distribution ?dist .
                    ?dist ?dp ?do .
                  }
                }
                """.formatted(resourceType.getURI());

        return sparqlConstruct(queryString);
    }

    @Override
    public Model getEntireStoreModel() {
        log.info("Retrieving entire default graph from the store.");
        final Model storeModelCopy = ModelFactory.createDefaultModel();

        dataset.executeRead(() -> {
            Model defaultModel = dataset.getDefaultModel();
            storeModelCopy.setNsPrefixes(defaultModel.getNsPrefixMap());
            storeModelCopy.add(defaultModel);
        });

        log.info("Retrieved {} triples from the default graph.", storeModelCopy.size());
        return storeModelCopy;
    }

    @Override
    public String updateDataset(String datasetUuid, Model rdfData) throws NoSuchElementException {
        String resourceUri = uriService.buildDatasetUri(datasetUuid);
        log.info("Updating dataset with URI: {}", resourceUri);

        if (rdfData == null || rdfData.isEmpty()) {
            throw new IllegalArgumentException("Input RDF model cannot be null or empty.");
        }

        dataset.executeWrite(() -> {
            Model defaultModel = dataset.getDefaultModel();
            Resource resource = defaultModel.getResource(resourceUri);
            if (resource == null) {
                log.warn("Dataset not found for URI: {}", resourceUri);
                throw new NoSuchElementException("Dataset with URI " + resourceUri + " not found.");
            }

            // Clear existing triples for this resource
            defaultModel.removeAll(resource, null, null);

            // Add the new RDF data
            defaultModel.add(rdfData);
            log.info("Successfully updated dataset with URI: {}", resourceUri);
        });

        log.debug("Updated dataset with URI: {} with {} triples", resourceUri, rdfData.size());
        return resourceUri;
    }

    @Override
    public String updatePlugin(String pluginUuid, Model rdfData) throws NoSuchElementException {
        String resourceUri = uriService.buildPluginUri(pluginUuid);
        log.info("Updating plugin with URI: {}", resourceUri);

        if (rdfData == null || rdfData.isEmpty()) {
            throw new IllegalArgumentException("Input RDF model cannot be null or empty.");
        }

        dataset.executeWrite(() -> {
            Model defaultModel = dataset.getDefaultModel();
            Resource resource = defaultModel.getResource(resourceUri);
            if (resource == null) {
                log.warn("Plugin not found for URI: {}", resourceUri);
                throw new NoSuchElementException("Plugin with URI " + resourceUri + " not found.");
            }

            // Clear existing triples for this resource
            defaultModel.removeAll(resource, null, null);

            // Add the new RDF data
            defaultModel.add(rdfData);
            log.info("Successfully updated plugin with URI: {}", resourceUri);
        });

        log.debug("Updated plugin with URI: {} with {} triples", resourceUri, rdfData.size());
        return resourceUri;
    }
}
