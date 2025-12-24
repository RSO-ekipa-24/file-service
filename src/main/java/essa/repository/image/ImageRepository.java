package essa.repository.image;

import essa.entity.Image;
import essa.entity.LevelOfDetail;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ImageRepository {

    @PersistenceContext
    EntityManager em;
    
    @Transactional
    public void persist(Image image) {
        em.persist(image);
    }

    @Transactional
    public void delete(Image image) {
        if (em.contains(image)) {
            em.remove(image);
        } else {
            em.remove(em.merge(image));
        }
    }
}
