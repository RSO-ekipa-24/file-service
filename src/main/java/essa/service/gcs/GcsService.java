package essa.service.gcs;

import java.util.concurrent.TimeUnit;
import java.net.URL;
import java.util.List;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.HttpMethod;
import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ImpersonatedCredentials;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class GcsService {
    private final Storage storage;
    private final String privateBucketName;
    private final String publicBucketName;
    private static final String GCS_BASE_URL = "https://storage.googleapis.com";
    private final String serviceAccountEmail;

    private List<String> scopes = List.of(
        "https://www.googleapis.com/auth/cloud-platform"
    );

    @Inject
    public GcsService(
            @ConfigProperty(name = "gcs.project-id") String projectId,
            @ConfigProperty(name = "gcs.private-bucket-name") String privateBucketName,
            @ConfigProperty(name = "gcs.public-bucket-name") String publicBucketName,
            @ConfigProperty(name = "gcs.service-account-email") String serviceAccountEmail
        ) {

        this.privateBucketName = privateBucketName;
        this.publicBucketName = publicBucketName;
        this.serviceAccountEmail = serviceAccountEmail;

        GoogleCredentials sourceCredentials;
        try {
            sourceCredentials = GoogleCredentials.getApplicationDefault();
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain application default credentials", e);
        }

        ImpersonatedCredentials impersonatedCredentials =
            ImpersonatedCredentials.create(
                sourceCredentials,
                this.serviceAccountEmail,
                null,
                scopes,
                3600
            );

        // Provide Google Application Credentials with ``gcloud auth application-default login`` when running locally
        this.storage = StorageOptions.newBuilder()
            .setProjectId(projectId)
            .setCredentials(impersonatedCredentials)
            .build()
            .getService();
    }

    public String getPrivateBucketName() {
        return privateBucketName;
    }

    public String getPublicBucketName() {
        return publicBucketName;
    }

    public URL generateV4PutObjectSignedUrl(String bucketName, String objectName, String contentType, int durationMinutes) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName))
                .setContentType(contentType)
                .build();

        URL url = storage.signUrl(
                blobInfo,
                durationMinutes, TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withContentType(),
                Storage.SignUrlOption.withV4Signature()
        );
        return url;
    }

    public URL generateV4GetObjectSignedUrl(String bucketName, String objectName, int durationMinutes) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName))
                .build();

        URL url = storage.signUrl(
                blobInfo,
                durationMinutes, TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                Storage.SignUrlOption.withV4Signature()
        );
        return url;
    }

    public boolean deleteObject(String bucketName, String objectName) {
        BlobId blobId = BlobId.of(bucketName, objectName);
        return storage.delete(blobId);
    }

    private Long findLatestDeletedGeneration(String bucketName, String fileName) {
        Page<Blob> blobs = storage.list(bucketName,
            BlobListOption.softDeleted(true),
            BlobListOption.prefix(fileName)
        );

        for (Blob blob : blobs.iterateAll()) {
            if (blob.getName().equals(fileName)) {
                return blob.getGeneration();
            }
        }
        return null;
    }

    public void restoreDeletedObject(String bucketName, String objectName) throws Exception {
        Long generation = findLatestDeletedGeneration(bucketName, objectName);
        if (generation == null) {
            throw new WebApplicationException("Object not found", Response.Status.NOT_FOUND);
        }

        BlobId blobId = BlobId.of(bucketName, objectName, generation);
        Blob restored = storage.restore(blobId);
        if (restored == null) {
            throw new WebApplicationException("Failed to restore object", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public URL generatePublicObjectUrl(String bucketName, String objectName) throws Exception {
        String urlString = String.format("%s/%s/%s", GCS_BASE_URL, bucketName, objectName);
        return new URL(urlString);
    }
}


