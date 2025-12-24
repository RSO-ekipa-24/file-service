package essa.repository.file;

import essa.entity.Tag;
import io.smallrye.common.constraint.NotNull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TagRepository {

    @PersistenceContext
    EntityManager em;

        @Transactional
    public void persist(@NotNull Tag tag) {
        em.persist(tag);
    }

    @Transactional
    public Tag update(@NotNull Tag tag) {
        return em.merge(tag);
    }
    
}
