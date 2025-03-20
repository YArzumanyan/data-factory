from pydantic import BaseModel, Field
from typing import Optional
from bson import ObjectId

class PyObjectId(ObjectId):
    @classmethod
    def __get_validators__(cls):
         yield cls.validate

    @classmethod
    def validate(cls, v):
         if not ObjectId.is_valid(v):
              raise ValueError("Invalid objectid")
         return ObjectId(v)

    @classmethod
    def __modify_schema__(cls, field_schema):
         field_schema.update(type="string")

class FileModel(BaseModel):
    id: PyObjectId = Field(default_factory=PyObjectId, alias="_id")
    filename: str
    content_type: Optional[str] = None
    content: bytes

    class Config:
         allow_population_by_field_name = True
         json_encoders = {ObjectId: str}
