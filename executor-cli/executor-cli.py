import os
import json
import shutil
import logging
import requests
import typer
import subprocess
import networkx as nx
import matplotlib.pyplot as plt
from rdflib import Graph
from typing import Optional, List, Set, Dict
from urllib.parse import urlparse, urlunparse
from dotenv import load_dotenv

# --- Configuration ---
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
load_dotenv()
METADATA_STORE_BASE_URL = "http://localhost:8083/api/v1/pipelines"
ARTIFACT_REPOSITORY_URL = os.getenv("ARTIFACT_REPOSITORY_URL")
MAIN_WORKSPACE = "./tmp/orchestrator_workspace"

def map_media_type_to_format(media_type_uri: Optional[str]) -> tuple[str, str]:
    """Maps a IANA media type URI to a shutil format and file extension."""
    if not media_type_uri:
        return 'zip', '.zip' # Default to zip if not specified

    media_type = media_type_uri.split('/')[-1].lower()
    
    mapping = {
        "zip": ('zip', '.zip'),
        "gzip": ('gztar', '.tar.gz'),
        "x-gzip": ('gztar', '.tar.gz'),
        "x-tar": ('tar', '.tar'),
        "x-bzip2": ('bztar', '.tar.bz2'),
    }
    
    for key, value in mapping.items():
        if key in media_type:
            return value
            
    logging.warning("Unknown compressFormat '%s', defaulting to .zip", media_type_uri)
    return 'zip', '.zip'

def rewrite_url_base(original_url: str, new_base: str) -> str:
    """
    Replaces the base of a given URL with a new base.
    
    Args:
        original_url: The full original URL.
        new_base: The new base URL.
        
    Returns:
        The rewritten URL as a string.
    """
    original_parts = urlparse(original_url)
    new_base_parts = urlparse(new_base)
    
    rewritten_parts = [
        new_base_parts.scheme,
        new_base_parts.netloc,
        original_parts.path,
        original_parts.params,
        original_parts.query,
        original_parts.fragment
    ]
    
    return urlunparse(rewritten_parts)

# --- Graph Visualization Utility ---
def visualize_graph(graph: nx.DiGraph, title: str = "Combined Pipeline Workflow"):
    """Uses Matplotlib to draw the detailed workflow graph."""
    if not graph or graph.number_of_nodes() == 0:
        logging.warning("Graph is empty, skipping visualization.")
        return
    plt.figure(figsize=(20, 16))
    pos = nx.spring_layout(graph, seed=42, k=0.9, iterations=50)
    type_map = nx.get_node_attributes(graph, 'type')
    color_map = {"Step": "#80bfff", "Variable": "#90ee90", "Dataset": "#900090", "Plugin": "#ffb3ba"}
    node_colors = [color_map.get(type_map.get(node), "#cccccc") for node in graph.nodes()]
    labels = nx.get_node_attributes(graph, 'label')
    nx.draw(graph, pos, labels=labels, with_labels=True, node_size=3500,
            node_color=node_colors, font_size=9, font_weight='bold',
            arrowsize=20, width=1.5)
    edge_labels = nx.get_edge_attributes(graph, 'label')
    nx.draw_networkx_edge_labels(graph, pos, edge_labels=edge_labels, font_color='darkred', font_size=8)
    plt.title(title, size=22)
    handles = [plt.Line2D([0], [0], marker='o', color='w', markerfacecolor=color, markersize=10, label=type_name)
               for type_name, color in color_map.items()]
    plt.legend(handles=handles, title="Node Types", loc='upper left', fontsize=10)
    plt.savefig("combined_workflow.png")
    logging.info("Saved combined workflow graph to combined_workflow.png")
    plt.show()


