from pydantic import BaseModel
from typing import Optional

class FileSchema(BaseModel):
    id: Optional[str]
    filename: str
    content_type: Optional[str] = None

    class Config:
        orm_mode = True
