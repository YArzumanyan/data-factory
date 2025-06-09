package cz.cuni.mff.df_manager.service.impl;

import cz.cuni.mff.df_manager.model.pipeline.PipelineConfig;
import cz.cuni.mff.df_manager.model.pipeline.Step;
import cz.cuni.mff.df_manager.model.pipeline.Variable;
import cz.cuni.mff.df_manager.service.MetadataStoreService;
import cz.cuni.mff.df_manager.service.RdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the RdfService that uses Apache Jena to generate RDF data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RdfServiceImpl implements RdfService {

    private final MetadataStoreService metadataStoreService;

    @Value("${artifact-repository.download-endpoint}")
    private String downloadEndpointTemplate;

    @Value("${rdf.namespace.df}")
    private String dfNamespace;

    @Value("${rdf.namespace.pipe}")
    private String pipeNamespace;

    @Value("${rdf.namespace.step}")
    private String stepNamespace;

    @Value("${rdf.namespace.var}")
    private String varNamespace;

    @Value("${rdf.namespace.ds}")
    private String dsNamespace;

    @Value("${rdf.namespace.pl}")
    private String plNamespace;

    // RDF vocabulary namespaces
    private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private static final String DCTERMS_NS = "http://purl.org/dc/terms/";
    private static final String DCAT_NS = "http://www.w3.org/ns/dcat#";
    private static final String PPLAN_NS = "http://purl.org/net/p-plan#";
    private static final String PROV_NS = "http://www.w3.org/ns/prov#";

    /**
     * Adds common properties to a resource, such as title, description, and distribution.
     *
     * @param model The RDF model
     * @param resource The resource to which properties will be added
     * @param title The title of the resource
     * @param description The description of the resource
     * @param artifactId The artifact ID for the distribution access URL
     * @param fileExtension The file extension for determining MIME type
     */
    private void addCommonResourceProperties(Model model, Resource resource, String title, String description, String artifactId, String fileExtension) {
        // Add title and description
        if (title != null && !title.isEmpty()) {
            resource.addProperty(
                    model.createProperty(DCTERMS_NS + "title"),
                    title
            );
        }

        if (description != null && !description.isEmpty()) {
            resource.addProperty(
                    model.createProperty(DCTERMS_NS + "description"),
                    description
            );
        }

        // Create distribution
        Resource distribution = model.createResource();

        // Add a distribution type
        distribution.addProperty(
                model.createProperty(RDF_TYPE),
                model.createResource(DCAT_NS + "Distribution")
        );

        // Add access URL
        String downloadUrl = downloadEndpointTemplate.replace("{artifactId}", artifactId);
        distribution.addProperty(
                model.createProperty(DCAT_NS + "accessURL"),
                model.createResource(downloadUrl)
        );

        // Add a compress format
        String mimeType = determineMimeType(fileExtension);
        distribution.addProperty(
                model.createProperty(DCAT_NS + "compressFormat"),
                model.createResource("http://www.iana.org/assignments/media-types/" + mimeType)
        );

        // Link distribution to resource
        resource.addProperty(
                model.createProperty(DCAT_NS + "distribution"),
                distribution
        );
    }

    @Override
    public String generateDatasetRdf(String title, String description, String artifactId, String fileExtension) {
        // Create a new model
        Model model = ModelFactory.createDefaultModel();
        
        // Set up namespaces
        model.setNsPrefix("dcat", DCAT_NS);
        model.setNsPrefix("dcterms", DCTERMS_NS);
        model.setNsPrefix("ds", dsNamespace);

        String datasetUri = dsNamespace + artifactId;
        
        // Create the dataset resource
        Resource dataset = model.createResource(datasetUri);
        
        // Add a dataset type
        dataset.addProperty(
                model.createProperty(RDF_TYPE),
                model.createResource(DCAT_NS + "Dataset")
        );
        
        // Add title and description
        addCommonResourceProperties(model, dataset, title, description, artifactId, fileExtension);
        
        // Convert model to Turtle format
        java.io.StringWriter sw = new java.io.StringWriter();
        model.write(sw, "TURTLE");
        return sw.toString();
    }

    @Override
    public String generatePluginRdf(String title, String description, String artifactId, String fileExtension) {
        // Create a new model
        Model model = ModelFactory.createDefaultModel();
        
        // Set up namespaces
        model.setNsPrefix("dcat", DCAT_NS);
        model.setNsPrefix("dcterms", DCTERMS_NS);
        model.setNsPrefix("df", dfNamespace);
        model.setNsPrefix("pl", plNamespace);

        String pluginUri = plNamespace + artifactId;
        
        // Create the plugin resource
        Resource plugin = model.createResource(pluginUri);
        
        // Add plugin types
        plugin.addProperty(
                model.createProperty(RDF_TYPE),
                model.createResource(dfNamespace + "Plugin")
        );
        
//        plugin.addProperty(
//                model.createProperty(RDF_TYPE),
//                model.createResource(DCAT_NS + "Resource")
//        );
        
        // Add title and description
        addCommonResourceProperties(model, plugin, title, description, artifactId, fileExtension);

        // Convert model to Turtle format
        java.io.StringWriter sw = new java.io.StringWriter();
        model.write(sw, "TURTLE");
        return sw.toString();
    }

    @Override
    public String generatePipelineRdf(PipelineConfig pipelineConfig) {
        // Create a new model
        Model model = ModelFactory.createDefaultModel();
        
        // Set up namespaces
        model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        model.setNsPrefix("dcterms", DCTERMS_NS);
        model.setNsPrefix("p-plan", PPLAN_NS);
        model.setNsPrefix("dcat", DCAT_NS);
        model.setNsPrefix("prov", PROV_NS);
        model.setNsPrefix("df", dfNamespace);
        model.setNsPrefix("pipe", pipeNamespace);
        model.setNsPrefix("step", stepNamespace);
        model.setNsPrefix("var", varNamespace);
        model.setNsPrefix("ds", dsNamespace);
        model.setNsPrefix("pl", plNamespace);
        
        // Generate a UUID for the pipeline
        String pipelineUuid = UUID.randomUUID().toString();
        String pipelineUri = pipeNamespace + pipelineUuid;
        
        // Create the pipeline resource
        Resource pipeline = model.createResource(pipelineUri);
        
        // Add a pipeline type
        pipeline.addProperty(
                model.createProperty(RDF_TYPE),
                model.createResource(PPLAN_NS + "Plan")
        );
        
        // Add title and description
        if (pipelineConfig.getTitle() != null && !pipelineConfig.getTitle().isEmpty()) {
            pipeline.addProperty(
                    model.createProperty(DCTERMS_NS + "title"),
                    pipelineConfig.getTitle()
            );
        }
        
        if (pipelineConfig.getDescription() != null && !pipelineConfig.getDescription().isEmpty()) {
            pipeline.addProperty(
                    model.createProperty(DCTERMS_NS + "description"),
                    pipelineConfig.getDescription()
            );
        }
        
        // Process variables
        Map<String, String> variableIdMap = new HashMap<>();
        for (Variable variable : pipelineConfig.getVariables()) {
            String variableUuid = UUID.randomUUID().toString();
            String variableUri = varNamespace + variableUuid;
            variableIdMap.put(variable.getId(), variableUri);
            
            Resource variableResource = model.createResource(variableUri);
            
            // Add variable type
            variableResource.addProperty(
                    model.createProperty(RDF_TYPE),
                    model.createResource(PPLAN_NS + "Variable")
            );
            
            // Add title
            if (variable.getTitle() != null && !variable.getTitle().isEmpty()) {
                variableResource.addProperty(
                        model.createProperty(DCTERMS_NS + "title"),
                        variable.getTitle()
                );
            }
            
            // Link to pipeline
            variableResource.addProperty(
                    model.createProperty(PPLAN_NS + "isVariableOfPlan"),
                    pipeline
            );
            
            // Handle optional dataset link
            if (variable.getDatasetUuid() != null && !variable.getDatasetUuid().isEmpty()) {
                // Validate that the dataset exists
                if (!metadataStoreService.resourceExists("ds", variable.getDatasetUuid())) {
                    throw new IllegalArgumentException("Dataset with UUID " + variable.getDatasetUuid() + " does not exist");
                }
                
                String datasetUri = dsNamespace + variable.getDatasetUuid();
                variableResource.addProperty(
                        model.createProperty(PROV_NS + "specializationOf"),
                        model.createResource(datasetUri)
                );
            }
        }
        
        // Process steps (first pass)
        Map<String, String> stepIdMap = new HashMap<>();
        for (Step step : pipelineConfig.getSteps()) {
            String stepUuid = UUID.randomUUID().toString();
            String stepUri = stepNamespace + stepUuid;
            stepIdMap.put(step.getId(), stepUri);
            
            Resource stepResource = model.createResource(stepUri);
            
            // Add a step type
            stepResource.addProperty(
                    model.createProperty(RDF_TYPE),
                    model.createResource(PPLAN_NS + "Step")
            );
            
            // Add title
            if (step.getTitle() != null && !step.getTitle().isEmpty()) {
                stepResource.addProperty(
                        model.createProperty(DCTERMS_NS + "title"),
                        step.getTitle()
                );
            }
            
            // Link to pipeline
            stepResource.addProperty(
                    model.createProperty(PPLAN_NS + "isStepOfPlan"),
                    pipeline
            );
            
            // Link plugin and validate
            if (step.getPluginUuid() != null && !step.getPluginUuid().isEmpty()) {
                // Validate that the plugin exists
                if (!metadataStoreService.resourceExists("pl", step.getPluginUuid())) {
                    throw new IllegalArgumentException("Plugin with UUID " + step.getPluginUuid() + " does not exist");
                }
                
                String pluginUri = plNamespace + step.getPluginUuid();
                stepResource.addProperty(
                        model.createProperty(dfNamespace + "usesPlugin"),
                        model.createResource(pluginUri)
                );
            }
            
            // Link inputs
            if (step.getInputs() != null) {
                for (String inputId : step.getInputs()) {
                    String variableUri = variableIdMap.get(inputId);
                    if (variableUri != null) {
                        stepResource.addProperty(
                                model.createProperty(PPLAN_NS + "hasInputVar"),
                                model.createResource(variableUri)
                        );
                    }
                }
            }
            
            // Link outputs
            if (step.getOutputs() != null) {
                for (String outputId : step.getOutputs()) {
                    String variableUri = variableIdMap.get(outputId);
                    if (variableUri != null) {
                        stepResource.addProperty(
                                model.createProperty(PPLAN_NS + "isOutputVarOf"),
                                model.createResource(variableUri)
                        );
                    }
                }
            }
        }
        
        // Process steps (second pass - add dependencies)
        for (Step step : pipelineConfig.getSteps()) {
            if (step.getPrecededBy() != null && !step.getPrecededBy().isEmpty()) {
                String stepUri = stepIdMap.get(step.getId());
                Resource stepResource = model.getResource(stepUri);
                
                for (String precedingStepId : step.getPrecededBy()) {
                    String precedingStepUri = stepIdMap.get(precedingStepId);
                    if (precedingStepUri != null) {
                        stepResource.addProperty(
                                model.createProperty(PPLAN_NS + "isPrecededBy"),
                                model.createResource(precedingStepUri)
                        );
                    }
                }
            }
        }
        
        // Identify terminal variables and generate output datasets
        Set<String> consumedVariables = new HashSet<>();
        for (Step step : pipelineConfig.getSteps()) {
            if (step.getInputs() != null) {
                consumedVariables.addAll(step.getInputs());
            }
        }
        
        Set<String> producedVariables = new HashSet<>();
        for (Step step : pipelineConfig.getSteps()) {
            if (step.getOutputs() != null) {
                producedVariables.addAll(step.getOutputs());
            }
        }
        
        // Terminal variables are those that are produced but not consumed
        Set<String> terminalVariableIds = producedVariables.stream()
                .filter(id -> !consumedVariables.contains(id))
                .collect(Collectors.toSet());
        
        // Generate output datasets for terminal variables
        for (String terminalVariableId : terminalVariableIds) {
            String variableUri = variableIdMap.get(terminalVariableId);
            if (variableUri != null) {
                Resource variableResource = model.getResource(variableUri);
                
                // Get variable title
                String variableTitle = "";
                StmtIterator titleStmts = variableResource.listProperties(model.createProperty(DCTERMS_NS + "title"));
                if (titleStmts.hasNext()) {
                    variableTitle = titleStmts.nextStatement().getObject().toString();
                }
                
                // Generate output dataset
                String outputDatasetUuid = UUID.randomUUID().toString();
                String outputDatasetUri = dsNamespace + outputDatasetUuid;
                Resource outputDataset = model.createResource(outputDatasetUri);
                
                // Add a dataset type
                outputDataset.addProperty(
                        model.createProperty(RDF_TYPE),
                        model.createResource(DCAT_NS + "Dataset")
                );
                
                // Add title
                outputDataset.addProperty(
                        model.createProperty(DCTERMS_NS + "title"),
                        variableTitle + " (Output)"
                );
                
                // Link to variable and pipeline
                outputDataset.addProperty(
                        model.createProperty(PROV_NS + "wasDerivedFrom"),
                        variableResource
                );
                
                outputDataset.addProperty(
                        model.createProperty(PROV_NS + "wasGeneratedBy"),
                        pipeline
                );
            }
        }
        
        // Convert model to Turtle format
        StringWriter sw = new java.io.StringWriter();
        model.write(sw, "TURTLE");
        return sw.toString();
    }

    @Override
    public String determineCompressFormat(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename != null) {
            int lastDotIndex = filename.lastIndexOf('.');
            if (lastDotIndex > 0) {
                String extension = filename.substring(lastDotIndex + 1).toLowerCase();
                return determineMimeType(extension);
            }
        }
        return "application/octet-stream";
    }
    
    private String determineMimeType(String extension) {
        return switch (extension.toLowerCase()) {
            case "zip" -> "application/zip";
            case "tar" -> "application/x-tar";
            case "tar.gz", "tgz", "gz", "gzip" -> "application/gzip";
            case "tar.bz2", "tbz" -> "application/x-bzip2";
            case "7z" -> "application/x-7z-compressed";
            case "jar" -> "application/java-archive";
            default -> "application/octet-stream";
        };
    }
}