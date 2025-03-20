from fastapi import APIRouter, UploadFile, File, HTTPException, status
from fastapi.responses import Response
from app.services.file_service import save_file, get_file, delete_file

router = APIRouter()

@router.post("/", response_model=dict, status_code=status.HTTP_201_CREATED)
async def upload_file_endpoint(uploaded_file: UploadFile = File(...)):
    file_data = await uploaded_file.read()
    file_info = {
        "filename": uploaded_file.filename,
        "content_type": uploaded_file.content_type,
        "content": file_data
    }
    file_id = await save_file(file_info)
    return {
        "id": str(file_id),
        "filename": uploaded_file.filename,
        "content_type": uploaded_file.content_type
    }

@router.get("/{file_id}")
async def download_file_endpoint(file_id: str):
    try:
        file_doc = await get_file(file_id)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    
    if not file_doc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="File not found")
    return Response(content=file_doc["content"], media_type=file_doc.get("content_type", "application/octet-stream"))

@router.delete("/{file_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_file_endpoint(file_id: str):
    deleted = await delete_file(file_id)
    if not deleted:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="File not found")
    return {"detail": "File deleted"}