# --- Graph Building Class ---
class CombinedWorkflowBuilder:
    """
    Builds a single, unified graph of a pipeline and all its recursive dependencies.
    """
    def __init__(self, api_base_url: str):
        self.api_base_url = api_base_url
        self.combined_graph = nx.DiGraph()
        self.processed_pipelines: Set[str] = set()

    def _fetch_pipeline_ttl(self, pipeline_uuid: str) -> Optional[str]:
        api_url = f"{self.api_base_url}/{pipeline_uuid}"
        logging.info(f"Fetching data from: {api_url}")
        try:
            response = requests.get(api_url, timeout=15)
            response.raise_for_status()
            return response.text
        except requests.exceptions.RequestException as e:
            logging.error(f"Failed to fetch pipeline {pipeline_uuid}: {e}")
            return None

    def _get_inter_pipeline_dependencies(self, rdf_graph: Graph) -> List[str]:
        query = """
            PREFIX prov: <http://www.w3.org/ns/prov#>
            SELECT DISTINCT ?dependent_pipeline_iri
            WHERE {
                ?var prov:specializationOf ?dataset .
                ?dataset prov:wasGeneratedBy ?dependent_pipeline_iri .
            }
        """
        results = rdf_graph.query(query)
        dependents = []
        for row in results:
            dep_iri = str(row.dependent_pipeline_iri)
            if '#' in dep_iri:
                dependents.append(dep_iri.rsplit('#', 1)[-1])
        return dependents

    def _add_pipeline_to_combined_graph(self, rdf_graph: Graph):
        """Parses an RDF graph and merges its components into the main graph."""
        queries: Dict[str, str] = {
            "nodes": """
                SELECT ?iri ?title ?type ?accessURL ?compressFormat
                WHERE {
                    { ?iri a/rdfs:subClassOf* <http://purl.org/net/p-plan#Step> . BIND("Step" AS ?type) }
                    UNION
                    { ?iri a/rdfs:subClassOf* <http://purl.org/net/p-plan#Variable> . BIND("Variable" AS ?type) }
                    UNION
                    { ?iri a <http://localhost:8080/ns/df#Plugin> . BIND("Plugin" AS ?type) }
                    UNION
                    { ?iri a <http://www.w3.org/ns/dcat#Dataset> . BIND("Dataset" AS ?type) }
                    ?iri <http://purl.org/dc/terms/title> ?title .
                    OPTIONAL {
                        ?iri <http://www.w3.org/ns/dcat#distribution> ?dist .
                        ?dist <http://www.w3.org/ns/dcat#accessURL> ?accessURL .
                        OPTIONAL { ?dist <http://www.w3.org/ns/dcat#compressFormat> ?compressFormat . }
                    }
                }
            """,
            "edges": """
                SELECT ?source ?target ?label
                WHERE {
                    { ?target <http://purl.org/net/p-plan#hasInputVar> ?source . BIND("input" AS ?label) }
                    UNION
                    { ?source <http://purl.org/net/p-plan#isOutputVarOf> ?target . BIND("output" AS ?label) }
                    UNION
                    { ?source <http://localhost:8080/ns/df#usesPlugin> ?target . BIND("uses" AS ?label) }
                    UNION
                    { ?target <http://purl.org/net/p-plan#isPrecededBy> ?source . BIND("precedes" AS ?label) }
                    UNION
                    { ?source <http://www.w3.org/ns/prov#specializationOf> ?target . BIND("is_instance_of" AS ?label) }
                }
            """
        }
        
        for row in rdf_graph.query(queries["nodes"]):
            self.combined_graph.add_node(
                str(row.iri),
                label=str(row.title),
                type=str(row.type),
                accessURL=str(row.accessURL) if row.accessURL else None,
                compressFormat=str(row.compressFormat) if row.compressFormat else None
            )

        for row in rdf_graph.query(queries["edges"]):
            self.combined_graph.add_edge(str(row.source), str(row.target), label=str(row.label))

    def build_graph(self, start_pipeline_uuid: str) -> Optional[nx.DiGraph]:
        pipelines_to_process = [start_pipeline_uuid]
        while pipelines_to_process:
            current_uuid = pipelines_to_process.pop(0)
            if current_uuid in self.processed_pipelines: continue
            logging.info(f"Processing pipeline: {current_uuid}")
            ttl_data = self._fetch_pipeline_ttl(current_uuid)
            if not ttl_data:
                logging.warning(f"Could not retrieve data for {current_uuid}. Skipping.")
                self.processed_pipelines.add(current_uuid)
                continue
            try:
                rdf_graph = Graph().parse(data=ttl_data, format="turtle")
            except Exception as e:
                logging.error(f"Failed to parse TTL for {current_uuid}: {e}")
                self.processed_pipelines.add(current_uuid)
                continue
            self._add_pipeline_to_combined_graph(rdf_graph)
            dependencies = self._get_inter_pipeline_dependencies(rdf_graph)
            if dependencies:
                logging.info(f"Found dependencies for {current_uuid}: {dependencies}")
                for dep_uuid in dependencies:
                    if dep_uuid not in self.processed_pipelines:
                        pipelines_to_process.append(dep_uuid)
            self.processed_pipelines.add(current_uuid)
        logging.info("Finished processing all pipelines.")
        return self.combined_graph


