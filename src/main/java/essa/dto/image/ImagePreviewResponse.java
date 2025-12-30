package essa.dto.image;

import java.util.UUID;
import java.net.URL;
import java.util.List;

public class ImagePreviewResponse {

    private UUID id;

    private URL previewUrl;

    private List<String> tags;

    public ImagePreviewResponse() {}

    public ImagePreviewResponse(UUID id, URL previewUrl, List<String> tags) {
        this.id = id;
        this.previewUrl = previewUrl;
        this.tags = tags;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public URL getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(URL previewUrl) {
        this.previewUrl = previewUrl;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
