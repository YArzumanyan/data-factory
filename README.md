# Data Factory

The Data Factory is a modular platform designed to enable users to create, run, and share data processing pipelines in a flexible, FAIR-compliant manner. The project is built around four main components that work together to orchestrate the execution of pipelines constructed from datasets and scripts.

## Pipeline Configuration and JSON Schema

The JSON schema defines the structure of the pipeline configuration and metadata files, ensuring that each file adheres to a standard format that the various components of the Data Factory can understand. Below is a detailed breakdown:

### The Root Object: Pipeline

At the highest level, the pipeline configuration is a JSON file that represents a pipeline as a directed graph. This root object must include:

- **type:**  
  An array of strings that, for a pipeline, includes `"pipeline"`. This tells the system that the file is a pipeline configuration.
  
- **url:**  
  A unique identifier (or resource locator) for the pipeline. This can be a simple ID or a URL.

- **nodes:**  
  An array that contains each individual node (either a dataset or a script) that makes up the pipeline.

### Nodes: Datasets and Scripts

Each entry in the `nodes` array represents a component of the pipeline. They extend a base "ROOT" structure with the following common properties:

- **id:**  
  A unique identifier within the pipeline. This ID is critical for referencing the node later, especially for dynamic binding during execution.

- **label (optional):**  
  A human-friendly name for the node.

- **dependsOn:**  
  An array listing the node IDs that the current node depends on. This is used to establish the execution order.

- **previousVersion (optional):**  
  A field that can hold a URL reference to an earlier version of the configuration, which helps with version control.

#### Dataset Nodes

Dataset nodes can come in different types:

- **`dataset-file`:**  
  Indicates that the dataset configuration is stored as a standalone file. If you include the dataset configuration directly within the pipeline file, you would omit this type.

- **`dataset-derived`:**  
  Represents a dataset that is computed by running a sub-pipeline. It requires:
  - **pipeline:** A URL pointing to the sub-pipeline configuration.
  - **bindings (optional):** An array that maps inputs of the sub-pipeline to nodes in the current pipeline. Each binding object includes:
    - `node`: The ID of the node within the referenced pipeline.
    - `dataset`: The ID of the dataset in the current pipeline that should be bound.

- **`dataset-reference`:**  
  Used within the pipeline configuration to refer to an external, standalone dataset file. When used, its node ID becomes the reference for dynamic binding during execution.

#### Script Nodes

Script nodes have similar types:

- **`script-file`:**  
  Indicates that the script's configuration is maintained in a standalone file (stored in the Metadata Store). If you embed the configuration directly in the pipeline, omit this type.

- **`script-reference`:**  
  This type is used in the pipeline configuration to refer to an external standalone script file.

In both cases, scripts (whether standalone or referenced) use the following properties in addition to the common ones:
  
- **distribution:**  
  An object containing a URL (`url`) where the script can be downloaded. For standalone files, this distribution information is used to retrieve the file.  
- **arguments (in script metadata):**  
  In the standalone script metadata, the `arguments` object can contain both simple static values (e.g., numbers, strings, booleans) and dynamic references. For dynamic values, you use an object like `{ "ref": "node_id" }`. At runtime, these placeholders are replaced with the actual resource (either a dataset node or an output from another script).

### Dynamic Binding Through Arguments

How does the script metadata handle arguments:
  
- The dynamic references are integrated directly into the `arguments` section. This means that any argument that is meant to be dynamic is provided as an object with a `ref` key.  
- The arguments structure supports both static values and dynamic references. For example:

  ```json
  "arguments": {
    "input_file": { "ref": "raw_data" },
    "supplement": { "ref": "supplemental_info" },
    "threshold": "0.75",
    "verbose": true
  }
  ```

  In this example, `input_file` and `supplement` are dynamically resolved at runtime using the node IDs `"raw_data"` and `"supplemental_info"`. The static arguments `threshold` and `verbose` are passed as is. When parsed, the script execution will look similar this:

  ```bash
  ./script --input_file /path/to/raw_data --supplement /path/to/supplemental_info --threshold 0.75 --verbose
  ```
## Architecture and Components

## Components

The Data Factory is composed of four main components, each designed with a clear set of responsibilities, attempting to ensure flexibility, scalability, and adherence to FAIR principles. Below is a high-level view of each component:

### Database

- **Purpose:**  
  Acts as the persistent storage layer for standalone configuration files for datasets and scripts. Majority of files that are referenced by `distribution.url` in the pipeline configurations are stored here.
  
