# Metadata Store

The Metadata Store is a Spring Boot application serving as a central repository for managing pipeline configurations and metadata for our data factory. It stores JSON "recipes" for pipelines—defining graph-based workflows with tasks (nodes) and dependencies (edges)—and validates them against a defined JSON Schema before persisting.

## Getting Started

### Prerequisites

- **Java Development Kit (JDK) 11 or higher**
- **Maven** (or Gradle, based on your preference)

### Clone the Repository

```bash
git clone https://github.com/YArzumanyan/data-factory-metadata-store.git
cd metadata-store
```

### Build the Project

If using Maven:

```bash
mvn clean install
```

### Run the Application

You can run the application directly using Maven:

```bash
mvn spring-boot:run
```

Or run the generated JAR file:

```bash
java -jar target/metadata-store-0.0.1-SNAPSHOT.jar
```

The application will start on the default port (typically `8080`). You can change this in the `src/main/resources/application.properties` file if needed.

## API Endpoints

- **Create/Update Pipeline**
  - **Endpoint:** `POST /pipelines`
  - **Description:** Accepts a JSON payload representing the pipeline configuration. If the payload includes an `id`, the deployment is treated as an update.
  
- **Retrieve Pipeline by ID**
  - **Endpoint:** `GET /pipelines/{pipelineId}`
  - **Description:** Retrieves the pipeline configuration for the specified pipeline ID.

- **Retrieve All Pipelines**
  - **Endpoint:** `GET /pipelines`
  - **Description:** Returns a list of all pipeline configurations stored in the Metadata Store.

## Configuration

All configuration settings (e.g., server port, database connections) are managed in the `src/main/resources/application.properties` file.


### Pipeline Configuration Schema

Pipeline configurations must adhere to the following JSON Schema:

```json
{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Pipeline.java Configuration Schema",
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

### Example Pipeline Payload

Below is an example of a valid pipeline configuration:

```json
{
  "pipeline": {
    "name": "Example Pipeline.java",
    "description": "This pipeline processes sample data.",
    "nodes": [
      {
        "id": "node1",
        "type": "script",
        "name": "Data Fetcher",
        "artifact": "http://example.com/scripts/data-fetcher.sh",
        "arguments": ["--source", "api"],
        "inputs": [],
        "outputs": ["raw_data"],
        "parameters": {
          "timeout": "30s"
        }
      },
      {
        "id": "node2",
        "type": "dataset",
        "name": "Raw Data Set",
        "artifact": "file:///data/raw_data.csv",
        "arguments": [],
        "inputs": ["raw_data"],
        "outputs": [],
        "parameters": {}
      }
    ],
    "edges": [
      {
        "source": "node1",
        "target": "node2",
        "mapping": {
          "raw_data": "raw_data"
        }
      }
    ]
  }
}
```
