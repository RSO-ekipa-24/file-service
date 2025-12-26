package essa.dto.file;

import java.time.LocalDateTime;
import java.util.UUID;

public class FileMetadataResponse {
    private UUID id;
    private String fileName;
    private String contentType;
    private long fileSize;
    private LocalDateTime dateUploaded;
    private LocalDateTime dateModified;

    public FileMetadataResponse(UUID id, String fileName, String contentType, long fileSize, LocalDateTime dateUploaded, LocalDateTime dateModified) {
        this.id = id;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.dateUploaded = dateUploaded;
        this.dateModified = dateModified;
    }

    public UUID getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public LocalDateTime getDateUploaded() {
        return dateUploaded;
    }

    public LocalDateTime getDateModified() {
        return dateModified;
    }
}
