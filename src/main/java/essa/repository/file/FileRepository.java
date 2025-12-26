package essa.repository.file;

import essa.entity.File;
import io.smallrye.common.constraint.NotNull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class FileRepository {

    @PersistenceContext
    EntityManager em;

    @NotNull
    public File findById(@NotNull UUID id) {
        return em.find(File.class, id);
    }

    @Transactional
    public void persist(@NotNull File file) {
        em.persist(file);
    }

    @Transactional
    public File merge(@NotNull File file) {
        return em.merge(file);
    }

    @Transactional
    public void delete(@NotNull UUID id) {
        File file = findById(id);
        if (file != null) {
            // property_file and file_access relations are deleted with DELETE ON CASCADE
            em.remove(file);   
        }
    }

    @Transactional
    public void delete(@NotNull File file) {
        if (em.contains(file)) {
            em.remove(file);
        } else {
            em.remove(em.merge(file));
        }
    }
}
