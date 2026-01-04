import logging
import requests
import os
import io
import time

from google.cloud import storage
from PIL import Image

LOW_MAX_DIM = 480
LOW_QUALITY = 60
LOW_NAME = "LOW"

MEDIUM_MAX_DIM = 960
MEDIUM_QUALITY = 75
MEDIUM_NAME = "MEDIUM"

HIGH_MAX_DIM = 1920
HIGH_QUALITY = 85
HIGH_NAME = "HIGH"

KEYCLOAK_URL = os.environ["KEYCLOAK_URL"]
KEYCLOAK_REALM = os.environ["KEYCLOAK_REALM"]
KEYCLOAK_CLIENT_ID = os.environ["KEYCLOAK_CLIENT_ID"]
KEYCLOAK_CLIENT_SECRET = os.environ["KEYCLOAK_CLIENT_SECRET"]

KEYCLOAK_TOKEN_URL = f"{KEYCLOAK_URL}/realms/{KEYCLOAK_REALM}/protocol/openid-connect/token"

ADD_LOD_FILE_SERVICE_URL = os.environ["FILE_SERVICE_ADDRESS"] + "/image/add-level-of-detail"


_token_cache = None
_token_expiry = 0


def get_access_token() -> str:
    """Fetch access token from Keycloak using client credentials."""
    global _token_cache, _token_expiry

    now = time.time()
    if _token_cache and now < _token_expiry - 30:
        return _token_cache

    resp = requests.post(
        KEYCLOAK_TOKEN_URL,
        data={
            "grant_type": "client_credentials",
            "client_id": KEYCLOAK_CLIENT_ID,
            "client_secret": KEYCLOAK_CLIENT_SECRET,
        },
        timeout=5,
    )

    resp.raise_for_status()
    data = resp.json()

    _token_cache = data["access_token"]
    _token_expiry = now + data.get("expires_in", 60)

    return _token_cache


def extract_uuid(name):
    """Extract UUID from object name."""
    """Object name-private: folder/user/<uuid>-filename.ext"""
    """Object name-public: folder/user/lod/<uuid>-filename.ext"""

    split_slash = name.split('/')
    if len(split_slash) == 0:
        return None
    name = split_slash[-1]
    uuid_end_index = name.rfind('-')
    if uuid_end_index == -1:
        return None
    uuid = name[0:uuid_end_index]
    return uuid

def generate_lod_object_name(original_name, level_of_detail):
    """Generate object name for level of detail image."""
    """Object name: folder/user/lod/<uuid>-filename.ext"""
    
    split = original_name.split('/')
    if len(split) != 4:
        logging.error(f"Failed to generate {level_of_detail} LOD object name for: {original_name}, invalid format.")
        return None
    
    lod_name = f"{split[0]}/{split[1]}/{level_of_detail}/{split[3]}"
    return lod_name

def is_image(bytes):
    """Check if the file is a valid image."""
    try: 
        with Image.open(io.BytesIO(bytes)) as img:
            img.verify()
        return True
    except Exception as e:
        logging.warning(f"File has image content type but is not a valid image: {e}")
        return False

def resize_image(img, max_dim, quality, format="WEBP"):
    """Resize image to fit within max_dim while maintaining aspect ratio."""
    w, h = img.size
    scale = min(max_dim / max(w, h), 1.0)
    new_size = (int(w * scale), int(h * scale))
    resized = img.resize(new_size, Image.LANCZOS)
    out = io.BytesIO()
    resized.save(out, format=format, quality=quality, method=6)
    return out.getvalue()
    
def notify_file_service(uuid, level_of_detail, object_name, file_size):
    """Notify file service about new level of detail image."""
    token = get_access_token()
    url = f"{ADD_LOD_FILE_SERVICE_URL}/{uuid}"
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json" 
    }
    payload = {
        "id": uuid,
        "levelOfDetail": level_of_detail,
        "objectName": object_name,
        "size": file_size
    }

    res = requests.post(url, headers=headers, json=payload, timeout=10)
    res.raise_for_status()


def on_object_finalize(event, context):
    """Triggered by GCS object finalized event."""
    bucket_name = event['bucket']
    object_name = event['name']

    storage_client = storage.Client()
    bucket = storage_client.bucket(bucket_name)
    blob = bucket.blob(object_name)
    
    if blob.content_type is not None:
        if not blob.content_type.startswith('image/'):
            return
        
    data = blob.download_as_bytes()
    
    try:
        image = Image.open(io.BytesIO(data)).convert("RGB")
        image.verify()
    except Exception as e:
        logging.warning(f"File has image content type but is not a valid image: {e}")
        return
    
    uuid = extract_uuid(object_name)
    if uuid is None:
        logging.error(f"Cloud not extract UUID from: {object_name}")
        return
    
    # LOW
    object_name_LOW = generate_lod_object_name(object_name, LOW_NAME)

    if bucket.blob(object_name_LOW).exists():
        logging.info(f"LOW LOD image already exists for: {object_name}, skipping creation.")
    else:
        data_LOW = resize_image(image, LOW_MAX_DIM, LOW_QUALITY)
        blob_LOW = bucket.blob(object_name_LOW)
        blob_LOW.upload_from_string(data_LOW, content_type='image/webp')

        try:
            notify_file_service(uuid, LOW_NAME, object_name_LOW, len(data_LOW))
        except Exception as e:
            logging.error(f"Failed to notify file service for LOW LOD - Deleting the object... Error: {e}")
            blob_LOW.delete()
            return
        del data_LOW

    # MEDIUM
    object_name_MEDIUM = generate_lod_object_name(object_name, MEDIUM_NAME)

    if bucket.blob(object_name_MEDIUM).exists():
        logging.info(f"MEDIUM LOD image already exists for: {object_name}, skipping creation.")
    else:
        data_MEDIUM = resize_image(image, MEDIUM_MAX_DIM, MEDIUM_QUALITY)
        blob_MEDIUM = bucket.blob(object_name_MEDIUM)
        blob_MEDIUM.upload_from_string(data_MEDIUM, content_type='image/webp')

        try:
            notify_file_service(uuid, MEDIUM_NAME, object_name_MEDIUM, len(data_MEDIUM))
        except Exception as e:
            logging.error(f"Failed to notify file service for MEDIUM LOD - Deleting the object... Error: {e}")
            blob_MEDIUM.delete()
            return
        del data_MEDIUM

    # HIGH
    object_name_HIGH = generate_lod_object_name(object_name, HIGH_NAME)

    if bucket.blob(object_name_HIGH).exists():
        logging.info(f"HIGH LOD image already exists for: {object_name}, skipping creation.")
    else:
        data_HIGH = resize_image(image, HIGH_MAX_DIM, HIGH_QUALITY)    
        blob_HIGH = bucket.blob(object_name_HIGH)
        blob_HIGH.upload_from_string(data_HIGH, content_type='image/webp')

        try:
            notify_file_service(uuid, HIGH_NAME, object_name_HIGH, len(data_HIGH))
        except Exception as e:
            logging.error(f"Failed to notify file service for HIGH LOD - Deleting the object... Error: {e}")
            blob_HIGH.delete()
            return
        del data_HIGH

    logging.info(f"Successfully created all Levels of Detail images for: {object_name}")

