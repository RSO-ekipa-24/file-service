package essa.service.schedule;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import essa.entity.File;
import essa.entity.enums.FileStatus;
import essa.repository.file.FileRepository;
import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class FileCleanupService {

    private static final Logger log = Logger.getLogger(FileCleanupService.class);
    private static final int RETENTION_DAYS = 7;
    private static final int PENDING_MINUTES = 5;

    @Inject
    FileRepository fileRepository;


    @Scheduled(cron = "0 0 2 * * ?") // Runs daily at 2:00
    @Transactional
    public void cleanupDeletedFiles() {
        log.info("Starting cleanup of DELETED files older than " + RETENTION_DAYS + " days");
        OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays((long)RETENTION_DAYS);
        cleanupFiles(cutoffDate, FileStatus.DELETED);
    }

    @Scheduled(cron = "0 05 2 * * ?") // Runs daily at 2:05
    @Transactional
    public void cloeanupPendingFiles() {
        log.info("Starting cleanup of PENDING files older than " + PENDING_MINUTES + " minutes");
        OffsetDateTime cutoffDate = OffsetDateTime.now().minusMinutes((long)PENDING_MINUTES);
        cleanupFiles(cutoffDate, FileStatus.PENDING);
    }

    @Transactional
    public void cleanupFiles(OffsetDateTime cutoffDate, FileStatus status) { 
        try {
            List<File> filesToDelete = fileRepository.findFilesWithStatusOlderThan(cutoffDate, status);
            
            log.info("Found " + filesToDelete.size() + " files to delete");
            
            for (File file : filesToDelete) {
                try {
                        fileRepository.delete(file);
                } catch (Exception e) {
                    log.error("Error deleting file: " + file.getId(), e);
                }
            }
            
            log.info("Cleanup task for status " + status + " completed successfully");
        } catch (Exception e) {
            log.error("Error during cleanup task for status " + status, e);
        }
    }
}