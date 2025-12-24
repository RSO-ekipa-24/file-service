package essa.dto.file;
import java.util.UUID;
import java.net.URL;
import java.time.LocalDateTime;

public class FileUploadResponse {
    private UUID id;
    private URL uploadUrl;
    private LocalDateTime expiresAt;

    public FileUploadResponse() {}

    public FileUploadResponse(UUID id, URL uploadUrl, LocalDateTime expiresAt) {
        this.id = id;
        this.uploadUrl = uploadUrl;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public URL getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(URL uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
