package cz.cuni.mff.metadata_store.service;

import cz.cuni.mff.metadata_store.utils.Vocab;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb2.TDB2Factory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the RdfStorageServiceImpl class.
 * These tests use an in-memory Jena TDB2 dataset to ensure isolation and speed.
 */
class RdfStorageServiceImplTest {

    private Dataset dataset;
    private RdfStorageService rdfStorageService;
    private UriService uriService;

    /**
     * Sets up an in-memory TDB2 dataset and initializes the services before each test.
     */
    @BeforeEach
    void setUp() {
        dataset = TDB2Factory.createDataset();
        uriService = new UriService();
        rdfStorageService = new RdfStorageServiceImpl(dataset, uriService);
    }

    /**
     * Closes the dataset resources after each test.
     */
    @AfterEach
    void tearDown() {
        if (dataset != null) {
            dataset.close();
        }
    }

    private Model createTestDatasetModel(String uuid) {
        Model model = ModelFactory.createDefaultModel();
        String datasetUri = uriService.buildDatasetUri(uuid);
        Resource datasetResource = model.createResource(datasetUri);
        model.add(datasetResource, Vocab.type, Vocab.Dataset);
        model.add(datasetResource, model.createProperty(Vocab.DCTERMS_NS, "title"), "Test Dataset");
        return model;
    }

    @Test
    void storeRdfGraph_Success() {
        String uuid = UUID.randomUUID().toString();
        Model testModel = createTestDatasetModel(uuid);

        String storedUri = rdfStorageService.storeRdfGraph(testModel, Vocab.Dataset);

        dataset.executeRead(() -> {
            assertEquals(uriService.buildDatasetUri(uuid), storedUri);
            assertFalse(dataset.getDefaultModel().isEmpty(), "Dataset should not be empty after storing a graph.");
            assertTrue(dataset.getDefaultModel().containsAll(testModel), "The stored model should contain the triples from the test model.");
        });
    }

    @Test
    void storeRdfGraph_ThrowsException_WhenTypeIsMissing() {
        Model wrongTypeModel = ModelFactory.createDefaultModel();
        wrongTypeModel.createResource(uriService.buildDatasetUri(UUID.randomUUID().toString()))
                .addProperty(wrongTypeModel.createProperty(Vocab.DCTERMS_NS, "title"), "Wrong Type");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            rdfStorageService.storeRdfGraph(wrongTypeModel, Vocab.Dataset);
        });

        assertTrue(exception.getMessage().contains("does not contain a resource of type"), "Exception message should indicate the missing type.");
    }

    @Test
    void getDatasetDescription_Success() {
        String uuid = UUID.randomUUID().toString();
        Model testModel = createTestDatasetModel(uuid);
        rdfStorageService.storeRdfGraph(testModel, Vocab.Dataset);

        Model fetchedModel = rdfStorageService.getDatasetDescription(uuid);

        assertNotNull(fetchedModel);
        assertFalse(fetchedModel.isEmpty());
        assertTrue(fetchedModel.containsAll(testModel));
    }

    @Test
    void getGenericResourceDescription_Success() {
        String uuid = UUID.randomUUID().toString();
        Model testModel = createTestDatasetModel(uuid);
        rdfStorageService.storeRdfGraph(testModel, Vocab.Dataset);

        Optional<Model> result = rdfStorageService.getGenericResourceDescription(uuid);

        assertTrue(result.isPresent());
        assertFalse(result.get().isEmpty());
        assertTrue(result.get().containsAll(testModel));
    }

    @Test
    void getGenericResourceDescription_NotFound() {
        String nonExistentUuid = UUID.randomUUID().toString();

        Optional<Model> result = rdfStorageService.getGenericResourceDescription(nonExistentUuid);

        assertTrue(result.isEmpty());
    }

    @Test
    void listResources_Success() {
        String uuid1 = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();
        rdfStorageService.storeRdfGraph(createTestDatasetModel(uuid1), Vocab.Dataset);
        rdfStorageService.storeRdfGraph(createTestDatasetModel(uuid2), Vocab.Dataset);

        Model resultModel = rdfStorageService.listResources(Vocab.Dataset);

        dataset.executeRead(() -> {
            assertFalse(resultModel.isEmpty());
            Resource res1 = resultModel.getResource(uriService.buildDatasetUri(uuid1));
            Resource res2 = resultModel.getResource(uriService.buildDatasetUri(uuid2));

            assertTrue(resultModel.containsResource(res1));
            assertTrue(resultModel.containsResource(res2));
            assertEquals(2, resultModel.listSubjectsWithProperty(Vocab.type, Vocab.Dataset).toList().size());
        });
    }
}
