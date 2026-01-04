import requests
import os
import logging

from google.cloud import storage

KEYCLOAK_URL = os.getenv("KEYCLOAK_URL")
KEYCLOAK_REALM = os.getenv("KEYCLOAK_REALM")
KEYCLOAK_CLIENT_ID = os.getenv("KEYCLOAK_CLIENT_ID")
KEYCLOAK_CLIENT_SECRET = os.getenv("KEYCLOAK_CLIENT_SECRET")
KEYCLOAK_TOKEN_URL = f"{KEYCLOAK_URL}/realms/{KEYCLOAK_REALM}/protocol/openid-connect/token"

CONFIRM_FILE_SERVICE_URL = os.getenv("FILE_SERVICE_ADDRESS") + "/file/confirm"

def get_token():
    """Fetch access token from Keycloak using client credentials."""
    data = {
        "grant_type": "client_credentials",
        "client_id": KEYCLOAK_CLIENT_ID,
        "client_secret": KEYCLOAK_CLIENT_SECRET
    }
    
    try:
        res = requests.post(KEYCLOAK_TOKEN_URL, data=data, timeout=10)
        res.raise_for_status()
        return res.json()["access_token"]
    except requests.exceptions.RequestException as e:
        logging.error(f"Token fetch failed: {e}")
        raise

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

def delete_blob(bucket, object_name):
    """Delete blob from GCS bucket."""
    storage_client = storage.Client()
    bucket = storage_client.bucket(bucket)
    blob = bucket.blob(object_name)
    blob.delete()

def on_object_finalize(event, context):
    """Triggered by GCS object finalized event."""
    bucket = event['bucket']
    object_name = event['name']
    
    uuid = extract_uuid(object_name)
    if not uuid:
        logging.warning(f"No UUID found in object name: {object_name}")
        return
    
    try:
        token = get_token()
        url = f"{CONFIRM_FILE_SERVICE_URL}/{uuid}"
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }
        
        res = requests.put(url, headers=headers, timeout=10)
        res.raise_for_status()
        
    except requests.exceptions.RequestException as e:
        logging.error(f"Error confirming file: {e}")
        delete_blob(bucket, object_name)
        raise
    except Exception as e:
        logging.error(f"Unexpected error: {e}")
        delete_blob(bucket, object_name)
        raise