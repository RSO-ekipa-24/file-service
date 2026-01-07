package essa.service.image;

import essa.client.ImageClassificationClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.net.URL;

@ApplicationScoped
public class ImageClassificationService {

    @Inject
    @RestClient
    ImageClassificationClient client;

    public Map<String, Object> callByUuid(String uuid) {
        return client.classifyByUuid(uuid);
    }

    public Map<String, Object> callByUrl(URL imageUrl) {
        return client.classifyByUrl(Map.of("image_url", imageUrl.toString()));
    }
}
