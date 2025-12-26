$gcp_project = $env:GCP_PROJECT_ID
$region = $env:GCP_REGION
$bucket = $env:GCP_BUCKET_NAME
$kc_token_url = $env:KC_TOKEN_URL
$kc_client_id = $env:KC_CLIENT_ID
$kc_client_secret = $env:KC_CLIENT_SECRET
$confirm_base_url = $env:CONFIRM_BASE_URL


# Enable APIs (one-time)
gcloud services enable cloudfunctions.googleapis.com eventarc.googleapis.com run.googleapis.com storage.googleapis.com --project $gcp_project

# Deploy function
gcloud functions deploy confirm-on-upload `
  --gen2 `
  --runtime python312 `
  --region $region `
  --source c:\Development\essa\file-service\cloud-function `
  --entry-point on_finalize `
  --trigger-event google.cloud.storage.object.v1.finalized `
  --trigger-resource $bucket `
  --set-env-vars KC_TOKEN_URL=$kc_token_url,KC_CLIENT_ID=$kc_client_id,KC_CLIENT_SECRET=$kc_client_secret,CONFIRM_BASE_URL=$confirm_base_url `
  --project $gcp_project