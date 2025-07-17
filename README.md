# Data Factory

Data Factory is a collection of microservices and command line tools for storing, describing and executing data processing pipelines. The repository contains multiple components which can be started together via Docker Compose.

## Components

| Directory | Description |
|-----------|-------------|
| `artifact-repo` | Spring Boot service providing an API for storing binary artifacts in a MinIO bucket. |
| `metadata-store` | Spring Boot service exposing a REST API backed by an Apache Jena RDF store. It holds metadata for datasets, plugins and pipelines. |
| `df-manager` | Middleware service that offers a unified API and coordinates communication between `artifact-repo` and `metadata-store`. |
| `manager-cli` | Python Typer CLI for uploading datasets, plugins and pipeline definitions to `df-manager`. |
| `executor-cli` | Python Typer CLI that fetches pipeline definitions through `df-manager`, fetches distributions from `artifact-repo` and builds/executes/visualizes an execution graph. |

Each subproject contains its own README or documentation with more detailed instructions.

## Getting Started

### Part 1: Deployment

This section covers the deployment of the core backend services using Docker.

#### **Prerequisites**

  * **Docker** and **Docker Compose**

#### **Deployment Guide**

1.  **Clone the Repository**
    First, obtain the project source code from its GitHub repository.

    ```bash
    git clone git@github.com:YArzumanyan/data-factory.git
    cd data-factory
    ```

2.  **Configure the Environment**
    The system is configured using environment variables. It's best to create a `.env` file in the project root. You can copy the template to get started.

    ```bash
    cp .env.template .env
    ```

    Open the `.env` file and configure the key variables, such as `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY` and others as needed.

3.  **Start the Platform**
    From the root directory of the project, run the following command to build and start all services in detached mode.

    ```bash
    docker-compose up --build -d
    ```

4.  **Verify Deployment**
    Check that all service containers are running correctly.

    ```bash
    docker ps
    ```

    You should see four running containers: `minio`, `Artifact Repository`, `Metadata Store`, and `df-manager`. The services will be accessible on the following ports:

      * **DF-Manager**: Port `8083`
      * **Metadata Store**: Port `8082`
      * **Artifact Repository**: Port `8081`
      * **MinIO API / Console**: Ports `9000` / `9001`

### **Part 2: Usage**

Once the backend is deployed, you can use the CLI tools to manage and run your pipelines.

#### Prerequisites

* **Install `libmagic`**: This library is required for `python-magic` to correctly identify file types.
  * **Windows**: Modify the `executor-cli/requirements.txt` file. Change the line `python-magic` to `python-magic-bin`.
  * **Linux (Debian/Ubuntu)**:
    ```bash
    sudo apt install libmagic-dev
    ```
  * **macOS (Homebrew)**:
    ```bash
    brew install libmagic
    ```
* **Install Python Packages**: Install the required packages for both command-line interfaces.
    ```bash
    pip install -r manager-cli/requirements.txt
    pip install -r executor-cli/requirements.txt
    ```
* **Configure Environment**: Create a `.env` file in both the `manager-cli` and `executor-cli` directories. You can copy the contents from the `.env.template` file provided in each directory.

#### 1. Upload a Dataset

First, upload a dataset to work with. This command registers your dataset and returns a unique ID (`DATASET_UUID`).

```bash
python manager-cli/manager-cli.py dataset manager-cli/example/MOCK_DATA_1.csv --title "Article" --description "A one-liner article"
```

**Note:** Keep track of the `DATASET_UUID` returned by this command.

### 2. Upload Plugins

Next, upload the necessary plugins for your pipeline. Each command will return a unique `PLUGIN_UUID`.

  * **Validator Plugin**:
    ```bash
    python manager-cli/manager-cli.py plugin manager-cli/example/validator.zip --title "Validator" --description "This plugin checks if text contains the word 'quick'"
    ```
  * **Reverser Plugin**:
    ```bash
    python manager-cli/manager-cli.py plugin manager-cli/example/reverser.zip --title "Reverser" --description "This plugin reverses text"
    ```

**Note:** Keep track of the `PLUGIN_UUID`s returned by these commands.

### 3. Create and Upload the Pipeline

With your dataset and plugins uploaded, you can now define and upload your pipeline.

  * **Edit the Pipeline Template**: Open `manager-cli/example/pipeline_template.json` and replace the placeholder `DATASET_UUID` and `PLUGIN_UUID`s with the actual IDs you received in the previous steps.
  * **Upload the Pipeline Definition**:
    ```bash
    python manager-cli/manager-cli.py pipeline manager-cli/example/pipeline_template.json
    ```

**Note:** This command will return a `PIPELINE_UUID`. You'll need it for the next steps.

### 4. Visualize and Execute the Pipeline

You are now ready to run your pipeline.

  * **Visualize the Pipeline (Optional)**: You can generate a visual graph of your pipeline to check its structure.
    ```bash
    # Replace PIPELINE_UUID with the actual UUID from the previous step
    python executor-cli/executor-cli.py visualize PIPELINE_UUID
    ```
  * **Execute the Pipeline**: Run the pipeline to process your data.
    ```bash
    # Replace PIPELINE_UUID with the actual UUID
    python executor-cli/executor-cli.py execute PIPELINE_UUID
    ```

### 5. View the Results

Once the execution is complete, you can find the output in the results directory, which is defined in your `executor-cli/.env` file (look for the `MAIN_WORKSPACE`/results path).

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.