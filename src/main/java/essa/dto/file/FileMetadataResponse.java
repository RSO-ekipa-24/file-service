package essa.dto.file;

import essa.entity.enums.FileStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class FileMetadataResponse {
    private UUID id;
    private String fileName;
    private String contentType;
    private long fileSize;
    private LocalDateTime dateUploaded;
    private LocalDateTime dateModified;
    private FileStatus status;
    private List<String> tags;

    public FileMetadataResponse() {}

    public FileMetadataResponse(UUID id, String fileName, String contentType, long fileSize, LocalDateTime dateUploaded, LocalDateTime dateModified, FileStatus status) {
        this.id = id;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.dateUploaded = dateUploaded;
        this.dateModified = dateModified;
        this.status = status;
        this.tags = new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getDateUploaded() {
        return dateUploaded;
    }

    public void setDateUploaded(LocalDateTime dateUploaded) {
        this.dateUploaded = dateUploaded;
    }

    public LocalDateTime getDateModified() {
        return dateModified;
    }

    public void setDateModified(LocalDateTime dateModified) {
        this.dateModified = dateModified;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        this.status = status;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
