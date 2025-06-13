# src/orchestrator/models.py

from dataclasses import dataclass, field
from typing import List, Dict, Set

@dataclass
class Dataset:
    """
    Represents a dcat:Dataset within the pipeline. It holds the
    conceptual information about a data artifact, including its
    identifier (IRI) and a potential download URL. [1]
    """
    iri: str
    title: str
    access_url: str = None  # Populated if the data already exists [1]

@dataclass
class Plugin:
    """
    Represents a df:usesPlugin entity, which is a self-contained,
    runnable component of a step. [1]
    """
    iri: str
    access_url: str # The URL to download the plugin's ZIP archive [1]

@dataclass
class PipelineStep:
    """
    Represents a p-plan:Step in the pipeline. This is a single unit
    of work, tied to a specific plugin and connected to inputs and
    outputs. [1]
    """
    iri: str
    title: str
    plugin: Plugin
    inputs: Dict = field(default_factory=dict)
    outputs: Dict = field(default_factory=dict)
    predecessors: Set[str] = field(default_factory=set) # IRIs of steps that must run before this one [1]

@dataclass
class PipelinePlan:
    """
    Represents the overall p-plan:Plan. It contains the collection
    of all steps that make up the pipeline. [1]
    """
    iri: str
    title: str
    steps: Dict = field(default_factory=dict)