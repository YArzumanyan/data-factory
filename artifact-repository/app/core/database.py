from app.config import MONGODB_URL, DATABASE_NAME
from motor.motor_asyncio import AsyncIOMotorClient

client = AsyncIOMotorClient(MONGODB_URL)
db = client[DATABASE_NAME]