# --- Orchestration Class ---
class Orchestrator:
    """Executes a pipeline workflow based on a provided graph."""
    def __init__(self, graph: nx.DiGraph):
        self.graph = graph
        self.workspace = MAIN_WORKSPACE
        self.results_map = {}

    def _setup_workspace(self):
        if os.path.exists(self.workspace):
            shutil.rmtree(self.workspace)
        os.makedirs(self.workspace)
        logging.info("Main workspace created at: %s", self.workspace)

    def _fetch_artifact(self, node_iri: str) -> str:
        """Downloads an artifact and saves it with the correct extension."""
        node_data = self.graph.nodes[node_iri]
        access_url = node_data.get('accessURL')
        if not access_url:
            raise ValueError(f"Cannot fetch artifact for {node_iri}: missing accessURL.")
        
        if ARTIFACT_REPOSITORY_URL:
            access_url = rewrite_url_base(access_url, ARTIFACT_REPOSITORY_URL)
        
        # Determine filename and extension from graph data
        _, file_ext = map_media_type_to_format(node_data.get('compressFormat'))
        local_filename = f"{node_data['label'].replace(' ', '_')}{file_ext}"
        local_path = os.path.join(self.workspace, "artifact_cache", local_filename)
        os.makedirs(os.path.dirname(local_path), exist_ok=True)
        
        logging.info("Downloading artifact from %s to %s", access_url, local_path)
        try:
            with requests.get(access_url, stream=True, timeout=30) as r:
                r.raise_for_status()
                with open(local_path, 'wb') as f:
                    for chunk in r.iter_content(chunk_size=8192):
                        f.write(chunk)
            return local_path
        except requests.exceptions.RequestException as e:
            logging.error("Failed to download artifact: %s", e)
            raise

    def _execute_step(self, step_iri: str):
        step_label = self.graph.nodes[step_iri]['label']
        logging.info("--- Executing Step: %s ---", step_label)

        step_workspace = os.path.join(self.workspace, step_label.replace(' ', '_'))
        inputs_dir = os.path.join(step_workspace, "inputs")
        outputs_dir = os.path.join(step_workspace, "outputs")
        plugin_dir = os.path.join(step_workspace, "plugin")
        os.makedirs(inputs_dir)
        os.makedirs(outputs_dir)
        os.makedirs(plugin_dir)

        for pred_iri in self.graph.predecessors(step_iri):
            if self.graph.nodes[pred_iri]['type'] == 'Variable':
                archive_path = self.results_map.get(pred_iri)
                if not archive_path:
                    raise ValueError(f"Orchestration error: Could not find result for input variable {pred_iri}")
                shutil.copy(archive_path, inputs_dir)
                logging.info("Staged input from %s", archive_path)

        plugin_iri = next(s for s in self.graph.successors(step_iri) if self.graph.nodes[s]['type'] == 'Plugin')
        plugin_archive_path = self._fetch_artifact(plugin_iri)
        
        shutil_format, _ = map_media_type_to_format(self.graph.nodes[plugin_iri].get('compressFormat'))
        logging.info("Unpacking plugin with format: %s", shutil_format)
        shutil.unpack_archive(plugin_archive_path, plugin_dir, format=shutil_format)
        
        with open(os.path.join(plugin_dir, "config.json")) as f:
            plugin_config = json.load(f)
        
        image_tag = f"plugin-{self.graph.nodes[plugin_iri]['label'].lower().replace(' ', '-')}"
        
        try:
            logging.info("Building Docker image: %s", image_tag)
            subprocess.run(["docker", "build", "-t", image_tag, "."], cwd=plugin_dir, check=True, capture_output=True)
            
            logging.info("Running container...")
            result = subprocess.run([
                "docker", "run", "--rm",
                "-v", f"{os.path.abspath(inputs_dir)}:{plugin_config['input_directory']}",
                "-v", f"{os.path.abspath(outputs_dir)}:{plugin_config['output_directory']}",
                image_tag
            ], check=True, capture_output=True)
            logging.info("Container finished successfully. STDOUT: %s", result.stdout.decode())

        except subprocess.CalledProcessError as e:
            logging.error("Docker execution failed! Exit Code: %s", e.returncode)
            logging.error("STDERR: %s", e.stderr.decode())
            raise
        
        output_var_iri = next(s for s in self.graph.successors(step_iri) if self.graph.nodes[s]['type'] == 'Variable')
        output_archive_path = None
        
        items_in_output = os.listdir(outputs_dir)
        # Check if the plugin produced a single archive as its output
        if len(items_in_output) == 1:
            single_item_name = items_in_output[0]
            if single_item_name.lower().endswith(('.zip', '.tar', '.gz', '.tar.gz', '.bz2')):
                logging.info("Plugin produced a single archive. Using it directly.")
                source_path = os.path.join(outputs_dir, single_item_name)
                final_path = os.path.join(self.workspace, single_item_name)
                shutil.move(source_path, final_path)
                output_archive_path = final_path

        # If the output was not a single archive, create one now.
        if output_archive_path is None:
            logging.info("Plugin produced multiple files or non-archive output. Archiving contents.")
            output_title = self.graph.nodes[output_var_iri]['label'].replace(' ', '_')
            output_archive_path = shutil.make_archive(os.path.join(self.workspace, output_title), 'zip', outputs_dir)

        self.results_map[output_var_iri] = output_archive_path
        logging.info("Step successful. Output for '%s' is at: %s", self.graph.nodes[output_var_iri]['label'], output_archive_path)

    def run(self):
        try:
            self._setup_workspace()
            for node_iri, data in self.graph.nodes(data=True):
                if data.get('type') == 'Dataset' and data.get('accessURL'):
                    dataset_path = self._fetch_artifact(node_iri)
                    var_iri = next(p for p in self.graph.predecessors(node_iri))
                    self.results_map[var_iri] = dataset_path
            
            execution_order = list(nx.topological_sort(self.graph))
            for node_iri in execution_order:
                if self.graph.nodes[node_iri].get('type') == 'Step':
                    self._execute_step(node_iri)
            
            logging.info("--- Orchestration Finished Successfully! ---")
            final_vars = [n for n in execution_order if self.graph.nodes[n].get('type') == 'Variable' and self.graph.out_degree(n) == 0]
            for var in final_vars:
                 logging.info("Final output for '%s' is available at: %s", self.graph.nodes[var]['label'], self.results_map.get(var))

        except Exception as e:
            logging.error("--- Orchestration Failed ---")
            logging.error("Error: %s", e, exc_info=False)
            raise

