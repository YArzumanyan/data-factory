# Data Factory Manager CLI

Manager CLI is a utility tool used for managing pipeline configuration files. The pipeline configuration file describes workflows as a graph of nodes (tasks and datasets) with explicit dependencies (edges). The deployed configuration file will have both the pipeline id and the system-assigned version in its filename.

---

## 1. CLI Overview

```
Usage: manager <COMMAND> [OPTIONS]
```

**Primary Command Groups:**
- **pipeline** – For managing pipeline configurations (creation, update, and fetching).
- **artifact** – For handling artifact uploads when needed.
- **help** – For command usage details.

---

## 2. Pipeline Commands

### a. Validate Pipeline Configuration

Validate the JSON configuration file for proper structure and dependency integrity.

```
Usage: manager pipeline validate -f <config-file>
```

**Example:**

```bash
manager pipeline validate -f my_pipeline.json
```

*What It Does:*
- Reads the JSON configuration file.
- Validates the schema.
- Checks for issues such as cycles in the dependency graph.
- Reports any errors or warnings without modifying the file.

---

### b. Deploy (Create or Update) a Pipeline

Deploying creates a *deployed version* of the pipeline configuration with all artifact references updated and registers it with the metadata store.

```
Usage: manager pipeline deploy -f <config-file>
```

**Example:**

```bash
manager pipeline deploy -f my_pipeline.json
```

*What It Does:*
1. **Validation:**  
   - Runs an initial validation on the provided JSON file.  
   - **Note:** If the configuration file contains a `version` property, an error is printed.

2. **Artifact Processing:**  
   - **For each artifact reference in the configuration:**
     - **Local Files (`file://`):**  
       - Strips the `file://` prefix, reads the local file, and uploads its content to the artifact repository.  
       - Receives a new artifact URI (for example, `artifact://<unique-id>`) and replaces the local file path in the configuration.
     - **Remote URIs (e.g., `ftp://`, `http://`):**  
       - Retains these URIs as provided.

3. **Creating the Deployed Version:**  
   - Generates a copy of the configuration file (the *deployed version*) with all artifact references updated to the correct URIs.

4. **API Communication:**  
   - Sends the deployed configuration to the metadata store through the API.
   - **Case 1: _Updating an Existing Pipeline_**  
     - If the configuration includes an `id` and the pipeline exists, the system updates it and returns the pipeline `id` and new version.
     - If the pipeline with the provided `id` does not exist, an error is printed.
   - **Case 2: _Creating a New Pipeline_**  
     - If the configuration does not include an `id`, a new pipeline is created (as version 1) and a new pipeline `id` and version are returned.
   - The returned pipeline `id` and version are inserted into the deployed configuration file.

5. **Output:**  
   - Prints a confirmation message that includes:
     - The assigned (or updated) pipeline ID.
     - The path to the deployed configuration file.  
       The filename includes both the pipeline id and the version.

**Output Example:**

```
Pipeline deployed successfully.
Pipeline ID: 123e4567-e89b-12d3-a456-426614174000
Deployed config file: /path/to/deployed_my_pipeline_123e4567-e89b-12d3-a456-426614174000_v2.json
```

---

### c. Fetch Pipeline Configuration

Fetch (download) the deployed pipeline configuration from the metadata store through the API by its pipeline ID.

```
Usage: manager pipeline fetch <pipeline-id> [-v <version>] [-o <output-path>]
```

**Example:**

```bash
manager pipeline fetch 123e4567-e89b-12d3-a456-426614174000 -v 1 -o ./fetched_pipeline.json
```

*What It Does:*
- Retrieves the deployed pipeline configuration file (which includes all updated artifact URIs and the pipeline ID) from the metadata store.
- Downloads the file to a local path.

---

## 3. Artifact Commands

### a. Upload an Artifact (Optional Manual Upload)

Manually upload a script or dataset if there's a need to pre-upload an artifact. This is useful if you want to obtain an artifact URI before referencing it in the pipeline configuration – especially if you want to upload a remote artifact to storage.

```
Usage: manager artifact upload -f <artifact-file> [--type <script|dataset>]
```

**Example:**

```bash
manager artifact upload -f my_script.py --type script
```

*What It Does:*
- Reads the specified file and uploads it to the artifact store.
- Returns a URI (e.g., `artifact://<unique-id>`) that you can use in the pipeline configuration.

---

## 4. Help and Documentation

For additional details on any command, use the help command:

```
Usage: manager help <COMMAND>
```

**Example:**

```bash
manager help pipeline deploy
```

---

## 5. Full Workflow Example

