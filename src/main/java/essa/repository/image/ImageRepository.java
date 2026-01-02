package essa.repository.image;

import java.util.UUID;

import org.jboss.logging.annotations.Param;
import org.jboss.resteasy.annotations.Query;

import essa.entity.enums.LevelOfDetail;
import essa.dto.image.PropertyThumbnailsResponse;
import essa.dto.image.ImagePreviewQuery;
import essa.entity.ImageLevelOfDetail;
import essa.entity.id.ImageLevelOfDetailId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;


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
    public List<ImagePreviewQuery> findImagePreviewDataForProperty(Long propertyId) {
        String query = """
            SELECT new essa.dto.image.ImagePreviewQuery(
                f.id,
                f.bucketName,
                i.objectName,
                collect(DISTINCT t.tagName)
            )
            FROM File f
            JOIN f.propertyLinks pf
            JOIN f.imageLOD i
            LEFT JOIN f.tags t
            WHERE pf.id.propertyId = :propertyId
              AND f.fileType = :fileType
              AND i.id.levelOfDetail = :lod
            GROUP BY f.id, f.bucketName, i.objectName
        """;
        List<ImagePreviewQuery> results = em.createQuery(query, ImagePreviewQuery.class)
            .setParameter("propertyId", propertyId)
            .getResultList();
        return results;
    }
}
