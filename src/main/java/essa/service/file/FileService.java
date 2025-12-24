package essa.service.file;

import essa.dto.file.FileUploadRequest;
import essa.dto.file.FileUploadResponse;
import essa.entity.File;
import essa.entity.Status;
import essa.repository.file.FileRepository;
import essa.repository.file.TagRepository;
import essa.service.gcs.GcsService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.validation.constraints.NotNull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import java.util.UUID;
import java.net.URL;
import java.time.LocalDateTime;

@ApplicationScoped
public class FileService {

    @Inject
    FileRepository fileRepository;

    @Inject
    TagRepository tagRepository;

    @Inject
    GcsService gcsService;

    @Inject
    SecurityIdentity securityIdentity;

    @Transactional
    public FileUploadResponse uploadFile(@Valid @NotNull FileUploadRequest request) throws Exception {
        String keycloakId = securityIdentity.getPrincipal().getName();
        UUID uuid = UUID.randomUUID();

        StringBuilder builder = new StringBuilder("files/");
        builder.append(keycloakId);
        builder.append("/");
        builder.append(uuid.toString());
        builder.append("-");
        builder.append(request.getFileName());

        String objectName = builder.toString();

        URL gcsUploadSignedUrl = gcsService.generateV4PutObjectSignedUrl(objectName, request.getContentType(), 5);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);
        
        File file = new File();
        file.setId(uuid);
        file.setFileName(request.getFileName());
        file.setContentType(request.getContentType());
        file.setFileSize(request.getSize());
        file.setBucketName(gcsService.getBucketName());
        file.setObjectName(objectName);
        file.setStatus(Status.PENDING);
        file.setOwnerKeycloakId(keycloakId);
        
        fileRepository.persist(file);
        
        FileUploadResponse response = new FileUploadResponse();
        response.setId(uuid);
        response.setUploadUrl(gcsUploadSignedUrl);
        response.setExpiresAt(expiresAt);

        return response;
    }
}
