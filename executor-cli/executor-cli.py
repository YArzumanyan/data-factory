import os
import sys
import json
import shutil
import logging
import requests
import typer
import subprocess
import networkx as nx
import matplotlib.pyplot as plt
import magic
from rdflib import Graph
from typing import Optional, List, Set, Dict
from urllib.parse import urlparse, urlunparse
from dotenv import load_dotenv
from abc import ABC, abstractmethod

# --- Configuration ---
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
load_dotenv()
METADATA_STORE_BASE_URL = "http://localhost:8083/api/v1/pipelines"
ARTIFACT_REPOSITORY_URL = os.getenv("ARTIFACT_REPOSITORY_URL")
MAIN_WORKSPACE = "./tmp/orchestrator_workspace"


# --- Helper Functions (Unchanged) ---
def get_uuid_from_iri(iri: str) -> str:
    """Extracts the last part of a URI, assuming it's the UUID."""
    if '#' in iri:
        return iri.rsplit('#', 1)[-1]
    return iri.rsplit('/', 1)[-1]

def rewrite_url_base(original_url: str, new_base: str) -> str:
    original_parts = urlparse(original_url)
    new_base_parts = urlparse(new_base)
    rewritten_parts = [
        new_base_parts.scheme, new_base_parts.netloc, original_parts.path,
        original_parts.params, original_parts.query, original_parts.fragment
    ]
    return urlunparse(rewritten_parts)


# --- Graph Visualization Utility (Unchanged) ---
def visualize_graph(graph: nx.DiGraph, title: str = "Combined Pipeline Workflow"):
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


