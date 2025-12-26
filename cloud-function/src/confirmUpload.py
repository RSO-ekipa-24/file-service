import functions_framework
import requests
import os
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

KC_TOKEN_URL = os.getenv("KC_TOKEN_URL")
KC_CLIENT_ID = os.getenv("KC_CLIENT_ID")
KC_CLIENT_SECRET = os.getenv("KC_CLIENT_SECRET")
CONFIRM_BASE_URL = os.getenv("CONFIRM_BASE_URL")

def get_token():
    """Fetch access token from Keycloak using client credentials."""
    data = {
        "grant_type": "client_credentials",
        "client_id": KC_CLIENT_ID,
        "client_secret": KC_CLIENT_SECRET
    }
    
    try:
        res = requests.post(KC_TOKEN_URL, data=data, timeout=10)
        res.raise_for_status()
        return res.json()["access_token"]
    except requests.exceptions.RequestException as e:
        logger.error(f"Token fetch failed: {e}")
        raise

def extract_uuid(name):
    """Extract UUID from object name."""
    """Object name: folder/user/<uuid>-filename.ext"""

    split_slash = name.split('/')
    if len(split_slash) == 0:
        return None
    name = split_slash[-1]
    uuid_end_index = name.rfind('-')
    if uuid_end_index == -1:
        return None
    uuid = name[0:uuid_end_index]
    return uuid

@functions_framework.cloud_event
def on_finalize(cloud_event):
    """Triggered by GCS object finalized event."""
    data = cloud_event.data
    bucket = data.get("bucket")
    name = data.get("name")
    
    logger.info(f"Processing file: {bucket}/{name}")
    
    uuid = extract_uuid(name)
    if not uuid:
        logger.warning(f"No UUID found in object name: {name}")
        return
    
    try:
        token = get_token()
        url = f"{CONFIRM_BASE_URL}/{uuid}"
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }
        
        logger.info(f"Calling: {url}")
        res = requests.post(url, headers=headers, timeout=10)
        res.raise_for_status()
        
        logger.info(f"File {uuid} confirmed successfully")
    except requests.exceptions.RequestException as e:
        logger.error(f"Error confirming file: {e}")
        raise
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        raise