# --- Main Execution Block (using Typer) ---
app = typer.Typer(
    help="A command-line tool to build, visualize, and execute semantic pipeline workflows.",
    add_completion=False
)

@app.command()
def run_pipeline(
    start_uuid: str = typer.Argument(
        "66f87504-48e7-4ef5-8ff9-340ec44b5ec6",
        help="The starting pipeline UUID to process."
    ),
    visualize_only: bool = typer.Option(
        False,
        "--visualize-only",
        help="Only build and visualize the graph, do not execute the pipeline."
    )
):
    """
    Builds and executes a complex pipeline workflow starting from a given UUID.
    """
    if not visualize_only and shutil.which("docker") is None:
        logging.error("Docker is not installed or not in the system's PATH. Cannot execute pipeline.")
        raise typer.Exit(code=1)

    typer.echo("--- Building Combined Workflow Graph for All Dependent Pipelines ---")
    builder = CombinedWorkflowBuilder(api_base_url=METADATA_STORE_BASE_URL)
    final_graph = builder.build_graph(start_uuid)

    if not final_graph or final_graph.number_of_nodes() == 0:
        logging.critical("Failed to build a graph. Please check the starting UUID and API endpoint.")
        raise typer.Exit(code=1)

    logging.info(f"Successfully built combined graph with {final_graph.number_of_nodes()} nodes and {final_graph.number_of_edges()} edges.")
    
    if visualize_only:
        typer.echo("Visualizing graph as requested...")
        visualize_graph(final_graph)
    else:
        typer.echo("\n--- Starting Pipeline Orchestration ---")
        try:
            orchestrator = Orchestrator(final_graph)
            orchestrator.run()
        except Exception:
            # The orchestrator logs the detailed error, here we just confirm failure.
            typer.secho("Execution failed. Check logs for details.", fg=typer.colors.RED, bold=True)
            raise typer.Exit(code=1)


if __name__ == "__main__":
    app()