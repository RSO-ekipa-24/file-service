import os
import sys
import subprocess
from pathlib import Path


from load_env import load_env_file
from check_command import check_command

# -------------------------
# Load .env
# -------------------------
load_env_file(env_file=".env")

gcp_project = os.getenv("GCP_PROJECT_ID")
region = os.getenv("GCP_REGION")
serverless_vpc_connector_name = os.getenv("GCP_SERVERLESS_VPC_CONNECTOR_NAME", "files-cloud-run-connector")
private_bucket = os.getenv("GCP_PRIVATE_BUCKET")
public_bucket = os.getenv("GCP_PUBLIC_BUCKET")
kc_token_url = os.getenv("KC_TOKEN_URL")
kc_client_id = os.getenv("KC_CLIENT_ID")
kc_client_secret = os.getenv("KC_CLIENT_SECRET")
confirm_file_service_url = os.getenv("CONFIRM_FILE_SERVICE_URL")

# -------------------------
# Detect gcloud/gsutil paths
# -------------------------
GCLOUD = check_command("gcloud")
GSUTIL = check_command("gsutil")

# -------------------------
# Enable APIs
# -------------------------
print("Enable required GCP APIs...")
subprocess.run(
    [
        GCLOUD, "services", "enable",
        "cloudfunctions.googleapis.com",
        "eventarc.googleapis.com",
        "run.googleapis.com",
        "storage.googleapis.com",
        "vpcaccess.googleapis.com",
        "pubsub.googleapis.com",
        "--project", gcp_project
    ],
    check=True
)
print()

# -------------------------
# Grant Eventarc permissions
# -------------------------
print("Grant Eventarc permissions to access private bucket...")
project_number = subprocess.run(
    [
        GCLOUD, "projects", "describe", gcp_project,
        "--format=value(projectNumber)"
    ], 
    capture_output=True,
    text=True,
    check=True
).stdout.strip()

eventarc_sa = f"service-{project_number}@gcp-sa-eventarc.iam.gserviceaccount.com"

subprocess.run(
    [
        GSUTIL, "iam", "ch",
        f"serviceAccount:{eventarc_sa}:objectViewer",
        f"gs://{private_bucket}"
    ],
    check=True
)
print()

# -------------------------
# Grant Cloud Storage service account Pub/Sub permissions
# -------------------------
print("Grant Cloud Storage service account Pub/Sub publisher role...")
gcs_sa = f"service-{project_number}@gs-project-accounts.iam.gserviceaccount.com"

subprocess.run(
    [
        GCLOUD, "projects", "add-iam-policy-binding", gcp_project,
        "--member", f"serviceAccount:{gcs_sa}",
        "--role", "roles/pubsub.publisher"
    ],
    check=True
)
print()

# -------------------------
# Create Serverless VPC Access Connector - to access GKE services from Cloud Functions
# -------------------------
print("Create Serverless VPC Access Connector...")
result = subprocess.run(
    [
        GCLOUD, "compute", "networks", "vpc-access", "connectors", "describe", serverless_vpc_connector_name,
        "--region", region,
        "--project", gcp_project
    ],
    capture_output=True,
    text=True
)

if result.returncode != 0:
    subprocess.run(
        [
            GCLOUD, "compute", "networks", "vpc-access", "connectors", "create", serverless_vpc_connector_name,
            "--region", region,
            "--network", "default",
        "--range", "10.0.0.0/28",
    ],
    check=True
)
else:
    print(f"Serverless VPC Access Connector '{serverless_vpc_connector_name}' already exists.")
    
print()

# -------------------------
# Deploy confirm-upload Cloud Functions
# -------------------------
print("Deploy confirm-upload-private Cloud Function...")
subprocess.run(
    [
        GCLOUD, "functions", "deploy", "confirm-upload-private",
        "--gen2",
        "--runtime", "python312",
        "--region", region,
        "--source", str(Path(__file__).parent.parent / "cloud-function/confirm-upload"),
        "--entry-point", "on_object_finalize",
        "--trigger-event", "google.cloud.storage.object.v1.finalized",
        "--trigger-resource", private_bucket,
        "--vpc-connector", serverless_vpc_connector_name,
        "--egress-settings", "private-ranges-only",
        "--set-env-vars",
        f"KC_TOKEN_URL={kc_token_url},KC_CLIENT_ID={kc_client_id},KC_CLIENT_SECRET={kc_client_secret},CONFIRM_FILE_SERVICE_URL={confirm_file_service_url}",
        "--project", gcp_project
    ],
    check=True
)
print()

print("Deploy confirm-upload-public Cloud Function...")
subprocess.run(
    [
        GCLOUD, "functions", "deploy", "confirm-upload-public",
        "--gen2",
        "--runtime", "python312",
        "--region", region,
        "--source", str(Path(__file__).parent.parent / "cloud-function/confirm-upload"),
        "--entry-point", "on_object_finalize",
        "--trigger-event", "google.cloud.storage.object.v1.finalized",
        "--trigger-resource", public_bucket,
        "--vpc-connector", serverless_vpc_connector_name,
        "--egress-settings", "private-ranges-only",
        "--set-env-vars",
        f"KC_TOKEN_URL={kc_token_url},KC_CLIENT_ID={kc_client_id},KC_CLIENT_SECRET={kc_client_secret},CONFIRM_FILE_SERVICE_URL={confirm_file_service_url}",
        "--project", gcp_project
    ],
    check=True
)
print("Deployment confirm-upload completed.")
print()
