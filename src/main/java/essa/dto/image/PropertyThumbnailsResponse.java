package essa.dto.image;

import java.net.URL;

public class PropertyThumbnailsResponse {
    private Long propertyId;
    private URL thumbnailUrl;

    public PropertyThumbnailsResponse() {}

    public PropertyThumbnailsResponse(Long propertyId, URL thumbnailUrl) {
        this.propertyId = propertyId;
        this.thumbnailUrl = thumbnailUrl;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public URL getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(URL thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
}