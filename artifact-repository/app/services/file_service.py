from app.core.database import db
from bson import ObjectId

async def save_file(file_info: dict):
    if not file_info.get("filename"):
        raise ValueError("Filename is required")
    if not file_info.get("content"):
        raise ValueError("File content is required")
    result = await db.files.insert_one(file_info)
    return result.inserted_id

async def get_file(file_id: str):
    if not ObjectId.is_valid(file_id):
        raise ValueError("Invalid file id")
    file_doc = await db.files.find_one({"_id": ObjectId(file_id)})
    return file_doc

async def delete_file(file_id: str):
    if not ObjectId.is_valid(file_id):
        raise ValueError("Invalid file id")
    result = await db.files.delete_one({"_id": ObjectId(file_id)})
    return result.deleted_count == 1
