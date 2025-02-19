# Artifact Repository API

A minimal artifact repository that stores files using their UUID as the filename. This API supports uploading, retrieving, listing, and deleting artifacts. The storage location is configurable via an environment variable, allowing you to store files on mounted directory.

## Features

- **Upload Artifacts:** Accept file uploads and store them with a unique UUID as the filename.
- **Retrieve Artifacts:** Fetch stored artifacts by their UUID.
- **List Artifacts:** List all artifact UUIDs currently stored.
- **Delete Artifacts:** Remove stored artifacts using their UUID.
- **Configurable Storage Location:** Set the artifacts directory using the `ARTIFACTS_DIR` environment variable.
- **API Documentation:** Automatically generated OpenAPI docs available at `/docs` and `/redoc`.

## Requirements

- Dependencies as listed in `requirements.txt`:
  - `fastapi`
  - `uvicorn[standard]`
  - `python-multipart`

## Installation

1. **Clone the Repository:**

   ```bash
   git clone https://github.com/YArzumanyan/data-factory-artifact-repository.git
   cd data-factory-artifact-repository
   ```

2. **Install Dependencies:**

   ```bash
   pip install -r requirements.txt
   ```

## Running the Application

### Locally

Run the application using Uvicorn:

```bash
uvicorn main:app --host 0.0.0.0 --port 8000
```

The API will be available at [http://localhost:8000](http://localhost:8000).

### Using Docker

1. **Build the Docker Image:**

   ```bash
   docker build -t artifact-repository .
   ```

2. **Run the Docker Container:**

   You can optionally set the `ARTIFACTS_DIR` environment variable to point to a remote or mounted directory:

   ```bash
   docker run -d -p 8000:8000 -e ARTIFACTS_DIR=/path/to/artifacts artifact-repository
   ```

## OpenAPI Documentation

Once the application is running, you can access it at:

- **Swagger UI:** [http://localhost:8000/docs](http://localhost:8000/docs)
- **ReDoc:** [http://localhost:8000/redoc](http://localhost:8000/redoc)