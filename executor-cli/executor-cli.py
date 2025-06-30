import os
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
from rich.console import Console
from rich.progress import Progress, SpinnerColumn, TextColumn, BarColumn
from rich.logging import RichHandler

# --- Configuration ---
logging.basicConfig(level=logging.WARNING, format='%(message)s', handlers=[RichHandler()])
console = Console()
load_dotenv()

PIPELINE_ENDPOINT = os.getenv("PIPELINE_ENDPOINT", "http://localhost:8083/api/v1/pipelines")
ARTIFACT_REPOSITORY_URL = os.getenv("ARTIFACT_REPOSITORY_URL", "http://localhost:8081")
MAIN_WORKSPACE = os.getenv("MAIN_WORKSPACE", "./tmp/executor_workspace")

# --- Helper Functions ---
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


# --- Graph Visualization Utility ---
def visualize_graph(graph: nx.DiGraph, title: str = "Combined Pipeline Workflow"):
    if not graph or graph.number_of_nodes() == 0:
        console.print("[yellow]Graph is empty, skipping visualization.[/yellow]")
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
    plt.show()


# --- Graph Building Class ---
class CombinedWorkflowBuilder:
    """Builds a combined execution graph by fetching pipeline definitions from the metadata store."""
    def __init__(self, api_base_url: str):
        self.api_base_url = api_base_url
        self.combined_graph = nx.DiGraph()
        self.processed_pipelines: Set[str] = set()

    def _fetch_pipeline_ttl(self, pipeline_uuid: str) -> Optional[str]:
        api_url = f"{self.api_base_url}/{pipeline_uuid}"
        try:
            response = requests.get(api_url, timeout=15)
            response.raise_for_status()
            return response.text
        except requests.exceptions.RequestException as e:
            console.print(f"[red]✗ Failed to fetch pipeline {pipeline_uuid}: {e}[/red]")
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
        return [(str(r.dataset_iri), str(r.generating_pipeline_iri)) for r in rdf_graph.query(query)]

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
                str(row.iri), label=str(row.title), type=str(row.type), accessURLs=urls)
        for row in rdf_graph.query(queries["edges"]):
            self.combined_graph.add_edge(str(row.source), str(row.target), label=str(row.label))

    def build_graph(self, start_pipeline_uuid: str, regenerate_uuids: List[str] = None) -> Optional[nx.DiGraph]:
        if regenerate_uuids is None: regenerate_uuids = []
        pipelines_to_process = [start_pipeline_uuid]

        with Progress(SpinnerColumn(), TextColumn("[progress.description]{task.description}"), console=console) as progress:
            task = progress.add_task("Building execution graph...", total=None)
            
            while pipelines_to_process:
                current_uuid = pipelines_to_process.pop(0)
                if current_uuid in self.processed_pipelines: continue

                progress.update(task, description=f"Processing definition for pipeline: [bold cyan]{current_uuid}[/bold cyan]")
                ttl_data = self._fetch_pipeline_ttl(current_uuid)
                if not ttl_data:
                    console.print(f"[yellow]Could not retrieve data for {current_uuid}. Skipping.[/yellow]")
                    self.processed_pipelines.add(current_uuid)
                    continue
                try:
                    rdf_graph = Graph().parse(data=ttl_data, format="turtle")
                except Exception as e:
                    console.print(f"[red]✗ Failed to parse TTL for {current_uuid}: {e}[/red]")
                    self.processed_pipelines.add(current_uuid)
                    continue
                
                self._add_pipeline_to_combined_graph(rdf_graph)
                dependencies = self._get_inter_pipeline_dependencies(rdf_graph)
                if dependencies:
                    for dataset_iri, pipeline_iri in dependencies:
                        dataset_uuid = get_uuid_from_iri(dataset_iri)
                        pipeline_uuid = get_uuid_from_iri(pipeline_iri)
                        if dataset_uuid in regenerate_uuids:
                            if pipeline_uuid not in self.processed_pipelines:
                                pipelines_to_process.append(pipeline_uuid)
                        
                self.processed_pipelines.add(current_uuid)
        
        console.print("[green]✓ Finished building execution graph.[/green]")

        # Post-process the graph to merge equivalent variables. This simplifies the graph
        dataset_to_vars = {}
        for node_iri, data in self.combined_graph.nodes(data=True):
            if data.get('type') == 'Dataset':
                preds = self.combined_graph.predecessors(node_iri)
                var_list = [p for p in preds if self.combined_graph.nodes[p].get('type') == 'Variable']
                if len(var_list) > 1:
                    dataset_to_vars[node_iri] = var_list

        relabel_map = {}
        for dataset_iri, var_list in dataset_to_vars.items():
            canonical_var = next((v for v in var_list if any(self.combined_graph.nodes[p].get('type') == 'Step' 
                                                             for p in self.combined_graph.predecessors(v))), var_list[0])
            
            for var in var_list:
                if var != canonical_var:
                    relabel_map[var] = canonical_var
                    console.print(f"[dim]Merging variable [cyan]{get_uuid_from_iri(var)}[/cyan] into [cyan]{get_uuid_from_iri(canonical_var)}[/cyan][/dim]")

        nx.relabel_nodes(self.combined_graph, relabel_map, copy=False)

        datasets_to_remove = list(dataset_to_vars.keys())
        self.combined_graph.remove_nodes_from(datasets_to_remove)
        console.print(f"[dim]Removed {len(datasets_to_remove)} redundant dataset nodes from graph.[/dim]")
        
        return self.combined_graph


