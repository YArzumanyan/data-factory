Okay, here's the project description, workflow, and example of usage for `df-manager`:

### 1\. Project Description

The **df-manager** is a Spring Boot-based middleware application designed to act as a central communication hub between external applications and a backend data management ecosystem. This ecosystem consists of:

* **`metadata-store`**: An RDF triple store (e.g., Apache Jena Fuseki, GraphDB) responsible for storing and managing metadata about data processing pipelines, datasets, and plugins. It uses standard vocabularies like DCAT, DCTerms, P-Plan, PROV-O, and custom `df:` terms.
* **`artifact-repository`**: A storage solution (e.g., a file server, Artifactory, MinIO) that holds the actual data files for datasets and the executable packages for plugins (typically archives like ZIP or TAR.GZ).

**Core Purpose:**
The primary goal of `df-manager` is to simplify interactions for external applications by providing a unified REST API. It abstracts the complexities of direct communication with the `metadata-store` (RDF graph manipulation) and the `artifact-repository` (file uploads/management).

**Key Functionalities:**

* **Artifact Upload (Datasets & Plugins):**
    * Receives artifact files (e.g., a dataset as a CSV file packaged in a ZIP, a plugin as a JAR in a ZIP) and their basic metadata (title, description) from external applications.
    * Uploads the received file to the `artifact-repository`.
    * Receives an identifier (artifact ID) for the uploaded file from the `artifact-repository`.
    * Generates RDF metadata for the artifact (as `dcat:Dataset` or `df:Plugin`). This RDF includes a `dcat:distribution` property with a `dcat:accessURL` pointing to the artifact's fetch location in the `artifact-repository` (using the artifact ID) and a `dcat:compressFormat` based on the uploaded file type.
    * Submits the generated RDF to the `metadata-store`.
* **Pipeline Configuration:**
    * Accepts a JSON configuration from external applications detailing a data processing pipeline (as described in `manager.md`). This JSON defines the pipeline's structure, steps, the plugins used by each step, and the flow of data variables.
    * Validates the JSON configuration.
    * Translates the JSON configuration into a comprehensive RDF graph using P-Plan, DCTerms, `df:`, and PROV-O vocabularies. This includes:
        * Creating a `p-plan:Plan` for the pipeline.
        * Creating `p-plan:Step` resources for each processing step, linking them to pre-existing `df:Plugin` resources (validated against the `metadata-store`).
        * Creating `p-plan:Variable` resources for data flow within the pipeline, potentially linking initial input variables to pre-existing `dcat:Dataset` resources via `prov:specializationOf`.
        * Automatically generating `dcat:Dataset` resources for the final outputs of the pipeline, linked to the terminal `p-plan:Variable`s via `prov:wasDerivedFrom` and to the pipeline itself via `prov:wasGeneratedBy`.
    * Submits the generated pipeline RDF graph to the `metadata-store`.
* **Metadata Retrieval:**
    * Provides GET endpoints for external applications to retrieve the RDF metadata for existing datasets, plugins, and pipelines by their UUIDs. It fetches this RDF directly from the `metadata-store`.

**Technology Stack:**

* Spring Boot 3.4.5
* Java 21
* Apache Jena (for RDF manipulation)
* Maven (for build and dependency management)
* RESTful APIs (for communication)

### 2\. Workflow

The `df-manager` orchestrates interactions as follows:

**A. Uploading a Dataset or Plugin:**

1.  **External Application Request:** An external application sends a `POST` request to `df-manager` (e.g., `/api/v1/datasets` or `/api/v1/plugins`). This request is a `multipart/form-data` containing:
    * The artifact file (e.g., `my_dataset.zip`).
    * `title` (String).
    * `description` (String).
2.  **`df-manager` - Artifact Upload:**
    * `df-manager` receives the request.
    * It forwards the artifact file to the `artifact-repository` via a POST request to its upload endpoint (e.g., `http://artifact-repository-host/api/v1/artifacts`).
