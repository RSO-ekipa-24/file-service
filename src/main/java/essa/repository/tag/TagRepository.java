package essa.repository.tag;

import essa.entity.Tag;
import essa.entity.enums.FileType;
import io.smallrye.common.constraint.NotNull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.UUID;
import java.util.List;

@ApplicationScoped
public class TagRepository {

    @PersistenceContext
    EntityManager em;

        @Transactional
    public void persist(@NotNull Tag tag) {
        em.persist(tag);
    }

    @Transactional
    public Tag merge(@NotNull Tag tag) {
        return em.merge(tag);
    }

    @Transactional
    public Tag findById(@NotNull UUID id) {
        return em.find(Tag.class, id);
    }

    @Transactional
    public void delete(@NotNull Tag tag) {
        if (em.contains(tag)) {
            em.remove(tag);
        } else {
            em.remove(em.merge(tag));
        }
    }
    
    @Transactional
    public List<String> listSystemTagsForFileType(@NotNull FileType fileType) {
        String query = "SELECT t.tagName FROM Tag t WHERE t.ownerKeycloakId is NULL AND t.fileType = :fileType";
        return em.createQuery(query, String.class)
                 .setParameter("fileType", fileType)
                 .getResultList();

    }

    @Transactional
    public List<String> listUserTagsForFileType(@NotNull FileType fileType, @NotNull String ownerKeycloak) {
        String query = "SELECT t.tagName FROM Tag t WHERE t.ownerKeycloakId = :ownerKeycloakId AND t.fileType = :fileType";
        return em.createQuery(query, String.class)
                 .setParameter("fileType", fileType)
                 .setParameter("ownerKeycloakId", ownerKeycloak)
                 .getResultList();
    }
}
