# Testing Guidelines

This document provides guidelines for testing the Metadata Store application.

## Testing Strategy

The Metadata Store application follows a comprehensive testing strategy that includes:

1. **Unit Tests**: Testing individual components in isolation
2. **Integration Tests**: Testing the interaction between components
3. **API Tests**: Testing the REST API endpoints
4. **End-to-End Tests**: Testing the complete application flow

## Test Structure

Tests are organized in the `src/test/java` directory, mirroring the structure of the main source code:

```
src/test/java/
└── cz/cuni/mff/metadata_store/
    ├── controller/
    │   ├── DatasetControllerTest.java
    │   ├── PipelineControllerTest.java
    │   ├── PluginControllerTest.java
    │   ├── ResourceControllerTest.java
    │   └── StoreControllerTest.java
    ├── service/
    │   ├── RdfStorageServiceImplTest.java
    │   └── UriServiceTest.java
    └── utils/
        ├── RdfMediaTypeTest.java
        └── VocabTest.java
```

## Writing Unit Tests

### Guidelines for Unit Tests

1. **Test One Thing**: Each test method should test one specific behavior or scenario
2. **Arrange-Act-Assert**: Structure tests with clear setup, action, and verification phases
3. **Descriptive Names**: Use descriptive method names that explain what is being tested
4. **Independent Tests**: Tests should not depend on each other or external state
5. **Mock Dependencies**: Use mocking frameworks to isolate the component being tested

### Example Unit Test

Here's an example unit test for the `UriService` class:

```java
@ExtendWith(MockitoExtension.class)
public class UriServiceTest {

    private static final String BASE_URI = "http://localhost:8080/rdfstore/";
    
    @InjectMocks
    private UriService uriService;
    
    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(uriService, "baseUri", BASE_URI);
    }
    
    @Test
    public void testCreateResourceUri() {
        // Arrange
        String resourceType = "datasets";
        String resourceId = "test-dataset";
        String expectedUri = BASE_URI + "datasets/test-dataset";
        
        // Act
        String actualUri = uriService.createResourceUri(resourceType, resourceId);
        
        // Assert
        assertEquals(expectedUri, actualUri, "The created URI should match the expected format");
    }
    
    @Test
    public void testExtractResourceId() {
        // Arrange
        String uri = BASE_URI + "datasets/test-dataset";
        String expectedId = "test-dataset";
        
        // Act
        String actualId = uriService.extractResourceId(uri);
        
        // Assert
        assertEquals(expectedId, actualId, "The extracted ID should match the expected value");
    }
    
    @Test
    public void testExtractResourceIdWithInvalidUri() {
        // Arrange
        String uri = "http://example.com/invalid";
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            uriService.extractResourceId(uri);
        }, "Should throw IllegalArgumentException for invalid URI");
    }
}
```

## Writing Integration Tests

### Guidelines for Integration Tests

1. **Test Component Interactions**: Focus on how components work together
2. **Use Real Dependencies**: Use real implementations of dependencies when possible
3. **Test Configuration**: Test that Spring configuration is correct
4. **Transaction Management**: Be careful with database transactions
5. **Clean Up**: Clean up any test data after tests

### Example Integration Test

Here's an example integration test for the `RdfStorageServiceImpl` class:

```java
@SpringBootTest
public class RdfStorageServiceIntegrationTest {

    @Autowired
    private RdfStorageService rdfStorageService;
    
    @Autowired
    private Dataset dataset;
    
    private Model testModel;
    private String testResourceUri;
    
    @BeforeEach
    public void setup() {
        // Create a test model with a dataset
        testModel = ModelFactory.createDefaultModel();
        Resource datasetResource = testModel.createResource("http://localhost:8080/rdfstore/datasets/test-integration");
        datasetResource.addProperty(RDF.type, DCAT.Dataset);
        datasetResource.addProperty(DCTerms.title, "Test Dataset");
        datasetResource.addProperty(DCTerms.description, "A test dataset for integration testing");
        
        // Store the test model
        testResourceUri = rdfStorageService.storeRdfGraph(testModel, DCAT.Dataset);
    }
    
    @AfterEach
    public void cleanup() {
        // Remove test data
        dataset.executeWrite(() -> {
            Model defaultModel = dataset.getDefaultModel();
            Resource resource = defaultModel.createResource(testResourceUri);
            defaultModel.removeAll(resource, null, null);
        });
    }
    
    @Test
    public void testGetDatasetDescription() throws ResourceNotFoundException {
        // Act
        String resourceId = testResourceUri.substring(testResourceUri.lastIndexOf('/') + 1);
        Model retrievedModel = rdfStorageService.getDatasetDescription(resourceId);
        
        // Assert
        assertFalse(retrievedModel.isEmpty(), "Retrieved model should not be empty");
        
        Resource datasetResource = retrievedModel.getResource(testResourceUri);
        assertTrue(retrievedModel.contains(datasetResource, RDF.type, DCAT.Dataset),
                "Retrieved model should contain the dataset type");
        
        String title = retrievedModel.getProperty(datasetResource, DCTerms.title).getString();
        assertEquals("Test Dataset", title, "Retrieved title should match the original");
    }
}
```

## Writing API Tests

### Guidelines for API Tests

1. **Test HTTP Endpoints**: Test the REST API endpoints directly
2. **Test Content Negotiation**: Test different content types and accept headers
3. **Test Error Handling**: Test error responses and status codes
4. **Test Request Validation**: Test validation of request parameters and bodies
5. **Use MockMvc**: Use Spring's MockMvc for testing controllers

