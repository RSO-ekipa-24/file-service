package essa.dto.image;

import essa.entity.enums.LevelOfDetail;
import jakarta.validation.constraints.NotNull;

public class ImageDownloadRequest {

    @NotNull
    private String id;
    private String levelOfDetail;

    public ImageDownloadRequest() {}
    public ImageDownloadRequest(String id, String levelOfDetail) {
        this.id = id;
        this.levelOfDetail = levelOfDetail;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public LevelOfDetail getLevelOfDetail() {
        return LevelOfDetail.valueOf(levelOfDetail);
    }
    public void setLevelOfDetail(LevelOfDetail levelOfDetail) {
        this.levelOfDetail = levelOfDetail.name();
    }
    
}
