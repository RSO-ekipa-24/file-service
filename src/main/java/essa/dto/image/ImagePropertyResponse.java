package essa.dto.image;

import java.util.UUID;
import java.net.URL;
import java.util.List;

public class ImagePropertyResponse {

    private UUID id;

    private URL imageUrl;

    private List<String> tags;

    public ImagePropertyResponse() {}

    public ImagePropertyResponse(UUID id, URL imageUrl, List<String> tags) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.tags = tags;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public URL getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(URL imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
