package essa.repository.image;

import java.util.UUID;

import essa.entity.enums.LevelOfDetail;
import essa.entity.ImageLevelOfDetail;
import essa.entity.id.ImageLevelOfDetailId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ImageRepository {

    @PersistenceContext
    EntityManager em;

    @Transactional
    public ImageLevelOfDetail findByIdAndLOD(UUID id, LevelOfDetail levelOfDetail) {
        return em.find(ImageLevelOfDetail.class, new ImageLevelOfDetailId(id, levelOfDetail));
    }
    
}
