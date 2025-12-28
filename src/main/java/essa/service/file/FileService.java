package essa.service.file;

import essa.dto.file.FileUploadRequest;
import essa.dto.file.FileUploadResponse;
import essa.dto.file.FileMetadataResponse;
import essa.entity.File;
import essa.entity.PropertyFile;
import essa.entity.Tag;
import essa.entity.enums.FileStatus;
import essa.entity.enums.FileType;
import essa.entity.id.PropertyFileId;
import essa.repository.file.FileRepository;
import essa.repository.tag.TagRepository;
import essa.service.gcs.GcsService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Response;

import java.util.UUID;
import java.util.Set;

import org.checkerframework.checker.units.qual.t;

import com.aayushatharva.brotli4j.common.annotations.Local;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

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

        if (request.getPropertyId() != null) {
            long propertyId;
            try {
                propertyId = Long.parseLong(request.getPropertyId());
            } catch (NumberFormatException e) {
                throw new WebApplicationException("Invalid propertyId format", Response.Status.BAD_REQUEST);
            }

            PropertyFileId propertyFileId = new PropertyFileId();
            propertyFileId.setPropertyId(propertyId);
            propertyFileId.setFileId(uuid);

            PropertyFile propertyFile = new PropertyFile();
            propertyFile.setId(propertyFileId);

            file.addPropertyLink(propertyFile);
        }
        
        fileRepository.persist(file);
        
        FileUploadResponse response = new FileUploadResponse();
        response.setId(uuid);
        response.setUploadUrl(gcsUploadSignedUrl);
        response.setExpiresAt(expiresAt);

        return response;
    }

    @Transactional 
    public void softDeleteFile(@NotNull UUID id) throws Exception {
        File file = findFileWithPermissionCheck(id);
        file.softDelete();
    }

    @Transactional
    public void hardDeleteFile(@NotNull UUID id) throws Exception {
        File file = findFileWithPermissionCheck(id);

        boolean deleted = gcsService.deleteObject(file.getObjectName());
        if (!deleted) {
            throw new WebApplicationException("Failed to delete file from storage", Response.Status.INTERNAL_SERVER_ERROR);
        }

        fileRepository.delete(file);
    }

    @Transactional
    public void confirmUpload(@NotNull UUID id) throws Exception {
        File file = fileRepository.findById(id);
        if (file == null)
            throw new WebApplicationException("File not found", Response.Status.NOT_FOUND);

        if (file.getStatus() == FileStatus.AVAILABLE)
            return;
        
        if (file.getStatus() == FileStatus.PENDING) {
            file.setStatus(FileStatus.AVAILABLE);
            return;
        }

        throw new WebApplicationException("File upload cannot be confirmed in its current state", Response.Status.BAD_REQUEST);
    }

    @Transactional
    public URL downloadFile(@NotNull UUID id) throws Exception {
        File file = findFileWithPermissionCheck(id);

        if (file.getStatus() != FileStatus.AVAILABLE) {
            throw new WebApplicationException("File not available", Response.Status.NOT_FOUND);
        }
        int durationMinutes = 5;
        URL downloadUrl = gcsService.generateV4GetObjectSignedUrl(file.getObjectName(), durationMinutes);
        if (downloadUrl == null) {
            throw new WebApplicationException("Failed to generate download URL", Response.Status.INTERNAL_SERVER_ERROR);
        }
        
        return downloadUrl; 
    }

    @Transactional
    public FileMetadataResponse getFileMetadata(@NotNull UUID id) throws Exception {
        File file = findFileWithPermissionCheck(id);
        if (file == null) {
            throw new WebApplicationException("File not found", Response.Status.NOT_FOUND);
        }

        FileMetadataResponse response = new FileMetadataResponse();
        response.setId(file.getId());
        response.setFileName(file.getFileName());
        response.setContentType(file.getContentType());
        response.setFileSize(file.getFileSize());
        response.setDateUploaded(file.getCreated().toLocalDateTime());
        response.setDateModified(file.getModified().toLocalDateTime());
        response.setStatus(file.getStatus());
        Set<Tag> tags = file.getTags();
        List<String> tagNames = tags.stream().map(tag -> tag.getTagName()).toList();
        response.setTags(tagNames);

        return response;
    }

    @Transactional
    public File findFileWithPermissionCheck(UUID id) throws Exception {
        String keycloakId = securityIdentity.getPrincipal().getName();
        File file = fileRepository.findById(id);
        if (file == null) {
            throw new WebApplicationException("File not found", Response.Status.NOT_FOUND);
        }
        if (!file.getOwnerKeycloakId().equals(keycloakId)) {
            throw new WebApplicationException("Forbidden", Response.Status.FORBIDDEN);
        }
        return file;
    }
}