# --- Execution Strategy Pattern ---
class StepExecutor(ABC):
    """Interface defining all actions with side-effects."""
    @abstractmethod
    def setup_main_workspace(self, path: str): pass
    @abstractmethod
    def prepare_step_workspace(self, base_path: str, step_label: str, step_iri: str) -> tuple[str, str, str]: pass
    @abstractmethod
    def stage_input(self, source_path: str, target_dir: str): pass
    @abstractmethod
    def fetch_file(self, progress: Progress, access_url: str, target_dir: str, artifact_repo_url: Optional[str]) -> str: pass
    @abstractmethod
    def unpack_plugin(self, archive_path: str, target_dir: str): pass
    @abstractmethod
    def read_plugin_config(self, plugin_dir: str) -> dict: pass
    @abstractmethod
    def build_docker_image(self, progress: Progress, image_tag: str, build_context_dir: str): pass
    @abstractmethod
    def run_docker_container(self, progress: Progress, image_tag: str, inputs_dir: str, outputs_dir: str, config: dict): pass
    @abstractmethod
    def finalize_output(self, outputs_dir: str, persistent_dir: str, base_name: str) -> str: pass

class LiveExecutor(StepExecutor):
    """The 'real' executor that performs file operations and runs Docker."""
    def setup_main_workspace(self, path: str):
        if os.path.exists(path): shutil.rmtree(path)
        os.makedirs(path)
    def prepare_step_workspace(self, base_path: str, step_label: str, step_iri: str) -> tuple[str, str, str]:
        step_uuid = get_uuid_from_iri(step_iri)
        unique_dir_name = f"{step_label.replace(' ', '_')}_{step_uuid}"
        step_workspace = os.path.join(base_path, unique_dir_name)
        inputs_dir = os.path.join(step_workspace, "inputs"); outputs_dir = os.path.join(step_workspace, "outputs"); plugin_dir = os.path.join(step_workspace, "plugin")
        os.makedirs(inputs_dir); os.makedirs(outputs_dir); os.makedirs(plugin_dir)
        return inputs_dir, outputs_dir, plugin_dir
    def stage_input(self, source_path: str, target_dir: str):
        if os.path.exists(source_path): shutil.copytree(source_path, target_dir)
        
    def fetch_file(self, progress: Progress, access_url: str, target_dir: str, artifact_repo_url: Optional[str]) -> str:
        if not access_url: raise ValueError("Cannot fetch artifact with an empty accessURL.")
        if artifact_repo_url: access_url = rewrite_url_base(access_url, artifact_repo_url)
        os.makedirs(target_dir, exist_ok=True)
        local_path = os.path.join(target_dir, os.path.basename(access_url))
        
        try:
            with requests.get(access_url, stream=True, timeout=30) as r:
                r.raise_for_status()
                total_size = int(r.headers.get('content-length', 0))
                task_id = progress.add_task(f"Downloading {os.path.basename(access_url)}", total=total_size)
                with open(local_path, 'wb') as f:
                    for chunk in r.iter_content(chunk_size=8192):
                        f.write(chunk)
                        progress.update(task_id, advance=len(chunk))
                progress.remove_task(task_id) # Remove the task when complete
            return local_path
        except requests.exceptions.RequestException as e:
            console.print(f"[red]✗ Failed to download file: {e}[/red]")
            raise

    def read_plugin_config(self, plugin_dir: str) -> dict:
        with open(os.path.join(plugin_dir, "config.json")) as f:
            return json.load(f)

    def unpack_plugin(self, archive_path: str, target_dir: str):
        mime_type = magic.from_file(archive_path, mime=True)
        shutil_format = 'zip'
        if 'zip' in mime_type: shutil_format = 'zip'
        elif 'gzip' in mime_type: shutil_format = 'gztar'
        elif 'tar' in mime_type: shutil_format = 'tar'
        elif 'bzip2' in mime_type: shutil_format = 'bztar'
        else: raise TypeError(f"Plugin {archive_path} is not a recognized archive format.")
        shutil.unpack_archive(archive_path, target_dir, format=shutil_format)

    def build_docker_image(self, progress: Progress, image_tag: str, build_context_dir: str):
        task_id = progress.add_task(f"Building Docker image: [bold blue]{image_tag}[/bold blue]", total=None)
        subprocess.run(["docker", "build", "-t", image_tag, "."], cwd=build_context_dir, check=True, capture_output=True)
        progress.remove_task(task_id)

    def run_docker_container(self, progress: Progress, image_tag: str, inputs_dir: str, outputs_dir: str, config: dict):
        task_id = progress.add_task(f"Running container [bold blue]{image_tag}[/bold blue]...", total=None)
        subprocess.run(["docker", "run", "--rm",
                        "-v", f"{os.path.abspath(inputs_dir)}:{config['input_directory']}",
                        "-v", f"{os.path.abspath(outputs_dir)}:{config['output_directory']}",
                        image_tag], check=True, capture_output=True)
        progress.remove_task(task_id)

    def finalize_output(self, outputs_dir: str, persistent_dir: str, base_name: str) -> str:
        final_output_dir = os.path.join(persistent_dir, base_name)
        shutil.move(outputs_dir, final_output_dir)
        return final_output_dir


