# Data Factory

Data Factory is a collection of microservices and command line tools for storing, describing and executing data processing pipelines. The repository contains multiple components which can be started together via Docker Compose.

## Components

| Directory | Description |
|-----------|-------------|
| `artifact-repo` | Spring Boot service providing an API for storing binary artifacts in a MinIO bucket. |
| `metadata-store` | Spring Boot service exposing a REST API backed by an Apache Jena RDF store. It holds metadata for datasets, plugins and pipelines. |
| `df-manager` | Middleware service that offers a unified API and coordinates communication between `artifact-repo` and `metadata-store`. |
| `manager-cli` | Python Typer CLI for uploading datasets, plugins and pipeline definitions to `df-manager`. |
| `executor-cli` | Python Typer CLI that fetches pipeline definitions from the metadata store and builds a combined execution graph. |

Example pipeline templates can be found in `pipeline_template.json` and `dependant_pipeline_template.json`.
Each subproject contains its own README or documentation with more detailed instructions.

## Quick start

1. Ensure Docker and Docker Compose are installed.
2. Copy `.env.template` files if provided and adjust values (see component READMEs for details).
3. Start the stack:

```bash
docker-compose up --build
```

This will start MinIO, `artifact-repo`, `metadata-store` and `df-manager`.

4. Use the `manager-cli` and `executor-cli` to interact with the services

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.