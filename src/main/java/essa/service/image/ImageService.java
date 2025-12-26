package essa.service.image;

import java.net.URL;
import java.util.UUID;

import essa.dto.file.FileUploadRequest;
import essa.dto.file.FileUploadResponse;
import essa.entity.ImageLevelOfDetail;
import essa.entity.enums.LevelOfDetail;
import essa.repository.file.TagRepository;
import essa.repository.image.ImageRepository;
import essa.repository.file.FileRepository;
import essa.service.gcs.GcsService;
import essa.service.file.FileService;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

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
    public URL downloadImage(@NotNull UUID id, LevelOfDetail levelOfDetail) {
        if (levelOfDetail == null) {
            return fileService.downloadFile(id);
        }
        ImageLevelOfDetail imageLOD = imageRepository.findByIdAndLOD(id, levelOfDetail);
        int durationMinutes = 5;
        return gcsService.generateV4GetObjectSignedUrl(imageLOD.getObjectName(), durationMinutes);
    }
}
