#!/usr/bin/env python3
"""
Manager CLI - A command-line tool for interacting with the Middleware API
Handles posting datasets, plugins, and pipeline configurations.
"""

import json
import sys
import os
from pathlib import Path
from typing import Optional, Dict, Any
import mimetypes

import typer
import requests
from rich.console import Console
from rich.progress import Progress, SpinnerColumn, TextColumn
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

app = typer.Typer(
    name="manager-cli",
    help="Manager CLI - Interact with Middleware API for datasets, plugins, and pipelines",
    add_completion=False
)

console = Console()

class MiddlewareClient:
    """Client for interacting with the Middleware API"""
    
    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url.rstrip('/')
        self.session = requests.Session()

    def _post_file(self, file_path: Path, title: str, description: str, endpoint: str, item_name: str) -> Optional[str]:
        """
        Helper method to post a file to the middleware.
        
        Args:
            file_path: Path to the file.
            title: Title for the item.
            description: Description for the item.
            endpoint: API endpoint (e.g., "datasets", "plugins").
            item_name: Name of the item for messages (e.g., "dataset", "plugin").
            
        Returns:
            UUID of the created item on success, None on failure.
        """
        if not file_path.exists():
            console.print(f"[red]✗ File {file_path} does not exist[/red]")
            return None
        
        content_type, _ = mimetypes.guess_type(str(file_path))
        if not content_type:
            content_type = 'application/octet-stream'
        
        try:
            with Progress(
                SpinnerColumn(),
                TextColumn("[progress.description]{task.description}"),
                console=console
            ) as progress:
                task_description = f"Uploading {item_name}..."
                task = progress.add_task(task_description, total=None)
                
                with open(file_path, 'rb') as f:
                    files = {'file': (file_path.name, f, content_type)}
                    data = {'title': title, 'description': description}
                    
                    url = f"{self.base_url}/api/v1/{endpoint}"
                    response = self.session.post(url, files=files, data=data)
                
                progress.remove_task(task)
                
                if response.status_code == 201:
                    uri = response.text
                    uuid = uri.split('#')[-1]
                    console.print(f"[dim]full uri: {uri}[/dim]")
                    console.print(f"[green]✓ {item_name.capitalize()} created successfully with UUID: {uuid}[/green]")
                    return uri
                else:
                    console.print(f"[red]✗ Failed to create {item_name}: {response.status_code} - {response.text}[/red]")
                    return None
                    
        except requests.RequestException as e:
            console.print(f"[red]✗ Network error posting {item_name}: {e}[/red]")
            return None
        except Exception as e:
            console.print(f"[red]✗ Error posting {item_name}: {e}[/red]")
            return None

    def post_dataset(self, file_path: Path, title: str, description: str) -> Optional[str]:
        """
        Post a dataset file to the middleware
        
        Args:
            file_path: Path to the dataset file
            title: Title for the dataset
            description: Description for the dataset
            
        Returns:
            UUID of the created dataset on success, None on failure
        """
        return self._post_file(file_path, title, description, "datasets", "dataset")
    
    def post_plugin(self, file_path: Path, title: str, description: str) -> Optional[str]:
        """
        Post a plugin file to the middleware
        
        Args:
            file_path: Path to the plugin file
            title: Title for the plugin
            description: Description for the plugin
            
        Returns:
            UUID of the created plugin on success, None on failure
        """
        return self._post_file(file_path, title, description, "plugins", "plugin")
    
    def post_pipeline(self, pipeline_file: Path) -> Optional[str]:
        """
        Post a pipeline configuration to the middleware
        
        Args:
            pipeline_file: Path to the JSON pipeline configuration file
            
        Returns:
            UUID of the created pipeline on success, None on failure
        """
        if not pipeline_file.exists():
            console.print(f"[red]✗ Pipeline file {pipeline_file} does not exist[/red]")
            return None
        
        try:
            with open(pipeline_file, 'r') as f:
                pipeline_config = json.load(f)
            
            # Validate basic structure
            required_fields = ['title', 'variables', 'steps']
            for field in required_fields:
                if field not in pipeline_config:
                    console.print(f"[red]✗ Pipeline configuration missing required field: {field}[/red]")
                    return None
            
            with Progress(
                SpinnerColumn(),
                TextColumn("[progress.description]{task.description}"),
                console=console
            ) as progress:
                task = progress.add_task("Creating pipeline...", total=None)
                
                url = f"{self.base_url}/api/v1/pipelines"
                response = self.session.post(url, json=pipeline_config, headers={'Content-Type': 'application/json'})
                
                progress.remove_task(task)
                
                if response.status_code == 201:
                    uri = response.text
                    uuid = uri.split('#')[-1]
                    console.print(f"[green]✓ Pipeline created successfully with UUID: {uuid}[/green]")
                    return uuid
                else:
                    console.print(f"[red]✗ Failed to create pipeline: {response.status_code} - {response.text}[/red]")
                    return None
                    
        except json.JSONDecodeError as e:
            console.print(f"[red]✗ Invalid JSON in pipeline file: {e}[/red]")
            return None
        except requests.RequestException as e:
            console.print(f"[red]✗ Network error posting pipeline: {e}[/red]")
            return None
        except Exception as e:
            console.print(f"[red]✗ Error posting pipeline: {e}[/red]")
            return None

