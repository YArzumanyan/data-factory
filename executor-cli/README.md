# Data Factory Executor CLI

Executor CLI is a utility tool used to generate an executable Python script (the executor) from pipeline metadata. The executor script is responsible for setting up a Docker container, fetching required artifacts from the repository, and executing the pipeline as defined by its metadata.

---

## 1. CLI Overview

```
Usage: executor <COMMAND> [OPTIONS]
```

**Primary Command Group:**
- **generate** – Generates an executor script for a specified pipeline (with an optional version).
- **help** – For command usage details.

---

## 2. Executor Command

### a. Generate Executor Script

Generate an executable Python script that acts as the executor for a pipeline. The generated script performs the following actions:
- **Fetch Pipeline Metadata:**  
  Connects to the metadata API (using settings from the global configuration file) to retrieve the pipeline details. If a pipeline version is specified, that version is used; otherwise, the default or latest version is retrieved.
  
- **Generate Executor Script:**  
  Translates the fetched metadata into an executable Python script that:
  - Sets up and configures a Docker container to provide a consistent runtime environment.
  - Retrieves necessary artifacts (such as scripts and datasets) from the artifact repository.
  - Executes the pipeline according to the provided metadata.

- **Save the Script:**  
  Writes the generated script to the specified output file. If the file already exists, the command issues an error unless the `--force` option is provided.

```
Usage: executor generate <pipeline-id> [-v <version>] [-o <output-path>] [--force] [--verbose]
```

**Example:**

```bash
executor generate 123e4567-e89b-12d3-a456-426614174000 -v 2.0 -o executor_script.py --verbose
```

*What It Does:*
1. **Fetch Pipeline Metadata:**  
   - Connects to the metadata API using the global configuration.
   - Retrieves metadata for the specified pipeline. If a version is provided, that version is used; otherwise, the default or latest version is fetched.

2. **Generate Executor Script:**  
   - Converts the pipeline metadata into an executable Python script.
   - Embeds logic to create and configure a Docker container.
   - Includes functionality to download required artifacts from the artifact repository.
   - Integrates pipeline execution logic based on the retrieved metadata.

3. **Output:**  
   - Saves the generated script to the specified output file.
   - If the target file exists and the `--force` flag is not used, the command will report an error to prevent accidental overwrites.

---

## 3. Configuration

The Executor CLI automatically reads its global settings from a JSON configuration file. This file contains essential configuration details such as:
- API endpoint for fetching pipeline metadata.
- Artifact repository URL.
- Docker-specific configuration options.

By default, the configuration file is stored in a dedicated folder within the user's home directory. If the environment variable `DATA_FACTORY_EXECUTOR_HOME` is set, that directory is used instead. Users must update this JSON file directly to modify system-wide settings.

---

## 4. Help and Documentation

For additional details on any command, use the help command:

```
Usage: executor help <COMMAND>
```

**Example:**

```bash
executor help generate
```

---

## 5. Full Workflow Example

1. **Prepare Pipeline Metadata**  
   Ensure that the pipeline metadata is available via the metadata API. The metadata should include all necessary details (e.g., artifact references, environment settings) for pipeline execution.

2. **Generate the Executor Script**  
   Use the `generate` command to fetch the metadata and create the executor script:

   ```bash
   executor generate 123e4567-e89b-12d3-a456-426614174000 -v 2.0 -o executor_script.py
   ```

   - This command retrieves the metadata for the specified pipeline (and version) and generates a Python script (`executor_script.py`) that is capable of setting up a Docker container, fetching necessary artifacts, and executing the pipeline.

3. **Review the Generated Script**  
   Open the generated script to verify that it includes the required logic for containerization, artifact retrieval, and pipeline execution.

4. **Execute the Generated Script**  
   Run the generated executor script as you would any other Python script:

   ```bash
   python executor_script.py
   ```

   The script will:
   - Set up a Docker container.
   - Retrieve the required artifacts.
   - Execute the pipeline as defined by its metadata.