### Example API Test

Here's an example API test for the `DatasetController` class:

```java
@WebMvcTest(DatasetController.class)
public class DatasetControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private RdfStorageService rdfStorageService;
    
    @Test
    public void testGetDataset() throws Exception {
        // Arrange
        String datasetId = "test-dataset";
        Model datasetModel = ModelFactory.createDefaultModel();
        Resource datasetResource = datasetModel.createResource("http://localhost:8080/rdfstore/datasets/test-dataset");
        datasetResource.addProperty(RDF.type, DCAT.Dataset);
        datasetResource.addProperty(DCTerms.title, "Test Dataset");
        
        when(rdfStorageService.getDatasetDescription(datasetId)).thenReturn(datasetModel);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/datasets/{datasetId}", datasetId)
                .header(HttpHeaders.ACCEPT, RdfMediaType.TEXT_TURTLE_VALUE))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, RdfMediaType.TEXT_TURTLE_VALUE))
                .andExpect(content().string(containsString("Test Dataset")));
    }
    
    @Test
    public void testGetDatasetNotFound() throws Exception {
        // Arrange
        String datasetId = "non-existent";
        
        when(rdfStorageService.getDatasetDescription(datasetId))
                .thenThrow(new ResourceNotFoundException("Dataset not found"));
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/datasets/{datasetId}", datasetId)
                .header(HttpHeaders.ACCEPT, RdfMediaType.TEXT_TURTLE_VALUE))
                .andExpect(status().isNotFound());
    }
    
    @Test
    public void testCreateDataset() throws Exception {
        // Arrange
        String turtleData = "@prefix dcat: <http://www.w3.org/ns/dcat#> .\n" +
                "@prefix dcterms: <http://purl.org/dc/terms/> .\n" +
                "<http://localhost:8080/rdfstore/datasets/new-dataset> a dcat:Dataset ;\n" +
                "    dcterms:title \"New Dataset\" ;\n" +
                "    dcterms:description \"A new dataset for testing\" .";
        
        String expectedUri = "http://localhost:8080/rdfstore/datasets/new-dataset";
        
        when(rdfStorageService.storeRdfGraph(any(Model.class), eq(DCAT.Dataset)))
                .thenReturn(expectedUri);
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/datasets")
                .contentType(RdfMediaType.TEXT_TURTLE_VALUE)
                .content(turtleData)
                .header(HttpHeaders.ACCEPT, RdfMediaType.TEXT_TURTLE_VALUE))
                .andExpect(status().isCreated())
                .andExpect(content().string(expectedUri));
    }
}
```

## Test Data Management

### Creating Test Data

For tests that require RDF data, you can create test data in several ways:

1. **Programmatically**: Create models and resources using the Jena API
2. **From Files**: Load RDF data from test resource files
3. **Using Builders**: Create test data builder classes for common entities

### Example Test Data Builder

```java
public class TestDataBuilder {

    public static Model createTestDataset(String id, String title, String description) {
        Model model = ModelFactory.createDefaultModel();
        String uri = "http://localhost:8080/rdfstore/datasets/" + id;
        
        Resource dataset = model.createResource(uri)
                .addProperty(RDF.type, DCAT.Dataset)
                .addProperty(DCTerms.title, title)
                .addProperty(DCTerms.description, description);
        
        return model;
    }
    
    public static Model createTestPipeline(String id, String title, String datasetId) {
        Model model = ModelFactory.createDefaultModel();
        String uri = "http://localhost:8080/rdfstore/pipelines/" + id;
        String datasetUri = "http://localhost:8080/rdfstore/datasets/" + datasetId;
        
        Resource pipeline = model.createResource(uri)
                .addProperty(RDF.type, Vocab.Plan)
                .addProperty(DCTerms.title, title)
                .addProperty(Vocab.hasInput, model.createResource(datasetUri));
        
        return model;
    }
}
```

## Test Coverage

Aim for high test coverage, especially for critical components:

1. **Controllers**: Test all endpoints, parameters, and response types
2. **Services**: Test all business logic and edge cases
3. **Utilities**: Test helper methods and utility functions

Use a code coverage tool like JaCoCo to measure and report on test coverage.

## Running Tests

### Running All Tests

```bash
mvn test
```

### Running Specific Tests

```bash
# Run a specific test class
mvn test -Dtest=RdfStorageServiceImplTest

# Run a specific test method
mvn test -Dtest=RdfStorageServiceImplTest#testGetDatasetDescription
```

### Running Tests with Coverage

```bash
mvn verify
```

This will generate a coverage report in `target/site/jacoco/index.html`.

## Continuous Integration

Set up continuous integration to run tests automatically:

1. Configure your CI system (e.g., Jenkins, GitHub Actions) to run tests on every commit
2. Set up quality gates based on test coverage and test results
3. Generate and publish test reports

## Best Practices

1. **Write Tests First**: Consider Test-Driven Development (TDD) for new features
2. **Keep Tests Simple**: Tests should be easy to understand and maintain
3. **Test Edge Cases**: Test boundary conditions and error scenarios
4. **Refactor Tests**: Refactor tests when they become complex or repetitive
5. **Don't Test Framework Code**: Focus on testing your application code, not the framework
6. **Use Appropriate Assertions**: Use specific assertions that provide meaningful error messages
7. **Isolate Test Data**: Each test should have its own isolated test data
8. **Clean Up After Tests**: Ensure tests clean up any resources they create
9. **Review Test Code**: Apply the same code review standards to test code as production code
10. **Keep Tests Fast**: Tests should run quickly to provide fast feedback