3.  **`artifact-repository` - Stores File & Returns ID:**
    * The `artifact-repository` stores the file and generates a unique artifact ID (e.g., `artifact-xyz-789`).
    * It returns this artifact ID to `df-manager`.
4.  **`df-manager` - RDF Generation:**
    * `df-manager` generates a new UUID for the dataset/plugin (e.g., `ds:uuid-abc-123` or `pl:uuid-def-456`).
    * It constructs an RDF graph for the artifact:
        * Type: `dcat:Dataset` or `df:Plugin` (which is also a `dcat:Resource`).
        * `dcterms:title` and `dcterms:description` from the request.
        * A `dcat:distribution` which includes:
            * `dcat:accessURL`: Constructed using the `artifact-repository`'s fetch endpoint and the `artifactId` (e.g., `http://artifact-repository-host/api/v1/artifacts/artifact-xyz-789/download`).
            * `dcat:compressFormat`: Determined from the uploaded file's extension (e.g., `application/zip`).
5.  **`df-manager` - Metadata Submission:**
    * `df-manager` sends a POST request to the `metadata-store`'s RDF submission endpoint (e.g., `http://metadata-store-host/api/v1/rdf/graph`) with the generated RDF graph (typically as Turtle).
6.  **`metadata-store` - Stores RDF:**
    * The `metadata-store` ingests and stores the RDF graph.
7.  **`df-manager` - Response to External App:**
    * `df-manager` returns a response to the external application, typically including the generated RDF or a success message with the new dataset/plugin URI (e.g., `urn:ds:uuid-abc-123`).

**B. Creating a Pipeline:**

1.  **External Application Request:** An external application sends a `POST` request to `df-manager`'s `/api/v1/pipelines` endpoint with a JSON payload conforming to the structure defined in `manager.md`.
2.  **`df-manager` - Validation & RDF Generation:**
    * `df-manager` receives the JSON configuration.
    * **Validation (Conceptual):**
        * Validates the JSON schema.
        * For each `pluginUuid` referenced in a step, `df-manager` queries the `metadata-store` (e.g., GET `http://metadata-store-host/api/v1/resources/pl:{pluginUuid}`) to ensure the plugin exists.
        * For each `datasetUuid` referenced in an initial input variable, `df-manager` queries the `metadata-store` (e.g., GET `http://metadata-store-host/api/v1/resources/ds:{datasetUuid}`) to ensure the dataset exists.
    * **RDF Generation (as per `manager.md` Section 5):**
        * Generates a new UUID for the pipeline (`pipe:<uuidP>`).
        * Maps JSON variable IDs to `var:<uuidV>` and step IDs to `step:<uuidS>`.
        * Creates RDF for `p-plan:Plan`, `p-plan:Step` (linking to `df:Plugin` via `df:usesPlugin`), and `p-plan:Variable`.
        * Links initial input variables (`var:`) to existing datasets (`ds:`) using `prov:specializationOf`.
        * Links steps to their input/output variables (`p-plan:hasInputVar`, `p-plan:isOutputVarOf`).
        * Adds `p-plan:isPrecededBy` dependencies between steps.
        * Identifies terminal `var:` resources (outputs not consumed by other steps in *this* pipeline).
        * For each terminal variable, generates a new output `dcat:Dataset` (`ds:<outputDsUuid>`) linked via `prov:wasDerivedFrom` (to the `var:`) and `prov:wasGeneratedBy` (to the `pipe:`).
3.  **`df-manager` - Metadata Submission:**
    * `df-manager` sends a POST request to the `metadata-store`'s pipeline submission endpoint (e.g., `http://metadata-store-host/api/v1/pipelines` or a generic RDF submission endpoint) with the complete generated pipeline RDF graph.
4.  **`metadata-store` - Stores RDF:**
    * The `metadata-store` ingests and stores the pipeline RDF graph.
