from fastapi import FastAPI, UploadFile, File, HTTPException, Path
from fastapi.responses import FileResponse
from pydantic import BaseModel
from typing import List
import os
import uvicorn
from uuid import uuid4

class ArtifactResponse(BaseModel):
    artifact_id: str

class ListArtifactsResponse(BaseModel):
    artifacts: List[str]

class MessageResponse(BaseModel):
    detail: str

app = FastAPI(
    title="Artifact Repository API",
    description=(
        "A minimal artifact repository that stores files using their UUID as the filename. "
        "This API supports uploading, retrieving, listing, and deleting artifacts. "
        "Each artifact is stored as an independent file without any additional metadata."
    ),
    version="1.0.0"
)

ARTIFACTS_DIR = os.getenv("ARTIFACTS_DIR", "artifacts")
os.makedirs(ARTIFACTS_DIR, exist_ok=True)

@app.post("/artifacts", response_model=ArtifactResponse, summary="Upload Artifact", 
          response_description="Returns the UUID for the uploaded artifact.")
async def upload_artifact(file: UploadFile = File(...)):
    """
    Upload a file as an artifact.

    This endpoint accepts a file upload, generates a unique UUID, and stores the file on disk
    with the UUID as its filename.

    - **file**: The file to be uploaded.

    Returns:
      - **artifact_id**: A unique identifier (UUID) assigned to the uploaded artifact.
    """
    artifact_id = str(uuid4())
    file_path = os.path.join(ARTIFACTS_DIR, artifact_id)
    
    with open(file_path, "wb") as f:
        content = await file.read()
        f.write(content)
    
    return ArtifactResponse(artifact_id=artifact_id)

@app.get("/artifacts/{artifact_id}", summary="Retrieve Artifact", 
         response_description="The artifact file is returned.", response_class=FileResponse)
async def get_artifact(
    artifact_id: str = Path(..., description="The unique identifier of the artifact to retrieve.")
):
    """
    Retrieve a stored artifact by its UUID.

    This endpoint fetches the file associated with the provided UUID.

    - **artifact_id**: The unique identifier of the artifact.

    Returns:
      - The artifact file if it exists, otherwise a 404 error.
    """
    file_path = os.path.join(ARTIFACTS_DIR, artifact_id)
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="Artifact not found")
    return FileResponse(file_path, filename=artifact_id)

@app.get("/artifacts", response_model=ListArtifactsResponse, summary="List Artifacts", 
         response_description="A list of all stored artifact UUIDs.")
async def list_artifacts():
    """
    List all stored artifacts.

    This endpoint scans the storage directory and returns a list of all artifact identifiers (UUIDs).

    Returns:
      - **artifacts**: A list of artifact UUIDs.
    """
    artifact_ids = os.listdir(ARTIFACTS_DIR)
    return ListArtifactsResponse(artifacts=artifact_ids)

@app.delete("/artifacts/{artifact_id}", response_model=MessageResponse, summary="Delete Artifact", 
            response_description="Confirms deletion of the artifact.")
async def delete_artifact(
    artifact_id: str = Path(..., description="The unique identifier of the artifact to delete.")
):
    """
    Delete a stored artifact by its UUID.

    This endpoint removes the file associated with the given UUID from the disk.

    - **artifact_id**: The unique identifier of the artifact to be deleted.

    Returns:
      - A confirmation message if the deletion was successful.
    """
    file_path = os.path.join(ARTIFACTS_DIR, artifact_id)
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="Artifact not found")
    os.remove(file_path)
    return MessageResponse(detail="Artifact deleted")

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
