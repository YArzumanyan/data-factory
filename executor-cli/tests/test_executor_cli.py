import sys
import importlib.util
import pytest

# Dynamically import the executor_cli module from the specified file path
module_name = "executor_cli"
file_path = "executor-cli.py"

spec = importlib.util.spec_from_file_location(module_name, file_path)
executor_cli = importlib.util.module_from_spec(spec)
sys.modules[module_name] = executor_cli
spec.loader.exec_module(executor_cli)

# Import necessary classes and functions from the executor_cli module
CombinedWorkflowBuilder = executor_cli.CombinedWorkflowBuilder
get_uuid_from_iri = executor_cli.get_uuid_from_iri
app = executor_cli.app

# Pytest fixture to provide sample RDF data for tests
@pytest.fixture
def sample_ttl_data():
    """Provides a sample RDF graph in Turtle format for a simple pipeline."""
    return """
@prefix dcat: <http://www.w3.org/ns/dcat#> .
@prefix dcterms: <http://purl.org/dc/terms/> .
@prefix df: <http://localhost:8080/ns/df#> .
@prefix ds: <http://localhost:8080/ns/ds#> .
@prefix p-plan: <http://purl.org/net/p-plan#> .
@prefix pipe: <http://localhost:8080/ns/pipe#> .
@prefix pl: <http://localhost:8080/ns/pl#> .
@prefix prov: <http://www.w3.org/ns/prov#> .
@prefix step: <http://localhost:8080/ns/step#> .
@prefix var: <http://localhost:8080/ns/var#> .

# Pipeline Definition
pipe:pipeline-uuid a p-plan:Plan ;
    dcterms:title "Sample Pipeline" .

# Step Definition
step:step-uuid a p-plan:Step ;
    dcterms:title "Sample Step" ;
    p-plan:isStepOfPlan pipe:pipeline-uuid ;
    df:usesPlugin pl:plugin-uuid ;
    p-plan:hasInputVar var:input-var-uuid ;
    p-plan:isOutputVarOf var:output-var-uuid .

# Variable Definitions
var:input-var-uuid a p-plan:Variable ;
    dcterms:title "Input Data" ;
    p-plan:isVariableOfPlan pipe:pipeline-uuid ;
    prov:specializationOf ds:dataset-uuid .

var:output-var-uuid a p-plan:Variable ;
    dcterms:title "Output Data" ;
    p-plan:isVariableOfPlan pipe:pipeline-uuid .

# Dependent Resource Definitions
ds:dataset-uuid a dcat:Dataset ;
    dcterms:title "Source Dataset" ;
    dcat:distribution [
        a dcat:Distribution ;
        dcat:accessURL <http://artifact-repo/api/v1/objects/artifact1>
    ] .

pl:plugin-uuid a df:Plugin ;
    dcterms:title "Sample Plugin" ;
    dcat:distribution [
        a dcat:Distribution ;
        dcat:accessURL <http://artifact-repo/api/v1/objects/plugin-artifact>
    ] .
"""

def test_get_uuid_from_iri():
    """Tests the helper function for extracting UUIDs from IRIs."""
    assert get_uuid_from_iri("http://example.com/ns/ds#my-uuid-123") == "my-uuid-123"
    assert get_uuid_from_iri("http://example.com/ns/pl/another-uuid-456") == "another-uuid-456"


def test_combined_workflow_builder(mocker, sample_ttl_data):
    """
    Tests that the CombinedWorkflowBuilder correctly constructs a graph
    from mocked RDF data.
    """
    mock_get = mocker.patch("requests.get")
    mock_response = mocker.Mock()
    mock_response.status_code = 200
    mock_response.text = sample_ttl_data
    mock_get.return_value = mock_response

    builder = CombinedWorkflowBuilder(api_base_url="http://fake.me")
    graph = builder.build_graph(start_pipeline_uuid="pipeline-uuid")

    assert graph is not None
    assert graph.number_of_nodes() == 5  # Step, 2 Variables, Plugin, Dataset
    assert graph.number_of_edges() == 4 

    assert "http://localhost:8080/ns/var#input-var-uuid" in graph
    assert "http://localhost:8080/ns/pl#plugin-uuid" in graph
    assert graph.nodes["http://localhost:8080/ns/step#step-uuid"]["label"] == "Sample Step"
    assert graph.nodes["http://localhost:8080/ns/var#output-var-uuid"]["label"] == "Output Data"
    assert graph.nodes["http://localhost:8080/ns/var#input-var-uuid"]["type"] == "Variable"
    assert graph.nodes["http://localhost:8080/ns/pl#plugin-uuid"]["accessURLs"] == ["http://artifact-repo/api/v1/objects/plugin-artifact"]