5.  **`df-manager` - Response to External App:**
    * `df-manager` returns a response, typically the generated pipeline RDF or a success message with the new pipeline URI (e.g., `urn:pipe:uuidP-789`).

**C. Retrieving Metadata (Dataset, Plugin, or Pipeline):**

1.  **External Application Request:** An external application sends a `GET` request to `df-manager` (e.g., `/api/v1/datasets/{uuid}`, `/api/v1/plugins/{uuid}`, or `/api/v1/pipelines/{uuid}`).
2.  **`df-manager` - Forward Request:**
    * `df-manager` constructs the full resource URI (e.g., `urn:ds:{uuid}`).
    * It sends a GET request to the `metadata-store`'s resource retrieval endpoint (e.g., `http://metadata-store-host/api/v1/resources/ds:{uuid}`).
3.  **`metadata-store` - Fetches RDF:**
    * The `metadata-store` retrieves the RDF graph for the requested resource.
    * It returns the RDF data (e.g., in Turtle format) to `df-manager`.
4.  **`df-manager` - Response to External App:**
    * `df-manager` forwards the RDF data received from the `metadata-store` to the external application.

### 3\. Example of Usage

Assume `df-manager` is running at `http://localhost:8080`.

**A. Uploading a New Dataset:**

An external application wants to upload a dataset named "Customer Data Q1" which is in a file `customers_q1_archive.zip`.

* **Request (using cURL):**
  ```bash
  curl -X POST http://localhost:8080/api/v1/datasets \
       -F "file=@/path/to/customers_q1_archive.zip" \
       -F "title=Customer Data Q1" \
       -F "description=Customer acquisition data for the first quarter."
  ```
* **`df-manager` Internal Actions:**
    1.  Uploads `customers_q1_archive.zip` to `artifact-repository`, gets back `artifactId=artf_id_001`.
    2.  Generates `datasetUuid=ds_uuid_abc`.
    3.  Generates RDF for `urn:ds:ds_uuid_abc` including `dcterms:title`, `dcterms:description`, and a `dcat:distribution` with `dcat:accessURL` like `http://artifact-repo/api/v1/artifacts/artf_id_001/download` and `dcat:compressFormat "application/zip"`.
    4.  POSTs this RDF to `metadata-store`.
* **Response (from `df-manager` to external app, conceptual):**
  ```json
  {
    "rdfData": "@prefix dcat: <http://www.w3.org/ns/dcat#> .\n@prefix dcterms: <http://purl.org/dc/terms/> .\n@prefix ds: <urn:ds:> .\n\nds:ds_uuid_abc a dcat:Dataset ;\n    dcterms:title \"Customer Data Q1\" ;\n    dcterms:description \"Customer acquisition data for the first quarter.\" ;\n    dcat:distribution [\n        a dcat:Distribution ;\n        dcat:accessURL <http://artifact-repo/api/v1/artifacts/artf_id_001/download> ;\n        dcat:compressFormat <http://www.iana.org/assignments/media-types/application/zip> \n    ] .\n"
  }
  ```
  *(Note: `dcat:compressFormat` might use a direct string or a controlled vocabulary URI. The example uses a common IANA URI for zip).*

**B. Creating a New Pipeline:**

An external application wants to create a pipeline as defined in the `manager.md` example (Section 6).

