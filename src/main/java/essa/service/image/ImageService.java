package essa.service.image;

import java.net.URL;
import java.util.UUID;
import java.util.List;

import com.google.apps.card.v1.Image;

import essa.dto.file.FileUploadRequest;
import essa.dto.file.FileUploadResponse;
import essa.dto.image.ImageAddLODRequest;
import essa.dto.image.PropertyThumbnailsResponse;
import essa.entity.ImageLevelOfDetail;
import essa.entity.enums.LevelOfDetail;
import essa.entity.File;
import essa.repository.image.ImageRepository;
import essa.repository.tag.TagRepository;
import essa.repository.file.FileRepository;
import essa.service.gcs.GcsService;
import essa.service.file.FileService;
import essa.entity.id.ImageLevelOfDetailId;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class ImageService {
    
    @Inject
    ImageRepository imageRepository;

    @Inject 
    FileRepository fileRepository;
    
    @Inject
    TagRepository tagRepository;
    
    @Inject
    GcsService gcsService;
    
    @Inject
    SecurityIdentity securityIdentity;

    @Inject 
    FileService fileService;

    private static final String IMAGE_ROOT = "images";

    private String generateObjectName(String keycloakId, String lod, UUID uuid, String fileName) {
        StringBuilder builder = new StringBuilder(IMAGE_ROOT);
        builder.append("/");
        builder.append(keycloakId);
        builder.append("/");
        builder.append(lod);
        builder.append("/");
        builder.append(uuid.toString());
        builder.append("-");
        builder.append(fileName);
        return builder.toString();
    }

    @Transactional
    public FileUploadResponse uploadImage(@Valid @NotNull FileUploadRequest request) throws Exception {
        String keycloakId = securityIdentity.getPrincipal().getName();
        UUID uuid = UUID.randomUUID();

        String objectName = generateObjectName(keycloakId, null, uuid, request.getFileName());
        
        return fileService.handleFileUpload(keycloakId, uuid, objectName, request);
    }

    @Transactional
    public void softDeleteImage(@NotNull UUID id) throws Exception {
        fileService.softDeleteFile(id);
    }

    @Transactional
    public void hardDeleteImage(@NotNull UUID id) throws Exception {
        String keycloakId = securityIdentity.getPrincipal().getName();

        File file = fileRepository.findById(id);
        if (file == null) {
            throw new WebApplicationException("Image not found", Response.Status.NOT_FOUND);
        }
        if (!file.getOwnerKeycloakId().equals(keycloakId)) {
            throw new WebApplicationException("Forbidden", Response.Status.FORBIDDEN);
        }

        boolean deleted = gcsService.deleteObject(file.getObjectName());
        if (!deleted) {
            throw new WebApplicationException("Failed to delete image from storage", Response.Status.INTERNAL_SERVER_ERROR);
        }

        imageRepository.getAllImageLODsByFileId(id).forEach(lod -> {
            boolean deletedLOD = gcsService.deleteObject(lod.getObjectName());
            if (!deletedLOD) {
                throw new WebApplicationException("Failed to delete image LOD from storage", Response.Status.INTERNAL_SERVER_ERROR);
            }
        });

        fileRepository.delete(file);
    }


    @Transactional
    public void addLevelOfDetailToImage(@Valid @NotNull ImageAddLODRequest request) throws Exception {
        String keycloakId = securityIdentity.getPrincipal().getName();
        UUID fileId = request.getFileId();
        LevelOfDetail levelOfDetail = request.getLevelOfDetail();

        File file = fileRepository.findById(fileId);
        if (file == null) {
            throw new WebApplicationException("Image does not exist", Response.Status.NOT_FOUND);
        }

        String objectName = generateObjectName(keycloakId, levelOfDetail.name(), fileId, file.getFileName());

        ImageLevelOfDetailId id = new ImageLevelOfDetailId();
        id.setFileId(fileId);
        id.setLevelOfDetail(levelOfDetail);

        ImageLevelOfDetail imageLOD = new ImageLevelOfDetail();
        imageLOD.setId(id);
        imageLOD.setFile(file);
        imageLOD.setObjectName(objectName);
        imageLOD.setFileSize(request.getFileSize());

        file.addImageLOD(imageLOD);
    }

    @Transactional
    public URL downloadImage(@NotNull UUID id, LevelOfDetail levelOfDetail) throws Exception {
        int durationMinutes = 5;
        
        if (levelOfDetail == null) {
            File file = fileRepository.findById(id);
            if (file == null) {
                throw new WebApplicationException("Image not found", Response.Status.NOT_FOUND);
            }
            return gcsService.generateV4GetObjectSignedUrl(file.getObjectName(), durationMinutes);

        }
        
        ImageLevelOfDetail imageLOD = imageRepository.findByIdAndLOD(id, levelOfDetail);
        if (imageLOD == null) {
            throw new WebApplicationException("Level of detail does not exist", Response.Status.NOT_FOUND);
        }
        return gcsService.generateV4GetObjectSignedUrl(imageLOD.getObjectName(), durationMinutes);
    }

    @Transactional
    public List<PropertyThumbnailsResponse> getThumbnailsForProperties(@NotNull List<Long> propertyIds) {
        List<PropertyThumbnailsResponse> results = imageRepository.findThumbnailsByPropertyIds(propertyIds);
        return results;
    }

    // TODO get images of property
}
