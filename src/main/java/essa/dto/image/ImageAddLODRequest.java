package essa.dto.image;

import java.util.UUID;

import essa.entity.enums.LevelOfDetail;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public class ImageAddLODRequest {

    @NotBlank(message = "File id is required")
    private String fileId;

    @NotNull(message = "Level of detail is required")
    private LevelOfDetail levelOfDetail;

    @NotNull(message = "Size is required")
    @Min(value = 1, message = "Size must be non-negative")
    @Max(value = 1073741824, message = "Size must be less than or equal to 1GB")
    long fileSize;

    public ImageAddLODRequest() {}

    public ImageAddLODRequest(String fileId, LevelOfDetail levelOfDetail) {
        this.fileId = fileId;
        this.levelOfDetail = levelOfDetail;
    }

    public UUID getFileId() {
        return UUID.fromString(fileId);
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId.toString();
    }

    public LevelOfDetail getLevelOfDetail() {
        return levelOfDetail;
    }

    public void setLevelOfDetail(LevelOfDetail levelOfDetail) {
        this.levelOfDetail = levelOfDetail;
    }
    
    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
