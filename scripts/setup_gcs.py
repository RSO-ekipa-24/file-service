#!/usr/bin/env python3
"""
Idempotent script to create and configure GCS buckets.
Works on Windows, macOS, and Linux.
"""

import os
import sys
import subprocess
from pathlib import Path

from load_env import load_env_file
from check_command import check_command

# -------------------------
# Detect gcloud/gsutil paths
# -------------------------
GCLOUD = check_command("gcloud")
GSUTIL = check_command("gsutil")

# -------------------------
# Load .env
# -------------------------
load_env_file(env_file=".env")

# -------------------------
# Validation
# -------------------------
required_vars = [
    "GCP_PROJECT_ID",
    "GCP_REGION",
    "GCP_PUBLIC_BUCKET",
    "GCP_PRIVATE_BUCKET",
    "GCP_SERVICE_ACCOUNT"
]

for var in required_vars:
    if not os.getenv(var):
        print(f"Error: Missing env var: {var}")
        sys.exit(1)

# Set GCP project
subprocess.run(
    [GCLOUD, "config", "set", "project", os.getenv("GCP_PROJECT_ID")],
    stdout=subprocess.DEVNULL,
    check=True
)

# -------------------------
# Helpers
# -------------------------
def bucket_exists(bucket):
    """Check if a GCS bucket exists."""
    result = subprocess.run(
        [GSUTIL, "ls", "-b", f"gs://{bucket}"],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL
    )
    return result.returncode == 0

# -------------------------
# Create buckets (idempotent)
# -------------------------
if not bucket_exists(os.getenv("GCP_PUBLIC_BUCKET")):
    print(f"Creating public bucket: {os.getenv('GCP_PUBLIC_BUCKET')}")
    subprocess.run([
        GSUTIL, "mb",
        "-p", os.getenv("GCP_PROJECT_ID"),
        "-l", os.getenv("GCP_REGION"),
        "-c", "STANDARD",
        "-b", "on",
        f"gs://{os.getenv('GCP_PUBLIC_BUCKET')}"
    ], check=True)

if not bucket_exists(os.getenv("GCP_PRIVATE_BUCKET")):
    print(f"Creating private bucket: {os.getenv('GCP_PRIVATE_BUCKET')}")
    subprocess.run([
        GSUTIL, "mb",
        "-p", os.getenv("GCP_PROJECT_ID"),
        "-l", os.getenv("GCP_REGION"),
        "-c", "STANDARD",
        "-b", "on",
        f"gs://{os.getenv('GCP_PRIVATE_BUCKET')}"
    ], check=True)

# -------------------------
# Public bucket settings
# -------------------------
print(f"Configuring public bucket permissions: {os.getenv('GCP_PUBLIC_BUCKET')}")
subprocess.run([
    GSUTIL, "iam", "ch",
    "allUsers:objectViewer",
    f"gs://{os.getenv('GCP_PUBLIC_BUCKET')}"
], stdout=subprocess.DEVNULL, check=True)

# -------------------------
# Private bucket settings
# -------------------------
print(f"Enforcing Uniform Bucket-Level Access: {os.getenv('GCP_PRIVATE_BUCKET')}")
subprocess.run([
    GSUTIL, "pap", "set", "enforced",
    f"gs://{os.getenv('GCP_PRIVATE_BUCKET')}"
], stdout=subprocess.DEVNULL, check=True)

# -------------------------
# Service account access
# -------------------------
print(f"Granting service account access: {os.getenv('GCP_SERVICE_ACCOUNT')}")

subprocess.run([
    GSUTIL, "iam", "ch",
    f"serviceAccount:{os.getenv('GCP_SERVICE_ACCOUNT')}:objectAdmin",
    f"gs://{os.getenv('GCP_PUBLIC_BUCKET')}"
], stdout=subprocess.DEVNULL, check=True)

subprocess.run([
    GSUTIL, "iam", "ch",
    f"serviceAccount:{os.getenv('GCP_SERVICE_ACCOUNT')}:objectAdmin",
    f"gs://{os.getenv('GCP_PRIVATE_BUCKET')}"
], stdout=subprocess.DEVNULL, check=True)

print("GCS setup complete")