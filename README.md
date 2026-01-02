# files

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Environment

Set environmental variables in ``.env`` file. Follow ``.env.example``.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/core-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Environmental variables

Create ``.env`` file with environmental varaibles. Follow ``.env.example``.

## Building Docker image

Docker image can be built with:

```shell script
docker build -t file-service:latest -f ./src/main/docker/Dockerfile.jvm .
```

## Google Cloud Setup

Current implementation works with Google Cloud and uses Google Cloud Storage to save files.

### Prerequisites
- gcloud CLI
- gsutil
- Authenticated via `gcloud auth login`
- Set project with  `gcloud config set project $GCP_PROJECT_ID`

### Service account

For deployment service account is required. If it does not exist generate with

```bash
gcloud iam service-accounts create file-service --display-name "File Service"
```

For local development authenticate with 

```bash
gcloud auth application-default login
```

and grant your user account permission to impersonate the service account

```bash
gcloud iam service-accounts add-iam-policy-binding file-service@artful-reactor-351917.iam.gserviceaccount.com --member="user:$(gcloud config get-value account)" --role="roles/iam.serviceAccountTokenCreator"
```

### K8s service account

Our goal is to allow the Kubernetes pod to securely interact with Google Cloud Storage and generate Signed URLs without using static JSON key files.

You need to create a k8s service account (located inside the helm chart for files) and link it to the google SA. 

Then link this SA to the right pod - under `spec.serviceAccountName` (see sample in file-service-app.yaml)


#### 1. Enable the IAM Credentials API

The Google Cloud Storage library must communicate with the IAM API to sign the URL. This API is often disabled by default.

```bash
gcloud services enable iamcredentials.googleapis.com
```

#### 2. Link the K8s and Google Service Accounts

This step enables the Workload Identity handshake. It allows the Kubernetes Service Account (KSA) to act as the Google Service Account (GSA).
```bash
gcloud iam service-accounts add-iam-policy-binding \                                                 
    file-service@artful-reactor-351917.iam.gserviceaccount.com \
    --role="roles/iam.workloadIdentityUser" \
    --member="serviceAccount:artful-reactor-351917.svc.id.goog[essa-project/files-service-ksa]"
```

#### 3. Grant Signing Permissions (Token Creator)

For the `file-service` to generate Signed URLs, the GSA needs the authority to sign data. You must grant the `serviceAccountTokenCreator` role to the GSA itself.

```bash
gcloud iam service-accounts add-iam-policy-binding \
    [GSA_EMAIL] \
    --role="roles/iam.serviceAccountTokenCreator" \
    --member="serviceAccount:[GSA_EMAIL]"
```

#### 4. Restart the file-service pod

```bash
kubectl rollout restart deployment files-service -n essa-project
```

##### 5. Configuration check

After running these commands, ensure the following is true in your Kubernetes manifests:

- **Annotation**: Your ServiceAccount has the annotation iam.gke.io/gcp-service-account: file-service@artful-reactor-351917.iam.gserviceaccount.com.
- **Deployment**: Your Pod spec includes serviceAccountName: <your-name>

---

### Bucket setup

Service uses two buckets, one for private and one for public data. Buckets can be set up with Python script ``scripts/setup-gcs.py``

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

## Branching Strategy

- main: The production-ready branch.
- dev: The integration branch for features and fixes, often considered the "next release" branch.
- feature/: Branches for developing new features. These branches are created from dev and merged back into dev when the feature is complete.
- bugfix/: Branches for fixing bugs in the dev branch.
- release/: Branches for preparing a new production release. These branches allow for last-minute fixes and preparing release notes.
- hotfix/: Branches for fixing critical issues in the main branch. These are created from main and merged back into both main and dev.


#### Build and push the image to Google Registry:


First, you need to commit and push all the changes u made to Git!

Then extract your commit hash:

```bash
GIT_HASH=$(git rev-parse --short HEAD)
```
and `echo` it and confirm it matches the hash on GitHub UI.

1. Package the app - This generates the application files inside the target/ folder.


```bash
./mvnw clean package -Dquarkus.container-image.build=false -DskipTests
```

if it fails, you may need to run `chmod +x mvnw`


2. Build the local docker image and tag it for Google registry - we will tag it with the commit hash for easier rollbacks and to keep track.

```bash
docker build -f src/main/docker/Dockerfile.jvm -t europe-central2-docker.pkg.dev/artful-reactor-351917/essa-images/files-service:$GIT_HASH .
```

3. Push to the cloud:

```bash
docker push europe-central2-docker.pkg.dev/artful-reactor-351917/essa-images/files-service:$GIT_HASH
```