1. **Prepare The Pipeline Configuration File**  
   Create or update the JSON configuration file (e.g., `my_pipeline.json`) with the required nodes, edges, execution parameters, and metadata.  
   - Use artifact references with appropriate URI prefixes:  
     - Example for a local file: `"artifact": "file:///path/to/myscript.py"`  
     - Example for a remote resource: `"artifact": "http://example.com/dataset.csv"`

2. **Validate the Configuration (Optional)**
   ```bash
   manager pipeline validate -f my_pipeline.json
   ```
   - Check for any schema or dependency errors and make adjustments if needed.

3. **Deploy the Pipeline**
   ```bash
   manager pipeline deploy -f my_pipeline.json
   ```
   - The CLI validates the configuration.
   - For each artifact reference:
     - If the reference is a local file (prefixed with `file://`), the CLI reads and uploads the file and replaces it with the new artifact URI.
     - Remote URIs (like `http://` or `ftp://`) remain unchanged.
   - A deployed version of the configuration is created with the updated URIs.
   - The deployed configuration is sent to the metadata API.
   - Based on the presence (or absence) of the `id` property:
     - **If `id` is set:**  
       - The system updates the existing pipeline (if found) and returns the pipeline id and new version.
       - Otherwise, an error is printed.
     - **If `id` is not set:**  
       - A new pipeline is created with version 1, and the new id and version are returned.
   - The CLI outputs the pipeline id and the path to the deployed configuration file (which includes the id and version in the filename).

4. **Fetch the Deployed Pipeline Configuration**
   ```bash
   manager pipeline fetch 123e4567-e89b-12d3-a456-426614174000
   ```
   - Retrieves the deployed configuration file for the specified pipeline id.

---

## 6. Pipeline Configuration Schema

The pipeline configuration file must conform to a defined JSON Schema. Below is the JSON Schema along with documentation on its structure.

### JSON Schema Overview

- **Top-level Object (`pipeline`):**
  - **id** (string, Optional):  
    A unique identifier for the pipeline. If provided, the deployment is treated as an update request.
  - **name** (string, Required):  
    The name of the pipeline.
  - **description** (string, Optional):  
    A description of the pipeline.
  - **nodes** (array, Required):  
    An array of nodes that represent tasks (scripts) or datasets.
  - **edges** (array, Required):  
    An array of edges that define the dependencies between nodes.

- **Important:**  
  Do not supply a `"version"` property in your configuration file. If present, validation will fail. The system assigns the version during deployment.

### Nodes

Each node includes:

- **id** (string, Required): A unique identifier for the node.
- **type** (string, Required): Either `"script"` or `"dataset"`.
- **name** (string, Required): A human-readable name.
- **artifact** (string, Required): A URI reference to the resource. Supported protocols include:
  - `file://` (for local files, which are uploaded)
  - `http://` or `ftp://` (for remote resources, preserved as-is)
- **arguments** (array of strings, Optional):  
  Command-line arguments for tasks, supporting placeholders (e.g., `{{placeholder}}`) to reference outputs from upstream nodes.
- **inputs** (array of strings, Optional):  
  Input keys expected by the node.
- **outputs** (array of strings, Optional):  
  Output keys produced by the node.
- **parameters** (object, Optional):  
  Additional configuration parameters for the node.

### Edges

Edges define dependencies between nodes and include:

- **source** (string, Required):  
  The `id` of the node producing an output.
- **target** (string, Required):  
  The `id` of the node consuming the output.
- **mapping** (object, Optional):  
  A mapping between the source node's output keys and the target node's input placeholders. For example, `{ "raw_data": "input_data" }` means that the output key `raw_data` is passed as `input_data` to the target node.

### JSON Schema Definition

