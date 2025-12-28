package essa.service.gcs;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.net.URL;
import java.util.Map;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.SignUrlOption;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.HttpMethod;
import com.google.auth.oauth2.ServiceAccountCredentials;

import java.io.FileInputStream;
import java.io.IOException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GcsService {
    private final Storage storage;
    private final String bucketName;

    @Inject
    public GcsService(
            @ConfigProperty(name = "gcs.project-id") String projectId,
            @ConfigProperty(name = "gcs.bucket-name") String bucketName,
            @ConfigProperty(name = "gcs.credentials-path") String credentialsPath) {
        
        this.bucketName = bucketName;

        /**
         * Signing a URL requires Credentials which implement ServiceAccountSigner. These can be set
         * explicitly using the Storage.SignUrlOption.signWith(ServiceAccountSigner) option.
         * You could also pass a service account signer to StorageOptions, i.e.
         * StorageOptions().newBuilder().setCredentials(ServiceAccountSignerCredentials). Or alternatively,
         * set the GOOGLE_APPLICATION_CREDENTIALS environment variable to point to a service account key
         * file. See the documentation for Storage.signUrl for more details.
         */
        try {
            this.storage = StorageOptions.newBuilder()
                    .setProjectId(projectId)
                    .setCredentials(ServiceAccountCredentials.fromStream(
                            new FileInputStream(credentialsPath)
                    ))
                    .build()
                    .getService();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load GCS credentials from: " + credentialsPath, e);
        }
        
    }

    public String getBucketName() {
        return bucketName;
    }


    public URL generateV4PutObjectSignedUrl(String objectName, String contentType, int durationMinutes) {
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

    public URL generateV4GetObjectSignedUrl(String objectName, int durationMinutes) {
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

    public boolean deleteObject(String objectName) {
        BlobId blobId = BlobId.of(bucketName, objectName);
        return storage.delete(blobId);
    }
}
