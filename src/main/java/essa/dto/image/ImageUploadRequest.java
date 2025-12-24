package essa.dto.image;

import essa.dto.file.FileUploadRequest;
import essa.entity.LevelOfDetail;

public class ImageUploadRequest extends FileUploadRequest{
    private LevelOfDetail levelOfDetail;
    
    public ImageUploadRequest() {}
    
    public ImageUploadRequest(String filename, String contentType, Long size, String[] tagNames, LevelOfDetail levelOfDetail) {
        super(filename, contentType, size, tagNames);
        this.levelOfDetail = levelOfDetail;
    }
    
    
    public LevelOfDetail getLevelOfDetail() {
        return levelOfDetail;
    }
    
    public void setLevelOfDetail(LevelOfDetail levelOfDetail) {
        this.levelOfDetail = levelOfDetail;
    }
}
