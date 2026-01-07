package essa.service.image;

import java.net.URL;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import essa.dto.file.FileUploadRequest;
import essa.dto.file.FileUploadResponse;
import essa.dto.image.ImageAddLODRequest;
import essa.dto.image.PropertyThumbnailsResponse;
import essa.dto.image.ImagePropertyResponse;
import essa.dto.image.ImagePropertyQuery;
import essa.entity.ImageLevelOfDetail;
import essa.entity.enums.LevelOfDetail;
import essa.entity.enums.FileStatus;
import essa.entity.enums.FileType;
import essa.entity.File;
import essa.repository.image.ImageRepository;
import essa.repository.tag.TagRepository;
import essa.repository.file.FileRepository;
import essa.service.gcs.GcsService;
import essa.service.file.FileService;
import essa.service.image.ImageClassificationService;
import essa.entity.id.ImageLevelOfDetailId;
import org.jboss.logging.Logger;

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

    @Inject
    ImageClassificationService imageClassificationService;

    private static final String IMAGE_ROOT = "images";

    private static final Logger LOG = Logger.getLogger(ImageService.class);

    private String generateObjectName(String keycloakId, String lod, UUID uuid, String fileName) {
        String objectName = String.format("%s/%s/%s/%s-%s", 
            IMAGE_ROOT, keycloakId, lod, uuid.toString(), fileName);
        return objectName;
    }

    private boolean isImageContentType(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    @Transactional
    public FileUploadResponse uploadImage(@Valid @NotNull FileUploadRequest request) throws Exception {
        String keycloakId = securityIdentity.getPrincipal().getName();
        UUID uuid = UUID.randomUUID();

        if (isImageContentType(request.getContentType()) == false) {
            throw new WebApplicationException("Invalid content type, file is not an image", Response.Status.BAD_REQUEST);
        }

        String bucketName = gcsService.getPublicBucketName();
        String objectName = generateObjectName(keycloakId, "ORIGINAL", uuid, request.getFileName());

        
        return fileService.handleFileUpload(keycloakId, uuid, bucketName, objectName, FileType.IMAGE, request);
    }

    @Transactional
    public void hardDeleteImage(@NotNull UUID id) throws Exception {
        String keycloakId = securityIdentity.getPrincipal().getName();

        File image = fileRepository.findById(id);
        if (image == null) {
            throw new WebApplicationException("Image not found", Response.Status.NOT_FOUND);
        }
        if (!image.getOwnerKeycloakId().equals(keycloakId)) {
            throw new WebApplicationException("Forbidden", Response.Status.FORBIDDEN);
        }

        handleHardDeleteImage(image);
    } 

    public void handleHardDeleteImage(File image) throws Exception {
        boolean deleted = gcsService.deleteObject(image.getBucketName(), image.getObjectName());
        if (!deleted) {
            throw new WebApplicationException("Failed to delete image from storage", Response.Status.INTERNAL_SERVER_ERROR);
        }

        imageRepository.getAllImageLODsByFileId(image.getId()).forEach(lod -> {
            boolean deletedLOD = gcsService.deleteObject(image.getBucketName(), lod.getObjectName());
            if (!deletedLOD) {
                throw new WebApplicationException("Failed to delete image LOD from storage", Response.Status.INTERNAL_SERVER_ERROR);
            }
        });

        fileRepository.delete(image);
    }

    @Transactional
    public void deleteImagesOfProperty(@NotNull Long propertyId) throws Exception {
        String keycloakId = securityIdentity.getPrincipal().getName();

        List<File> images = imageRepository.findImagesByPropertyIdAndOwner(propertyId, keycloakId);
        
        for (File image : images) {
            handleHardDeleteImage(image);
        }
    }

    @Transactional
    public void addLevelOfDetailToImage(@Valid @NotNull ImageAddLODRequest request) throws Exception {
        UUID id = request.getId();
        LevelOfDetail levelOfDetail = request.getLevelOfDetail();

        File file = fileRepository.findById(id);
        if (file == null) {
            throw new WebApplicationException("Image does not exist", Response.Status.NOT_FOUND);
        }

        ImageLevelOfDetailId lodId = new ImageLevelOfDetailId();
        lodId.setFileId(id);
        lodId.setLevelOfDetail(levelOfDetail);

        ImageLevelOfDetail imageLOD = new ImageLevelOfDetail();
        imageLOD.setId(lodId);
        imageLOD.setFile(file);
        imageLOD.setObjectName(request.getObjectName());
        imageLOD.setFileSize(request.getSize());

        file.addImageLOD(imageLOD);
    }

    @Transactional
    public URL downloadImage(@NotNull UUID id, LevelOfDetail levelOfDetail) throws Exception {        
        File file = fileRepository.findById(id);
        if (file == null || file.getFileType() != FileType.IMAGE) {
            throw new WebApplicationException("Image not found", Response.Status.NOT_FOUND);
        }
        if (file.getStatus() != FileStatus.AVAILABLE) {
            throw new WebApplicationException("Image is not available", Response.Status.GONE);
        }
        
        if (levelOfDetail == null) {
            return gcsService.generatePublicObjectUrl(file.getBucketName(), file.getObjectName());
        }
        
        ImageLevelOfDetail imageLOD = imageRepository.findByIdAndLOD(id, levelOfDetail);
        if (imageLOD == null) {
            throw new WebApplicationException("Level of detail does not exist", Response.Status.NOT_FOUND);
        }
        return gcsService.generatePublicObjectUrl(imageLOD.getFile().getBucketName(), imageLOD.getObjectName());
    }

    @Transactional
    public List<PropertyThumbnailsResponse> getThumbnailsForProperties(@NotNull List<Long> propertyIds) {
        List<PropertyThumbnailsResponse> results = imageRepository.findThumbnailsByPropertyIds(propertyIds);
        return results;
    }

    @Transactional
    public List<ImagePropertyResponse> getImagesOfProperty(@NotNull Long propertyId, LevelOfDetail levelOfDetail) throws Exception {
        List<ImagePropertyQuery> imageData;
        if (levelOfDetail == null)
            imageData = imageRepository.findOriginalImagesOfProperty(propertyId);
        else
            imageData = imageRepository.findLodImagesOfProperty(propertyId, levelOfDetail);

        // BC hoce prazen seznam namest NOT FOUND
        // if (imageData.isEmpty()) {
        //     throw new WebApplicationException("No images found for property", Response.Status.NOT_FOUND);
        // }

        List<ImagePropertyResponse> responseList = new ArrayList<>();
        for (ImagePropertyQuery data : imageData) {
            URL downloadUrl = gcsService.generatePublicObjectUrl(data.getBucketName(), data.getObjectName());
            ImagePropertyResponse response = new ImagePropertyResponse();
            response.setId(data.getId());
            response.setImageUrl(downloadUrl);
            response.setTags(data.getTags());

            responseList.add(response);
        }


        return responseList;
    }

    @Transactional
    public void classifyImage(File image) throws Exception {
        URL downloadUrl = gcsService.generatePublicObjectUrl(image.getBucketName(), image.getObjectName());
        LOG.infof("Classifying image %s from URL: %s", image.getId(), downloadUrl.toString());
        Map<String, Object> classificationResult = imageClassificationService.callByUrl(downloadUrl);

        LOG.infof("Classification result for image %s: %s", image.getId(), classificationResult.toString());
    }

    @Transactional
    public void classifyImageAsync(UUID imageId) throws Exception {
        // Start a new transaction for classification
        File image = fileRepository.findById(imageId);
        if (image == null) return;
        classifyImage(image);
    }
}