class DryRunExecutor(StepExecutor):
    """A 'dry run' executor that only prints actions without side-effects."""
    def setup_main_workspace(self, path: str):
        console.print(f"[yellow][DRY RUN] Set up main workspace at: {path}[/yellow]")
    def prepare_step_workspace(self, base_path: str, step_label: str, step_iri: str) -> tuple[str, str, str]:
        step_uuid = get_uuid_from_iri(step_iri)
        unique_dir_name = f"{step_label.replace(' ', '_')}_{step_uuid}"
        step_workspace = os.path.join(base_path, unique_dir_name)
        console.print(f"[cyan][DRY RUN] Prepare workspace for step '{step_label}' at: {step_workspace}[/cyan]")
        return (os.path.join(step_workspace, "inputs"), os.path.join(step_workspace, "outputs"), os.path.join(step_workspace, "plugin"))
    def stage_input(self, source_path: str, target_dir: str):
        console.print(f"[cyan][DRY RUN] Stage input from {source_path} to {target_dir}[/cyan]")
    def fetch_file(self, progress: Progress, access_url: str, target_dir: str, artifact_repo_url: Optional[str]) -> str:
        if artifact_repo_url: access_url = rewrite_url_base(access_url, artifact_repo_url)
        path = os.path.join(target_dir, os.path.basename(access_url))
        console.print(f"[cyan][DRY RUN] Fetch artifact from {access_url} to {path}[/cyan]")
        return path
    def read_plugin_config(self, plugin_dir: str) -> dict:
        console.print(f"[cyan][DRY RUN] Read config.json from {plugin_dir}[/cyan]")
        return {"input_directory": "/dry_run/in", "output_directory": "/dry_run/out"}
    def unpack_plugin(self, archive_path: str, target_dir: str):
        console.print(f"[cyan][DRY RUN] Unpack plugin {archive_path} to {target_dir}[/cyan]")
    def build_docker_image(self, progress: Progress, image_tag: str, build_context_dir: str):
        cmd = f"docker build -t {image_tag} ."
        console.print(f"[cyan][DRY RUN] In directory '{build_context_dir}', execute: [bold]{cmd}[/bold][/cyan]")
    def run_docker_container(self, progress: Progress, image_tag: str, inputs_dir: str, outputs_dir: str, config: dict):
        cmd = f"docker run --rm -v '{os.path.abspath(inputs_dir)}:{config['input_directory']}' -v '{os.path.abspath(outputs_dir)}:{config['output_directory']}' {image_tag}"
        console.print(f"[cyan][DRY RUN] Execute: [bold]{cmd}[/bold][/cyan]")
    def finalize_output(self, outputs_dir: str, persistent_dir: str, base_name: str) -> str:
        final_path = os.path.join(persistent_dir, base_name)
        console.print(f"[cyan][DRY RUN] Finalize output from {outputs_dir} into {final_path}[/cyan]")
        return final_path


