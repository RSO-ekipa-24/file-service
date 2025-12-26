package essa.service.file;

import essa.dto.file.FileUploadRequest;
import essa.dto.file.FileUploadResponse;
import essa.dto.file.FileMetadataResponse;
import essa.entity.File;
import essa.entity.enums.FileStatus;
import essa.entity.enums.FileType;
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

    private static final String FILE_ROOT = "files";

    private String generateObjectName(String keycloakId, UUID uuid, String fileName) {
        StringBuilder builder = new StringBuilder(FILE_ROOT);
        builder.append("/");
        builder.append(keycloakId);
        builder.append("/");
        builder.append(uuid.toString());
        builder.append("-");
        builder.append(fileName);
        return builder.toString();
    }

    @Transactional
    public FileUploadResponse uploadFile(@Valid @NotNull FileUploadRequest request) throws Exception {
        String keycloakId = securityIdentity.getPrincipal().getName();
        UUID uuid = UUID.randomUUID();

        String objectName = generateObjectName(keycloakId, uuid, request.getFileName());

        return handleFileUpload(keycloakId, uuid, objectName, request);
    }

    @Transactional
    public FileUploadResponse handleFileUpload(String keycloakId, UUID uuid, String objectName, FileUploadRequest request) throws Exception {
        int durationMinutes = 5;
        URL gcsUploadSignedUrl = gcsService.generateV4PutObjectSignedUrl(objectName, request.getContentType(), durationMinutes);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(durationMinutes);
        
        File file = new File();
        file.setId(uuid);
        file.setFileName(request.getFileName());
        file.setContentType(request.getContentType());
        file.setFileType(FileType.FILE);
        file.setFileSize(request.getSize());
        file.setBucketName(gcsService.getBucketName());
        file.setObjectName(objectName);
        file.setStatus(FileStatus.PENDING);
        file.setOwnerKeycloakId(keycloakId);
        
        fileRepository.persist(file);
        
        FileUploadResponse response = new FileUploadResponse();
        response.setId(uuid);
        response.setUploadUrl(gcsUploadSignedUrl);
        response.setExpiresAt(expiresAt);

        return response;
    }


    @Transactional
    public boolean confirmUpload(@NotNull UUID id) throws Exception {
        File file = fileRepository.findById(id);
        if (file == null)
            return false;
        
        if (file.getStatus() == FileStatus.AVAILABLE)
            return true;
        
        if (file.getStatus() == FileStatus.PENDING) {
            file.setStatus(FileStatus.AVAILABLE);
            return true;
        }
    
        return false;
    }

    @Transactional
    public URL downloadFile(@NotNull UUID id) {
        File file = fileRepository.findById(id);
        if (file == null || file.getStatus() != FileStatus.AVAILABLE) {
            return null;
        }
        int durationMinutes = 5;
        return gcsService.generateV4GetObjectSignedUrl(file.getObjectName(), durationMinutes);
    }

    @Transactional
    public FileMetadataResponse getFileMetadata(@NotNull UUID id) {
        File file = fileRepository.findById(id);
        if (file == null) {
            return null;
        }
        return new FileMetadataResponse(
                file.getId(),
                file.getFileName(),
                file.getContentType(),
                file.getFileSize(),
                file.getCreated().toLocalDateTime(),
                file.getModified().toLocalDateTime()
        );
    }
}
