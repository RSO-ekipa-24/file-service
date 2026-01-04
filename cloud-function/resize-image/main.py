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
    name_split = name.split('-')
    if len(name_split) < 5:
        return None
    uuid = "-".join(name_split[0:5])
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
    token = get_access_token()
    url = f"{ADD_LOD_FILE_SERVICE_URL}"
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

def is_already_lod(object_name):
    split = object_name.split('/')
    if len(split) != 4:
        return False
    if split[2] in [LOW_NAME, MEDIUM_NAME, HIGH_NAME]:
        return True
    return False

def generate_and_upload_lod_image(uuid, bucket, image, original_object_name, level_of_detail, max_dim, quality):
    lod_object_name = generate_lod_object_name(original_object_name, level_of_detail)

    if bucket.blob(lod_object_name).exists():
        logging.info(f"{level_of_detail} LOD image already exists for: {original_object_name}, skipping creation.")
    else:
        data = resize_image(image, max_dim, quality)
        blob = bucket.blob(lod_object_name)
        blob.upload_from_string(data, content_type='image/webp')

        try:
            notify_file_service(uuid, level_of_detail, lod_object_name, len(data))
        except Exception as e:
            logging.error(f"Failed to notify file service for {level_of_detail} LOD - Deleting the object... Error: {e}")
            blob_delete = bucket.blob(lod_object_name)
            blob_delete.delete()
            raise
        del data

def on_object_finalize(event, context):
    """Triggered by GCS object finalized event."""
    bucket_name = event['bucket']
    object_name = event['name']

    if is_already_lod(object_name):
        return

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
    
    image = Image.open(io.BytesIO(data)).convert("RGB")
    
    uuid = extract_uuid(object_name)
    if uuid is None:
        logging.error(f"Cloud not extract UUID from: {object_name}")
        return
    
    generate_and_upload_lod_image(uuid, bucket, image, object_name, LOW_NAME, LOW_MAX_DIM, LOW_QUALITY)

    generate_and_upload_lod_image(uuid, bucket, image, object_name, MEDIUM_NAME, MEDIUM_MAX_DIM, MEDIUM_QUALITY)

    generate_and_upload_lod_image(uuid, bucket, image, object_name, HIGH_NAME, HIGH_MAX_DIM, HIGH_QUALITY)

    logging.info(f"Successfully created {LOW_NAME}, {MEDIUM_NAME}, and {HIGH_NAME} Levels of Detail images for: {object_name}")