# --- Orchestration Class ---
class Orchestrator:
    """Runs the execution graph step-by-step using a given executor strategy."""
    def __init__(self, graph: nx.DiGraph, executor: StepExecutor, artifact_repo_url: Optional[str]):
        self.graph = graph
        self.executor = executor
        self.workspace = MAIN_WORKSPACE
        self.artifact_repo_url = artifact_repo_url
        self.results_map = {}

    def _execute_step(self, step_iri: str, progress: Progress):
        step_label = self.graph.nodes[step_iri]['label']
        task = progress.add_task(f"Executing step: [bold]{step_label}[/bold]", total=None)
        
        inputs_dir, outputs_dir, plugin_dir = self.executor.prepare_step_workspace(self.workspace, step_label, step_iri)
        for pred_iri in self.graph.predecessors(step_iri):
            node_data = self.graph.nodes[pred_iri]
            if node_data['type'] == 'Variable':
                result_dir_path = self.results_map.get(pred_iri)
                if not result_dir_path:
                    raise ValueError(f"Could not find result for input variable {pred_iri}")
                dest_dir = os.path.join(inputs_dir, node_data['label'].replace(' ', '_'))
                self.executor.stage_input(result_dir_path, dest_dir)
        
        plugin_iri = next(s for s in self.graph.successors(step_iri) if self.graph.nodes[s]['type'] == 'Plugin')
        plugin_access_url = self.graph.nodes[plugin_iri]['accessURLs'][0]
        plugin_archive_path = self.executor.fetch_file(
            progress, plugin_access_url, os.path.join(self.workspace, "artifact_cache"), self.artifact_repo_url
        )
        self.executor.unpack_plugin(plugin_archive_path, plugin_dir)
        plugin_config = self.executor.read_plugin_config(plugin_dir)
        image_tag = f"plugin-{self.graph.nodes[plugin_iri]['label'].lower().replace(' ', '-')}"
        
        try:
            self.executor.build_docker_image(progress, image_tag, plugin_dir)
            self.executor.run_docker_container(progress, image_tag, inputs_dir, outputs_dir, plugin_config)
        except subprocess.CalledProcessError as e:
            console.print("[bold red]Docker execution failed![/bold red]")
            console.print(f"Exit Code: {e.returncode}")
            console.print(f"STDERR:\n{e.stderr.decode()}")
            raise
            
        output_var_iri = next(s for s in self.graph.successors(step_iri) if self.graph.nodes[s]['type'] == 'Variable')
        output_title = self.graph.nodes[output_var_iri]['label'].replace(' ', '_')
        final_output_path = self.executor.finalize_output(outputs_dir, os.path.join(self.workspace, "results"), output_title)
        self.results_map[output_var_iri] = final_output_path
        progress.update(task, total=1, completed=1, description=f"✓ Step completed: [bold green]{step_label}[/bold green]")

    def run(self):
        try:
            self.executor.setup_main_workspace(self.workspace)
            all_step_outputs = {s for n, d in self.graph.nodes(data=True) if d.get('type') == 'Step' for s in self.graph.successors(n) if self.graph.nodes[s].get('type') == 'Variable'}
            
            with Progress(SpinnerColumn(), TextColumn("[progress.description]{task.description}"), BarColumn(), "[progress.percentage]{task.percentage:>3.0f}%", console=console) as progress:
                initial_download_task = progress.add_task("Preparing initial datasets...", total=len(self.graph.nodes()))
                for node_iri, data in self.graph.nodes(data=True):
                    if data.get('type') == 'Dataset' and data.get('accessURLs'):
                        var_iri = next(self.graph.predecessors(node_iri))
                        if var_iri not in all_step_outputs:
                            dataset_title = data['label'].replace(' ', '_')
                            dataset_dir = os.path.join(self.workspace, "initial_datasets", dataset_title)
                            for url in data['accessURLs']:
                                self.executor.fetch_file(progress, url, dataset_dir, self.artifact_repo_url)
                            self.results_map[var_iri] = dataset_dir
                            
                progress.remove_task(initial_download_task)
                
                execution_order = list(nx.topological_sort(self.graph))
                for node_iri in execution_order:
                    if self.graph.nodes[node_iri].get('type') == 'Step':
                        self._execute_step(node_iri, progress)
            
            console.print("\n[bold green]--- Orchestration Finished Successfully! ---[/bold green]")
            final_vars = [n for n in execution_order if self.graph.nodes[n].get('type') == 'Variable' and self.graph.out_degree(n) == 0]
            for var in final_vars:
                 console.print(f"Final output for '{self.graph.nodes[var]['label']}' is at: [cyan]{self.results_map.get(var)}[/cyan]")

        except Exception as e:
            console.print("\n[bold red]--- Orchestration Failed ---[/bold red]")
            console.print(f"[red]Error: {e}[/red]")
            raise