* **Request (using cURL, with JSON payload from `pipeline_config.json`):**
  ```bash
  # pipeline_config.json contains the JSON from manager.md example
  # {
  #   "title": "Validation & Cleansing Pipeline v3",
  #   "variables": [
  #     { "id": "raw-data-var", "title": "Initial Raw Input", "datasetUuid": "dddddddd-dddd-dddd-dddd-dataset001" },
  #     { "id": "validated-data-var", "title": "Intermediate Validated Data" },
  #     { "id": "cleansed-output-var", "title": "Final Cleansed Output Var" }
  #   ],
  #   "steps": [
  #     { "id": "validate-step", "title": "Validate Raw", "pluginUuid": "pppppppp-pppp-pppp-pppp-plugin001", "inputs":  ["raw-data-var"], "outputs": ["validated-data-var"] },
  #     { "id": "cleanse-step", "title": "Cleanse Data", "pluginUuid": "qqqqqqqq-qqqq-qqqq-qqqq-plugin002", "inputs": ["validated-data-var"], "outputs": ["cleansed-output-var"], "precededBy": ["validate-step"] }
  #   ]
  # }

  curl -X POST http://localhost:8080/api/v1/pipelines \
       -H "Content-Type: application/json" \
       -d @/path/to/pipeline_config.json
  ```
* **`df-manager` Internal Actions:**
    1.  Validates that `ds:dddddddd-dddd-dddd-dddd-dataset001`, `pl:pppppppp-pppp-pppp-pppp-plugin001`, and `pl:qqqqqqqq-qqqq-qqqq-qqqq-plugin002` exist in `metadata-store`.
    2.  Generates RDF for the pipeline, steps, internal variables, and the auto-generated output dataset (`ds:<new_output_uuid>`) as per the logic in `manager.md`.
    3.  POSTs this complete RDF to `metadata-store`.
* **Response (from `df-manager`, conceptual):**
  ```json
  {
    "rdfData": "@prefix pipe: <urn:pipe:> .\n@prefix step: <urn:step:> .\n@prefix var: <urn:var:> .\n@prefix ds: <urn:ds:> .\n@prefix pl: <urn:pl:> .\n@prefix p-plan: <http://purl.org/net/p-plan#> .\n@prefix dcterms: <http://purl.org/dc/terms/> .\n@prefix prov: <http://www.w3.org/ns/prov#> .\n@prefix dcat: <http://www.w3.org/ns/dcat#> .\n@prefix df: <http://localhost:8080/ns/df#> .\n\npipe:uuidP-555 a p-plan:Plan;\n    dcterms:title \"Validation & Cleansing Pipeline v3\".\n\nvar:uuidV-PPP a p-plan:Variable; # ... and so on, similar to manager.md example output RDF ...\n\nds:auto_gen_ds_xyz a dcat:Dataset;\n    dcterms:title \"Final Cleansed Output Var (Output)\";\n    prov:wasDerivedFrom var:uuidV-RRR;\n    prov:wasGeneratedBy pipe:uuidP-555.\n"
  }
  ```

**C. Retrieving a Plugin's Metadata:**

An external application wants to get the metadata for plugin `pl:pppppppp-pppp-pppp-pppp-plugin001`.

* **Request (using cURL):**
  ```bash
  curl -X GET http://localhost:8080/api/v1/plugins/pppppppp-pppp-pppp-pppp-plugin001
  ```
* **`df-manager` Internal Actions:**
    1.  GETs RDF for `urn:pl:pppppppp-pppp-pppp-pppp-plugin001` from `metadata-store`.
* **Response (from `df-manager`, conceptual):**
  ```json
  {
    "rdfData": "@prefix dcat: <http://www.w3.org/ns/dcat#> .\n@prefix dcterms: <http://purl.org/dc/terms/> .\n@prefix df: <http://localhost:8080/ns/df#> .\n@prefix pl: <urn:pl:> .\n\npl:pppppppp-pppp-pppp-pppp-plugin001 a df:Plugin, dcat:Resource ;\n    dcterms:title \"Awesome Validator Plugin\" ;\n    dcterms:description \"Validates input data against a schema.\" ;\n    dcat:distribution [\n        a dcat:Distribution ;\n        dcat:accessURL <http://artifact-repo/api/v1/artifacts/artf_id_plug_002/download> ;\n        dcat:compressFormat <http://www.iana.org/assignments/media-types/application/zip> \n    ] .\n"
  }
  ```