Below is the full JSON Schema (`pipeline-schema.json`):

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Pipeline Configuration Schema",
  "description": "A JSON Schema for defining graph-based pipelines with tasks (nodes) and dependencies (edges). Nodes may be of type 'script' or 'dataset' and can include custom command-line arguments with placeholders. **Note:** The 'version' property is not allowed; the system assigns a version upon deployment.",
  "type": "object",
  "properties": {
    "pipeline": {
      "type": "object",
      "properties": {
        "id": {
          "description": "Optional pipeline identifier. If provided, the system will treat the deployment as an update request.",
          "type": "string"
        },
        "name": {
          "description": "The name of the pipeline.",
          "type": "string"
        },
        "description": {
          "description": "A description of the pipeline.",
          "type": "string"
        },
        "nodes": {
          "description": "An array of nodes representing tasks (scripts) or datasets.",
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "id": {
                "description": "A unique identifier for the node.",
                "type": "string"
              },
              "type": {
                "description": "The type of node: either a script or a dataset.",
                "type": "string",
                "enum": ["script", "dataset"]
              },
              "name": {
                "description": "A human-readable name for the node.",
                "type": "string"
              },
              "artifact": {
                "description": "A URI reference to the script or dataset. Supported protocols include file://, http://, ftp://, etc.",
                "type": "string"
              },
              "arguments": {
                "description": "An array of command-line arguments. Placeholders (e.g., {{placeholder}}) may be used to refer to outputs from upstream nodes.",
                "type": "array",
                "items": {
                  "type": "string"
                }
              },
              "inputs": {
                "description": "An array of input keys expected by this node.",
                "type": "array",
                "items": {
                  "type": "string"
                }
              },
              "outputs": {
                "description": "An array of output keys produced by this node.",
                "type": "array",
                "items": {
                  "type": "string"
                }
              },
              "parameters": {
                "description": "An object containing additional configuration parameters for the node.",
                "type": "object",
                "additionalProperties": true
              }
            },
            "required": ["id", "type", "name", "artifact"],
            "additionalProperties": false
          }
        },
        "edges": {
          "description": "An array of dependency edges connecting nodes. Each edge defines the flow of outputs from one node (source) to inputs in another node (target).",
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "source": {
                "description": "The node id of the source node producing an output.",
                "type": "string"
              },
              "target": {
                "description": "The node id of the target node consuming the output.",
                "type": "string"
              },
              "mapping": {
                "description": "A mapping between output keys of the source node and input placeholders/keys in the target node.",
                "type": "object",
                "patternProperties": {
                  "^[a-zA-Z0-9_]+$": {
                    "type": "string"
                  }
                },
                "additionalProperties": false
              }
            },
            "required": ["source", "target"],
            "additionalProperties": false
          }
        }
      },
      "required": ["name", "nodes", "edges"],
      "additionalProperties": false
    }
  },
  "required": ["pipeline"],
  "additionalProperties": false
}
```

### Example Configuration

Here is an example pipeline configuration that complies with the schema:

```json
{
  "pipeline": {
    "id": "pipeline-123",  // Optional; include only when updating an existing pipeline.
    "name": "Complex Data Pipeline with Datasets",
    "description": "A graph-based pipeline that demonstrates branching and merging, including dataset nodes.",
    "nodes": [
      {
        "id": "task1",
        "type": "script",
        "name": "Data Ingestion",
        "artifact": "file:///path/to/ingest_script.py",
        "outputs": ["raw_data"],
        "parameters": {
          "source": "database"
        }
      },
      {
        "id": "dataset1",
        "type": "dataset",
        "name": "Reference Data",
        "artifact": "ftp://ftp.example.com/data/reference.csv"
      },
      {
        "id": "task2",
        "type": "script",
        "name": "Data Processing",
        "artifact": "file:///path/to/processing_script.py",
        "arguments": [
          "-f", "{{raw_data}}",
          "-r", "{{reference_data}}",
          "--threshold", "0.75"
        ],
        "inputs": ["raw_data", "reference_data"],
        "outputs": ["processed_data"],
        "parameters": {
          "threshold": 0.75
        }
      },
      {
        "id": "dataset2",
        "type": "dataset",
        "name": "Supplementary Data",
        "artifact": "http://example.com/supplementary_data.csv"
      },
      {
        "id": "task3",
        "type": "script",
        "name": "Data Enrichment",
        "artifact": "http://example.com/enrich_script.py",
        "arguments": [
          "--input", "{{processed_data}}",
          "--supp", "{{supp_data}}"
        ],
        "inputs": ["processed_data", "supp_data"],
        "outputs": ["enriched_data"]
      },
      {
        "id": "task4",
        "type": "script",
        "name": "Final Aggregation",
        "artifact": "file:///path/to/aggregate_script.py",
        "arguments": [
          "-a", "{{processed_data}}",
          "-b", "{{enriched_data}}"
        ],
        "inputs": ["processed_data", "enriched_data"],
        "outputs": ["final_output"]
      }
    ],
    "edges": [
      {
        "source": "task1",
        "target": "task2",
        "mapping": {
          "raw_data": "raw_data"
        }
      },
      {
        "source": "dataset1",
        "target": "task2",
        "mapping": {
          "reference_data": "reference_data"
        }
      },
      {
        "source": "task2",
        "target": "task3",
        "mapping": {
          "processed_data": "processed_data"
        }
      },
      {
        "source": "dataset2",
        "target": "task3",
        "mapping": {
          "supp_data": "supp_data"
        }
      },
      {
        "source": "task2",
        "target": "task4",
        "mapping": {
          "processed_data": "processed_data"
        }
      },
      {
        "source": "task3",
        "target": "task4",
        "mapping": {
          "enriched_data": "enriched_data"
        }
      }
    ]
  }
}
```