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

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashSet;

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
        String objectName = String.format("%s/%s/%s-%s", 
            FILE_ROOT, keycloakId, uuid.toString(), fileName);
        return objectName;
    }

    @Transactional
    public FileUploadResponse uploadFile(@Valid @NotNull FileUploadRequest request) throws Exception {
        String keycloakId = securityIdentity.getPrincipal().getName();
        UUID uuid = UUID.randomUUID();

        String bucketName = gcsService.getPrivateBucketName();
        String objectName = generateObjectName(keycloakId, uuid, request.getFileName());

        return handleFileUpload(keycloakId, uuid, bucketName, objectName, FileType.FILE, request);
    }

    @Transactional
    public FileUploadResponse handleFileUpload(String keycloakId, UUID uuid, String bucketName,String objectName, FileType fileType, FileUploadRequest request) throws Exception {
        int durationMinutes = 5;
        URL gcsUploadSignedUrl = gcsService.generateV4PutObjectSignedUrl(bucketName, objectName, request.getContentType(), durationMinutes);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(durationMinutes);
        
        File file = new File();
        file.setId(uuid);
        file.setFileName(request.getFileName());
        file.setContentType(request.getContentType());
        file.setFileType(fileType);
        file.setFileSize(request.getSize());
        file.setBucketName(bucketName);
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

        if (request.getTagNames() != null) {
            Set<Tag> applicableTags = tagRepository.findApplicableTags(request.getTagNames(), fileType, keycloakId);

            Set<String> requestTags = new HashSet<String>(request.getTagNames());
            if (applicableTags.size() != requestTags.size()) {
                Set<String> applicableTagNames = applicableTags.stream().map(tag -> tag.getTagName()).collect(Collectors.toSet());
                requestTags.removeAll(applicableTagNames);
                String errorMessage = "Invalid tags: " + String.join(", ", requestTags);
                throw new WebApplicationException(errorMessage, Response.Status.BAD_REQUEST);
            }

            Set<Tag> fileTags = file.getTags();
            fileTags.addAll(applicableTags);
        }
        
        fileRepository.persist(file);
        
        FileUploadResponse response = new FileUploadResponse();
        response.setId(uuid);
        response.setUploadUrl(gcsUploadSignedUrl);
        response.setExpiresAt(expiresAt);

        return response;
    }

    @Transactional
    public void deleteFile(@NotNull UUID id) throws Exception {
        File file = findFileWithPermissionCheck(id);

        // GCS has soft delete enabled - file persists for GCP_RETENTION_DAYS before permanent deletion
        boolean deleted = gcsService.deleteObject(file.getBucketName(), file.getObjectName());
        if (!deleted) {
            throw new WebApplicationException("Failed to delete file from storage", Response.Status.INTERNAL_SERVER_ERROR);
        }

        file.softDelete();
    }

    @Transactional
    public void hardDeleteFile(UUID id) {
        fileRepository.deleteById(id);
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
        URL downloadUrl = gcsService.generateV4GetObjectSignedUrl(file.getBucketName(),file.getObjectName(), durationMinutes);
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

    @Transactional
    public List<FileMetadataResponse> getDeletedFiles() throws Exception {
        String keycloakId = securityIdentity.getPrincipal().getName();
        List<File> deletedFiles = fileRepository.findDeletedFilesByOwner(keycloakId);

        List<FileMetadataResponse> responseList = deletedFiles.stream().map(file -> {
            FileMetadataResponse response = new FileMetadataResponse();
            response.setId(file.getId());
            response.setFileName(file.getFileName());
            response.setContentType(file.getContentType());
            response.setFileSize(file.getFileSize());
            response.setDateUploaded(file.getCreated().toLocalDateTime());
            response.setDateModified(file.getModified().toLocalDateTime());
            response.setStatus(file.getStatus());
            return response;
        }).toList();

        return responseList;
    }

    @Transactional
    public void restoreFile(@NotNull UUID id) throws Exception {
        File file = findFileWithPermissionCheck(id);
        if (file.getStatus() != FileStatus.DELETED) {
            throw new WebApplicationException("File is not deleted", Response.Status.BAD_REQUEST);
        }

        gcsService.restoreDeletedObject(file.getBucketName(), file.getObjectName());
        file.restore();
    }

    @Transactional
    public List<FileMetadataResponse> getAllFilesOfUser() throws Exception {
        String keycloakId = securityIdentity.getPrincipal().getName();
        List<File> userFiles = fileRepository.findAllFilesByOwner(keycloakId);

        List<FileMetadataResponse> responseList = userFiles.stream().map(file -> {
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
        }).toList();

        return responseList;
    }
}
