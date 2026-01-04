package essa.dto.image;

import java.util.UUID;

import essa.entity.enums.LevelOfDetail;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public class ImageAddLODRequest {

    @NotBlank(message = "File id is required")
    private String id;

    @NotNull(message = "Level of detail is required")
    private LevelOfDetail levelOfDetail;

    @NotBlank(message = "Object name is required")
    private String objectName;

    @NotNull(message = "Size is required")
    @Min(value = 1, message = "Size must be non-negative")
    @Max(value = 1073741824, message = "Size must be less than or equal to 1GB")
    long size; // in bytes

    public ImageAddLODRequest() {}

    public ImageAddLODRequest(String id, LevelOfDetail levelOfDetail) {
        this.id = id;
        this.levelOfDetail = levelOfDetail;
    }

    public UUID getId() {
        return UUID.fromString(id);
    }

    public void setId(UUID id) {
        this.id = id.toString();
    }

    public LevelOfDetail getLevelOfDetail() {
        return levelOfDetail;
    }

    public void setLevelOfDetail(LevelOfDetail levelOfDetail) {
        this.levelOfDetail = levelOfDetail;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }
    
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
