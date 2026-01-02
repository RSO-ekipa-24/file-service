package essa.dto.image;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class ImagePreviewQuery {

    private UUID id;
    private String bucketName;
    private String objectName;
    private List<String> tags;

    public ImagePreviewQuery(
            UUID id,
            String bucketName,
            String objectName,
            List<String> tags
    ) {
        this.id = id;
        this.bucketName = bucketName;
        this.objectName = objectName;
        this.tags = tags == null ? List.of() : tags;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
