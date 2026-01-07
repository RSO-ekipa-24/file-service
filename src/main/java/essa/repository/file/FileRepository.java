package essa.repository.file;

import essa.entity.File;
import essa.entity.enums.FileStatus;

import io.smallrye.common.constraint.NotNull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.OffsetDateTime;

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
    public void delete(@NotNull File file) {
        if (em.contains(file)) {
            em.remove(file);
        } else {
            em.remove(em.merge(file));
        }
    }

    @Transactional
    public void deleteById(@NotNull UUID id) {
        File file = findById(id);
        if (file != null) {
            delete(file);
        }
    }

    @Transactional
    public void flush() {
        em.flush();
    }

    @Transactional
    public List<File> findAllFilesByOwner(String ownerId) {
        String query = "SELECT f FROM File f WHERE f.ownerKeycloakId = :ownerId AND f.status != :status";
        return em.createQuery(query, File.class)
                .setParameter("ownerId", ownerId)
                .setParameter("status", FileStatus.DELETED)
                .getResultList();
    }

    @Transactional
    public List<File> findDeletedFilesByOwner(String ownerId) {
        String query = "SELECT f FROM File f WHERE f.ownerKeycloakId = :ownerId AND f.status = :status";
        return em.createQuery(query, File.class)
                .setParameter("ownerId", ownerId)
                .setParameter("status", FileStatus.DELETED)
                .getResultList();
    }

    @Transactional 
    public List<File> findFilesWithStatusOlderThan(OffsetDateTime cutoffDate, FileStatus status) {
        String query = "SELECT f FROM File f WHERE f.status = :status AND f.modified <= :cutoffDate";
        return em.createQuery(query, File.class)
                .setParameter("status", status)
                .setParameter("cutoffDate", cutoffDate)
                .getResultList();
    }

    @Transactional
    public Map<UUID, List<String>> findTagNamesForFileIds(List<UUID> fileIds) {
        String query = """
            SELECT f.id, t.tagName
            FROM File f
            JOIN f.tags t
            WHERE f.id IN :fileIds
        """;
        return  em.createQuery(query, Object[].class)
            .setParameter("fileIds", fileIds)
            .getResultStream()
            .collect(
                Collectors.groupingBy(
                    row -> (UUID) row[0],
                    Collectors.mapping(row -> (String) row[1], Collectors.toList())
                )
            );
    }
}
