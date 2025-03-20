from fastapi import FastAPI
from app.api.endpoints import file

app = FastAPI(
    title="Artifact Repository API",
    description="API for storing and managing files in the artifact repository using FastAPI and MongoDB.",
    version="1.0.0",
    docs_url="/docs",         
    redoc_url="/redoc",       
    openapi_url="/openapi.json"
)

app.include_router(file.router, prefix="/api/v1/files", tags=["Files"])

@app.get("/")
def read_root():
    return {"message": "Artifact Repository API"}