# --- Graph Building Class (Unchanged) ---
class CombinedWorkflowBuilder:
    # ... (The entire class content remains here, unchanged)
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

    def _get_inter_pipeline_dependencies(self, rdf_graph: Graph) -> List[tuple[str, str]]:
        query = """
            PREFIX prov: <http://www.w3.org/ns/prov#>
            SELECT DISTINCT ?dataset_iri ?generating_pipeline_iri
            WHERE {
                ?var prov:specializationOf ?dataset_iri .
                ?dataset_iri prov:wasGeneratedBy ?generating_pipeline_iri .
            }
        """
        results = rdf_graph.query(query)
        dependents = []
        for row in results:
            dependents.append((str(row.dataset_iri), str(row.generating_pipeline_iri)))
        return dependents

    def _add_pipeline_to_combined_graph(self, rdf_graph: Graph):
        queries: Dict[str, str] = {
            "nodes": """
                SELECT ?iri ?title ?type ?accessURL
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
            urls = str(row["accessURL"]).split(',') if row["accessURL"] else []
            self.combined_graph.add_node(
                str(row.iri),
                label=str(row.title),
                type=str(row.type),
                accessURLs=urls
            )
        for row in rdf_graph.query(queries["edges"]):
            self.combined_graph.add_edge(str(row.source), str(row.target), label=str(row.label))

    def build_graph(self, start_pipeline_uuid: str, regenerate_uuids: List[str] = None) -> Optional[nx.DiGraph]:
        if regenerate_uuids is None: regenerate_uuids = []
        pipelines_to_process = [start_pipeline_uuid]
        while pipelines_to_process:
            current_uuid = pipelines_to_process.pop(0)
            if current_uuid in self.processed_pipelines: continue
            logging.info(f"Processing definition for pipeline: {current_uuid}")
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
                for dataset_iri, pipeline_iri in dependencies:
                    dataset_uuid = get_uuid_from_iri(dataset_iri)
                    pipeline_uuid = get_uuid_from_iri(pipeline_iri)
                    if dataset_uuid in regenerate_uuids:
                        logging.info(f"Dataset {dataset_uuid} marked for regeneration. Adding pipeline {pipeline_uuid} to queue.")
                        if pipeline_uuid not in self.processed_pipelines:
                            pipelines_to_process.append(pipeline_uuid)
                    else:
                        logging.info(f"Dataset {dataset_uuid} not marked for regeneration. Will use its distribution if available.")
            self.processed_pipelines.add(current_uuid)
        logging.info("Finished building execution graph.")
        return self.combined_graph


# --- NEW: Execution Strategy Pattern ---

class StepExecutor(ABC):
    """Interface defining all actions with side-effects."""
    @abstractmethod
    def setup_main_workspace(self, path: str): pass
    
    @abstractmethod
    def prepare_step_workspace(self, base_path: str, step_label: str) -> tuple[str, str, str]: pass

    @abstractmethod
    def stage_input(self, source_path: str, target_dir: str): pass

    @abstractmethod
    def fetch_file(self, access_url: str, target_dir: str) -> str: pass

    @abstractmethod
    def unpack_plugin(self, archive_path: str, target_dir: str): pass
    
    @abstractmethod
    def read_plugin_config(self, plugin_dir: str) -> dict: pass

    @abstractmethod
    def build_docker_image(self, image_tag: str, build_context_dir: str): pass

    @abstractmethod
    def run_docker_container(self, image_tag: str, inputs_dir: str, outputs_dir: str, config: dict): pass

    @abstractmethod
    def finalize_output(self, outputs_dir: str, persistent_dir: str, base_name: str) -> str: pass


class LiveExecutor(StepExecutor):
    """The 'real' executor that performs file operations and runs Docker."""
    def setup_main_workspace(self, path: str):
        if os.path.exists(path):
            shutil.rmtree(path)
        os.makedirs(path)
        logging.info("Main workspace created at: %s", path)

    def prepare_step_workspace(self, base_path: str, step_label: str) -> tuple[str, str, str]:
        step_workspace = os.path.join(base_path, step_label.replace(' ', '_'))
        inputs_dir = os.path.join(step_workspace, "inputs")
        outputs_dir = os.path.join(step_workspace, "outputs")
        plugin_dir = os.path.join(step_workspace, "plugin")
        os.makedirs(inputs_dir); os.makedirs(outputs_dir); os.makedirs(plugin_dir)
        return inputs_dir, outputs_dir, plugin_dir
    
    def stage_input(self, source_path: str, target_dir: str):
        if os.path.exists(source_path):
            shutil.copytree(source_path, target_dir)
            
    def fetch_file(self, access_url: str, target_dir: str) -> str:
        if not access_url:
            raise ValueError("Cannot fetch artifact with an empty accessURL.")
        
        if ARTIFACT_REPOSITORY_URL:
            access_url = rewrite_url_base(access_url, ARTIFACT_REPOSITORY_URL)
            
        if not os.path.exists(target_dir):
            os.makedirs(target_dir, exist_ok=True)
        
        local_path = os.path.join(target_dir, os.path.basename(access_url))
        logging.info("Downloading file from %s to %s", access_url, local_path)
        try:
            with requests.get(access_url, stream=True, timeout=30) as r:
                r.raise_for_status()
                with open(local_path, 'wb') as f:
                    for chunk in r.iter_content(chunk_size=8192):
                        f.write(chunk)
            return local_path
        except requests.exceptions.RequestException as e:
            logging.error("Failed to download file: %s", e)
            raise

    def read_plugin_config(self, plugin_dir: str) -> dict:
        with open(os.path.join(plugin_dir, "config.json")) as f:
            return json.load(f)

    def unpack_plugin(self, archive_path: str, target_dir: str):
        mime_type = magic.from_file(archive_path, mime=True)
        shutil_format = 'zip' # default
        if 'zip' in mime_type: shutil_format = 'zip'
        elif 'gzip' in mime_type: shutil_format = 'gztar'
        elif 'tar' in mime_type: shutil_format = 'tar'
        elif 'bzip2' in mime_type: shutil_format = 'bztar'
        else: raise TypeError(f"Plugin {archive_path} is not a recognized archive format.")

        logging.info("Unpacking plugin with detected format: %s", shutil_format)
        shutil.unpack_archive(archive_path, target_dir, format=shutil_format)

    def build_docker_image(self, image_tag: str, build_context_dir: str):
        logging.info("Building Docker image: %s", image_tag)
        subprocess.run(["docker", "build", "-t", image_tag, "."], cwd=build_context_dir, check=True, capture_output=True)

    def run_docker_container(self, image_tag: str, inputs_dir: str, outputs_dir: str, config: dict):
        logging.info("Running container...")
        subprocess.run([
            "docker", "run", "--rm",
            "-v", f"{os.path.abspath(inputs_dir)}:{config['input_directory']}",
            "-v", f"{os.path.abspath(outputs_dir)}:{config['output_directory']}",
            image_tag
        ], check=True, capture_output=True)

    def finalize_output(self, outputs_dir: str, persistent_dir: str, base_name: str) -> str:
        final_output_dir = os.path.join(persistent_dir, base_name)
        shutil.move(outputs_dir, final_output_dir)
        return final_output_dir


class DryRunExecutor(StepExecutor):
    """A 'dry run' executor that only prints actions"""
    
    def setup_main_workspace(self, path: str):
        typer.secho(f"[DRY RUN] Would set up main workspace at: {path}", fg=typer.colors.YELLOW)
        
    def prepare_step_workspace(self, base_path: str, step_label: str) -> tuple[str, str, str]:
        step_workspace = os.path.join(base_path, step_label.replace(' ', '_'))
        typer.secho(f"[DRY RUN] Would prepare workspace for step '{step_label}' at: {step_workspace}", fg=typer.colors.CYAN)
        # Return dummy paths for the orchestrator to use conceptually
        return (
            os.path.join(step_workspace, "inputs"),
            os.path.join(step_workspace, "outputs"),
            os.path.join(step_workspace, "plugin")
        )

    def stage_input(self, source_path: str, target_dir: str):
        typer.secho(f"[DRY RUN] Would stage input from {source_path} to {target_dir}", fg=typer.colors.CYAN)

    def fetch_file(self, access_url: str, target_dir: str) -> str:
        if ARTIFACT_REPOSITORY_URL:
            access_url = rewrite_url_base(access_url, ARTIFACT_REPOSITORY_URL)
        path = os.path.join(target_dir, os.path.basename(access_url))
        typer.secho(f"[DRY RUN] Would fetch artifact from {access_url} to {path}", fg=typer.colors.CYAN)
        return path 
    
    def read_plugin_config(self, plugin_dir: str) -> dict:
        typer.secho(f"[DRY RUN] Would read config.json from {plugin_dir}", fg=typer.colors.CYAN)
        # Return a dummy config so the orchestrator can continue
        return {"input_directory": "/dry_run/in", "output_directory": "/dry_run/out"}
    
    def unpack_plugin(self, archive_path: str, target_dir: str):
        typer.secho(f"[DRY RUN] Would unpack plugin {archive_path} to {target_dir}", fg=typer.colors.CYAN)

    def build_docker_image(self, image_tag: str, build_context_dir: str):
        cmd = ["docker", "build", "-t", image_tag, "."]
        typer.secho(f"[DRY RUN] In directory '{build_context_dir}', would execute command:", fg=typer.colors.CYAN)
        typer.echo(f"  {' '.join(cmd)}")

    def run_docker_container(self, image_tag: str, inputs_dir: str, outputs_dir: str, config: dict):
        cmd = [
            "docker", "run", "--rm",
            "-v", f"'{os.path.abspath(inputs_dir)}:{config['input_directory']}'",
            "-v", f"'{os.path.abspath(outputs_dir)}:{config['output_directory']}'",
            image_tag
        ]
        typer.secho(f"[DRY RUN] Would execute command:", fg=typer.colors.CYAN)
        typer.echo(f"  {' '.join(cmd)}")

    def finalize_output(self, outputs_dir: str, persistent_dir: str, base_name: str) -> str:
        final_path = os.path.join(persistent_dir, base_name)
        typer.secho(f"[DRY RUN] Would finalize output from {outputs_dir} into {final_path}", fg=typer.colors.CYAN)
        return final_path


# --- Orchestration Class (Refactored) ---
class Orchestrator:
    def __init__(self, graph: nx.DiGraph, executor: StepExecutor):
        self.graph = graph
        self.executor = executor
        self.workspace = MAIN_WORKSPACE
        self.results_map = {}

    def _execute_step(self, step_iri: str):
        step_label = self.graph.nodes[step_iri]['label']
        logging.info("--- Executing Step: %s ---", step_label)

        inputs_dir, outputs_dir, plugin_dir = self.executor.prepare_step_workspace(self.workspace, step_label)

        for pred_iri in self.graph.predecessors(step_iri):
            node_data = self.graph.nodes[pred_iri]
            if node_data['type'] == 'Variable':
                result_dir_path = self.results_map.get(pred_iri)
                if not result_dir_path:
                    raise ValueError(f"Orchestration error: Could not find result for input variable {pred_iri}")
                dest_dir = os.path.join(inputs_dir, node_data['label'].replace(' ', '_'))
                self.executor.stage_input(result_dir_path, dest_dir)
                logging.info("Staged input '%s' from %s", node_data['label'], result_dir_path)

        plugin_iri = next(s for s in self.graph.successors(step_iri) if self.graph.nodes[s]['type'] == 'Plugin')
        plugin_access_url = self.graph.nodes[plugin_iri]['accessURLs'][0]
        plugin_archive_path = self.executor.fetch_file(
            plugin_access_url, os.path.join(self.workspace, "artifact_cache")
        )
        self.executor.unpack_plugin(plugin_archive_path, plugin_dir)
        
        plugin_config = self.executor.read_plugin_config(plugin_dir)
        
        image_tag = f"plugin-{self.graph.nodes[plugin_iri]['label'].lower().replace(' ', '-')}"
        
        try:
            self.executor.build_docker_image(image_tag, plugin_dir)
            self.executor.run_docker_container(image_tag, inputs_dir, outputs_dir, plugin_config)
        except subprocess.CalledProcessError as e:
            logging.error("Docker execution failed! Exit Code: %s", e.returncode)
            logging.error("STDERR: %s", e.stderr.decode())
            raise

        output_var_iri = next(s for s in self.graph.successors(step_iri) if self.graph.nodes[s]['type'] == 'Variable')
        output_title = self.graph.nodes[output_var_iri]['label'].replace(' ', '_')
        
        final_output_path = self.executor.finalize_output(
            outputs_dir, os.path.join(self.workspace, "results"), output_title
        )
        
        self.results_map[output_var_iri] = final_output_path
        logging.info("Step successful. Output for '%s' is at: %s", output_title, final_output_path)

    def run(self):
        try:
            self.executor.setup_main_workspace(self.workspace)
            all_step_outputs = {
                succ_iri
                for node_iri, data in self.graph.nodes(data=True) if data.get('type') == 'Step'
                for succ_iri in self.graph.successors(node_iri) if self.graph.nodes[succ_iri].get('type') == 'Variable'
            }
            
            for node_iri, data in self.graph.nodes(data=True):
                 if data.get('type') == 'Dataset' and data.get('accessURLs'):
                    var_iri = next(p for p in self.graph.predecessors(node_iri))
                    if var_iri not in all_step_outputs:
                        dataset_title = data['label'].replace(' ', '_')
                        dataset_dir = os.path.join(self.workspace, "initial_datasets", dataset_title)
                        for url in data['accessURLs']:
                            self.executor.fetch_file(url, dataset_dir)
                        self.results_map[var_iri] = dataset_dir

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


# --- Main Execution Block (Refactored for Dry Run) ---
app = typer.Typer(
    help="A command-line tool to build, visualize, and execute semantic pipeline workflows.",
    add_completion=False,
    no_args_is_help=True
)

def _build_graph_for_command(start_uuid: str, regenerate_uuids: Optional[List[str]] = None) -> nx.DiGraph:
    """Helper function to build the graph for any command."""
    typer.echo(f"--- Building Combined Workflow Graph (starting from {start_uuid}) ---")
    builder = CombinedWorkflowBuilder(api_base_url=METADATA_STORE_BASE_URL)
    final_graph = builder.build_graph(start_uuid, regenerate_uuids=regenerate_uuids)

    if not final_graph or final_graph.number_of_nodes() == 0:
        logging.critical("Failed to build a graph. Please check the starting UUID and API endpoint.")
        raise typer.Exit(code=1)

    logging.info(f"Successfully built combined graph with {final_graph.number_of_nodes()} nodes and {final_graph.number_of_edges()} edges.")
    return final_graph

@app.command()
def visualize(
    start_uuid: str = typer.Argument(..., help="The starting pipeline UUID to visualize.")
):
    """Builds and visualizes the pipeline workflow graph without executing it."""
    final_graph = _build_graph_for_command(start_uuid)
    typer.echo("Visualizing graph...")
    visualize_graph(final_graph)

@app.command()
def execute(
    start_uuid: str = typer.Argument(..., help="The starting pipeline UUID to execute."),
    regenerate: Optional[List[str]] = typer.Option(
        None, "--regenerate", "-r",
        help="A dataset UUID to regenerate. Can be used multiple times."
    ),
    dry_run: bool = typer.Option(
        False, "--dry-run",
        help="Print the execution steps without running them."
    )
):
    """Builds and executes the full pipeline workflow."""
    if not dry_run and shutil.which("docker") is None:
        logging.error("Docker is not installed or not in the system's PATH. Cannot execute pipeline.")
        raise typer.Exit(code=1)

    final_graph = _build_graph_for_command(start_uuid, regenerate_uuids=regenerate)
    
    if dry_run:
        typer.secho("\n--- Starting Dry Run ---", fg=typer.colors.YELLOW, bold=True)
        step_executor = DryRunExecutor()
    else:
        typer.echo("\n--- Starting Pipeline Orchestration ---")
        step_executor = LiveExecutor()

    try:
        orchestrator = Orchestrator(final_graph, executor=step_executor)
        orchestrator.run()
    except Exception:
        if not dry_run:
            typer.secho("Execution failed. Check logs for details.", fg=typer.colors.RED, bold=True)
        raise typer.Exit(code=1)


if __name__ == "__main__":
    app()