import os
from pathlib import Path
import sys

# -------------------------
# Load .env
# -------------------------
def load_env_file(env_file=".env"):
    """Load environment variables from .env file."""
    if not Path(env_file).exists():
        print(f"Error: {env_file} not found")
        sys.exit(1)
    
    with open(env_file, "r") as f:
        for line in f:
            line = line.strip()
            # Skip empty lines and comments
            if not line or line.startswith("#"):
                continue
            
            if "=" in line:
                key, value = line.split("=", 1)
                key = key.strip()
                value = value.strip()
                os.environ[key] = value