- **Key Features:**  
  - Uses a simple NoSQL database for lightweight and efficient storage.
  - Provides basic API endpoints for uploading, retrieving, and managing dataset and script files.
  - Integrates with the Metadata Store, allowing file IDs or URLs to be linked within pipeline configurations.

- **Technology Stack:**  
  - Developed in Python using FastAPI to expose a RESTful API.
  - NoSQL storage.

### Metadata Store

- **Purpose:**  
  Serves as the central repository for pipeline configurations and the standalone files that describe datasets and scripts.
  
- **Key Features:**  
  - Stores JSON-based pipeline configurations, including both inline definitions and external references.
  - Manages standalone configuration files (i.e., those with types `dataset-file` and `script-file`).
  - Ensures interoperability by maintaining open API endpoints documented via Swagger/OpenAPI.

- **Technology Stack:**  
  - Implemented using Java Spring Boot providing REST API capabilities.
  - Uses a simple NoSQL database (or similar storage) for backend storage, complementing the Database component.

### Manager CLI

- **Purpose:**  
  A command-line interface tool that facilitates the creation, validation and deployment of pipeline configurations.
  
- **Key Features:**  
  - **Validation:**  
    Checks the JSON syntax and logical consistency of the pipeline configuration and standalone files. This includes verifying dependencies, types, and dynamic references.
    
  - **Preparation for Deployment:**  
    - Processes local files (prefixed with `file://`) by uploading them to the Database.
    - Modifies `distribution.url` entries in pipeline and standalone configuration files to point to the appropriate IDs from the Database.
    - Prepares the overall configuration structure so that it’s ready for deployment via the Metadata Store.

- **Technology Stack:**  
  - Developed in Python with Typer, ensuring a user-friendly CLI experience.

### Executor CLI

- **Purpose:**  
  This CLI is responsible for fetching pipeline configurations from the Metadata Store, dynamically binding external references, and executing the pipeline in an isolated environment.
  
- **Key Features:**  
  - **Dynamic Binding:**  
    Binds node IDs in the pipeline (e.g., `dataset-reference` or `script-reference`) to actual standalone file IDs at runtime. For example:
    
    ```bash
    executor-cli run pipeline_001 --dataset-binding raw_data=raw_data_file_id --script-binding data_cleaning=cleaning_script_file_id
    ```
    
  - **Orchestration:**  
    Generates an orchestrator script that determines the execution order based on the `dependsOn` relationships. This script handles:
    - Fetching the necessary configuration files.
    - Resolving dynamic arguments defined in script metadata (supporting both static values and dynamic references, e.g., `{ "ref": "raw_data" }`).
  
  - **Execution Environment:**  
    Sets up a Docker container to execute the orchestrator script. The containerization ensures:
    - Isolation from the host environment.
    - Reproducibility of the execution context.
  
  - **Finalization:**  
    Extracts the results from the Docker container after execution and performs cleanup, such as removing temporary containers.

- **Technology Stack:**  
  - Also implemented in Python using Typer for the command-line interface.
  - Utilizes Docker (with appropriate socket binding) for managing containerized executions.
  - Integrates with the Metadata Store to retrieve pipeline configurations and standalone files.

#### *Orchestrator script*
 
1. Fetch pipeline config file
2. Fetch standalone config files
3. Organize the run order based on dependsOn property of the nodes
4. Fetch all the required datasets and scripts for the pipeline using only distribution `url`s
5. Run the scripts with defined bindings
6. For each derived dataset we reach in this order that doesn't have `distribution` property in pipeline config file:
   - Fetch the pipeline from `Metadata Store` and generate an orchestrator script
   - Run the orchestrator script and save the result locally
   - Modify the config file, add `distribution` property with `url` as the local result

### Assembly and Containerization

- **Integration:**  
  All components are containerized using Docker. A primary Docker Compose file manages the assembly, ensuring that the Database, Metadata Store, Manager CLI, and Executor CLI are correctly networked and can communicate seamlessly.

- **Docker-in-Docker Considerations:**  
  The Manager CLI is designed to handle Docker socket mounting to avoid nesting issues when creating containers from within a container.
 
## API abstract architecture

### Separate endpoints per-type
- api/v1/pipeline
  - getPipelines
- api/v1/file-dataset
- api/v1/derived-dataset
- api/v1/dataset
 
### Single endpoint
Split by value of "type"
- filter ... api/v2/entries?type=pipeline
  - getPipelines

It is possible to have both side by side, the business logic remains the same. The Separate enpoints will be implemented first as it's closer to REST architecture