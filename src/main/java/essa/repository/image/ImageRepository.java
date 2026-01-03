package essa.repository.image;

import java.util.UUID;

import org.jboss.logging.annotations.Param;
import org.jboss.resteasy.annotations.Query;

import essa.entity.enums.LevelOfDetail;
import essa.dto.image.PropertyThumbnailsResponse;
import essa.dto.image.ImagePropertyQuery;
import essa.entity.ImageLevelOfDetail;
import essa.entity.id.ImageLevelOfDetailId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@ApplicationScoped
public class ImageRepository {

    @PersistenceContext
    EntityManager em;

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
    public List<ImagePropertyQuery> findImagePreviewDataForProperty(Long propertyId, LevelOfDetail lod) {
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
        """;
        List<ImagePropertyQuery> images = em.createQuery(query, ImagePropertyQuery.class)
            .setParameter("propertyId", propertyId)
            .setParameter("fileType", essa.entity.enums.FileType.IMAGE)
            .setParameter("lod", lod.name())
            .getResultList();

        String query2 = """
            SELECT f.id, t.tagName
            FROM File f
            JOIN f.tags t
            WHERE f.id IN :fileIds
                """;
        Map<UUID, List<String>> tagsByFileId = em.createQuery(query2, Object[].class)
            .setParameter("fileIds", images.stream().map(ImagePropertyQuery::getId).toList())
            .getResultStream()
            .collect(
                Collectors.groupingBy(
                    row -> (UUID) row[0],
                    Collectors.mapping(row -> (String) row[1], Collectors.toList())
                )
            );

        images.forEach(image -> image.setTags(tagsByFileId.getOrDefault(image.getId(), List.of())));

        return images;
    }
}