# Global options
@app.callback()
def main(
    ctx: typer.Context,
    url: Optional[str] = typer.Option(
        None,
        "--url",
        help="Base URL of the middleware API (overrides METADATA_STORE_URL env variable)"
    ),
    verbose: bool = typer.Option(
        False,
        "--verbose",
        "-v",
        help="Enable verbose output"
    )
):
    """Manager CLI - Interact with Middleware API for datasets, plugins, and pipelines"""
    ctx.ensure_object(dict)
    
    # Get URL from command line, environment variable, or default
    if url:
        middleware_url = url
    else:
        middleware_url = os.getenv('METADATA_STORE_URL', 'http://localhost:8080')
    
    ctx.obj['url'] = middleware_url
    ctx.obj['verbose'] = verbose
    
    if verbose:
        env_source = "command line" if url else ("environment variable" if os.getenv('METADATA_STORE_URL') else "default")
        console.print(f"[dim]Connecting to middleware at: {middleware_url} (from {env_source})[/dim]")

@app.command()
def dataset(
    ctx: typer.Context,
    file: Path = typer.Argument(..., help="Path to the dataset file"),
    title: str = typer.Option(..., "--title", "-t", help="Title for the dataset"),
    description: str = typer.Option(..., "--description", "-d", help="Description for the dataset")
):
    """Post a dataset file to the middleware"""
    client = MiddlewareClient(ctx.obj['url'])
    uuid = client.post_dataset(file, title, description)
    
    if uuid is None:
        raise typer.Exit(1)

@app.command()
def plugin(
    ctx: typer.Context,
    file: Path = typer.Argument(..., help="Path to the plugin file"),
    title: str = typer.Option(..., "--title", "-t", help="Title for the plugin"),
    description: str = typer.Option(..., "--description", "-d", help="Description for the plugin"),
):
    """Post a plugin to the middleware"""    
    client = MiddlewareClient(ctx.obj['url'])
    uuid = client.post_plugin(file, title, description)
    
    if uuid is None:
        raise typer.Exit(1)

@app.command()
def pipeline(
    ctx: typer.Context,
    file: Path = typer.Argument(..., help="Path to the JSON pipeline configuration file")
):
    """Post a pipeline configuration to the middleware"""
    client = MiddlewareClient(ctx.obj['url'])
    uuid = client.post_pipeline(file)
    
    if uuid is None:
        raise typer.Exit(1)

if __name__ == '__main__':
    app()