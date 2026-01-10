package essa.repository.image;

import java.util.UUID;

import essa.entity.enums.LevelOfDetail;
import essa.dto.image.PropertyThumbnailsResponse;
import essa.dto.image.ImagePropertyQuery;
import essa.entity.enums.FileType;
import essa.entity.File;
import essa.entity.ImageLevelOfDetail;
import essa.entity.id.ImageLevelOfDetailId;
import essa.repository.file.FileRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ImageRepository {

    @PersistenceContext
    EntityManager em;

    @Inject
    FileRepository fileRepository;

    @Transactional
    public ImageLevelOfDetail findByIdAndLOD(UUID id, LevelOfDetail levelOfDetail) {
        return em.find(ImageLevelOfDetail.class, new ImageLevelOfDetailId(id, levelOfDetail));
    }

    @Transactional
    public void persist(ImageLevelOfDetail imageLevelOfDetail) {
        em.persist(imageLevelOfDetail);
    }

    @Transactional
    public List<PropertyThumbnailsResponse> findThumbnailsByPropertyIds(List<Long> propertyIds) {
        String query = """
            SELECT new essa.dto.image.PropertyThumbnailsResponse(p.id, i.objectName)
            FROM File f
            JOIN f.propertyLinks pf
            JOIN pf.property p
            JOIN f.imageLOD i
            WHERE p.id IN :propertyIds
            AND i.id.levelOfDetail = 'LOW'
        """;
        return em.createQuery(query, PropertyThumbnailsResponse.class)
                 .setParameter("propertyIds", propertyIds)
                 .getResultList();
    }

    @Transactional
    public List<ImageLevelOfDetail> getAllImageLODsByFileId(UUID fileId) {
        String query = "SELECT i FROM ImageLevelOfDetail i WHERE i.id.fileId = :fileId";
        return em.createQuery(query, ImageLevelOfDetail.class)
          .setParameter("fileId", fileId)
          .getResultList();
    }

    @Transactional
    public List<ImagePropertyQuery> findOriginalImagesOfProperty(Long propertyId) {
        String query = """
            SELECT new essa.dto.image.ImagePropertyQuery(
                f.id,
                f.bucketName,
                f.objectName
            )
            FROM File f
            JOIN f.propertyLinks pf
            WHERE pf.id.propertyId = :propertyId
              AND f.fileType = :fileType
              AND f.status = :status
            """;
        List<ImagePropertyQuery> images = em.createQuery(query, ImagePropertyQuery.class)
                .setParameter("propertyId", propertyId)
                .setParameter("fileType", FileType.IMAGE)
                .setParameter("status", FileStatus.AVAILABLE)
                .getResultList();

        if (images.isEmpty()) return images;

        List<UUID> imageIds = images.stream().map(ImagePropertyQuery::getId).toList();
        Map<UUID, List<String>> tagsByImageId = fileRepository.findTagNamesForFileIds(imageIds);
        images.forEach(image -> image.setTags(tagsByImageId.getOrDefault(image.getId(), List.of())));

        return images;
    }

    @Transactional
    public List<ImagePropertyQuery> findLodImagesOfProperty(Long propertyId, LevelOfDetail lod) {
        String query = """
        SELECT new essa.dto.image.ImagePropertyQuery(
            f.id,
            f.bucketName,
            i.objectName
        )
        FROM File f
        JOIN f.propertyLinks pf
        JOIN f.imageLOD i
        WHERE pf.id.propertyId = :propertyId
          AND f.fileType = :fileType
          AND i.id.levelOfDetail = :lod
          AND f.status = :status
    """;
        List<ImagePropertyQuery> images = em.createQuery(query, ImagePropertyQuery.class)
                .setParameter("propertyId", propertyId)
                .setParameter("fileType", FileType.IMAGE)
                .setParameter("lod", lod)
                .setParameter("status", FileStatus.AVAILABLE)
                .getResultList();

        if (images.isEmpty()) return images;

        List<UUID> imageIds = images.stream().map(ImagePropertyQuery::getId).toList();
        Map<UUID, List<String>> tagsByImageId = fileRepository.findTagNamesForFileIds(imageIds);

        images.forEach(image -> image.setTags(tagsByImageId.getOrDefault(image.getId(), List.of())));

        return images;
    }

    @Transactional 
    public List<File> findImagesByPropertyIdAndOwner(Long propertyId, String ownerKeycloakId) {
        String query = """
            SELECT f
            FROM File f
            JOIN f.propertyLinks pf
            WHERE pf.id.propertyId = :propertyId
              AND f.ownerKeycloakId = :ownerKeycloakId
              AND f.fileType = :fileType
        """;
        return em.createQuery(query, File.class)
            .setParameter("propertyId", propertyId)
            .setParameter("ownerKeycloakId", ownerKeycloakId)
            .setParameter("fileType", FileType.IMAGE)
            .getResultList();
    }
}