# --- CLI Application ---
app = typer.Typer(
    name="executor-cli",
    help="A command-line tool to build, visualize, and execute semantic pipeline workflows.",
    add_completion=False,
    no_args_is_help=True
)

@app.callback()
def main(
    ctx: typer.Context,
    url: Optional[str] = typer.Option(None, "--url", help=f"Base URL of the metadata store pipeline endpoint (overrides env var or default: {PIPELINE_ENDPOINT})"),
    artifact_url: Optional[str] = typer.Option(None, "--artifact-url", help="Base URL of the artifact repository (overrides ARTIFACT_REPOSITORY_URL env var)"),
):
    """Executor CLI - Build and run pipeline workflows."""
    ctx.ensure_object(dict)
    ctx.obj['api_url'] = url or PIPELINE_ENDPOINT
    ctx.obj['artifact_url'] = artifact_url or ARTIFACT_REPOSITORY_URL
    
@app.command()
def visualize(
    ctx: typer.Context,
    start_uuid: str = typer.Argument(..., help="The starting pipeline UUID to visualize.")
):
    """Builds and visualizes the pipeline workflow graph without executing it."""
    console.print(f"[bold]--- Building Combined Workflow Graph (starting from {start_uuid}) ---[/bold]")
    builder = CombinedWorkflowBuilder(api_base_url=ctx.obj['api_url'])
    final_graph = builder.build_graph(start_uuid)
    
    if not final_graph or final_graph.number_of_nodes() == 0:
        console.print("[bold red]Failed to build a graph. Please check the UUID and API endpoint.[/bold red]")
        raise typer.Exit(code=1)
        
    console.print(f"Successfully built graph with {final_graph.number_of_nodes()} nodes and {final_graph.number_of_edges()} edges.")
    console.print("Visualizing graph...")
    visualize_graph(final_graph)

@app.command()
def execute(
    ctx: typer.Context,
    start_uuid: str = typer.Argument(..., help="The starting pipeline UUID to execute."),
    regenerate: Optional[List[str]] = typer.Option(None, "--regenerate", "-r", help="A dataset UUID to regenerate. Can be used multiple times."),
    dry_run: bool = typer.Option(False, "--dry-run", help="Print the execution steps without running them.")
):
    """Builds and executes the full pipeline workflow."""
    if not dry_run and shutil.which("docker") is None:
        console.print("[bold red]✗ Docker is not installed or not in the system's PATH. Cannot execute pipeline.[/bold red]")
        raise typer.Exit(code=1)
        
    console.print(f"[bold]--- Building Combined Workflow Graph (starting from {start_uuid}) ---[/bold]")
    builder = CombinedWorkflowBuilder(api_base_url=ctx.obj['api_url'])
    final_graph = builder.build_graph(start_uuid, regenerate_uuids=regenerate)
    
    if not final_graph or final_graph.number_of_nodes() == 0:
        console.print("[bold red]Failed to build a graph. Cannot execute.[/bold red]")
        raise typer.Exit(code=1)

    if dry_run:
        console.print("\n[bold yellow]--- Starting Dry Run ---[/bold yellow]")
        step_executor = DryRunExecutor()
    else:
        console.print("\n[bold]--- Starting Pipeline Orchestration ---[/bold]")
        step_executor = LiveExecutor()
        
    try:
        orchestrator = Orchestrator(final_graph, executor=step_executor, artifact_repo_url=ctx.obj['artifact_url'])
        orchestrator.run()
    except Exception:
        if not dry_run:
            console.print("[bold red]Execution failed. Check logs for details.[/bold red]")
        raise typer.Exit(code=1)

if __name__ == "__main__":
    app()