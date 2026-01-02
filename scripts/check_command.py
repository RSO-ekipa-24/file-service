import os
import subprocess
import sys

def check_command(cmd):
    """Find gcloud/gsutil command, with Windows support."""
    cmd += ".cmd" if os.name == "nt" else ""
    try:
        result = subprocess.run([cmd, "--version"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except FileNotFoundError:
        print(f"Error: {cmd} not found in PATH")
        print(f"Add {cmd} to your PATH. Or set the GCLOUD_PATH and GSUTIL_PATH environment variables with the full paths to the commands.")
        print()
        print("Install Google Cloud SDK: https://cloud.google.com/sdk/docs/install-sdk")
        sys.exit(1)

    return cmd