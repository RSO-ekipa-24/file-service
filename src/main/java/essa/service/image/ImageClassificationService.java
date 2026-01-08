package essa.service.image;

import essa.client.ImageClassificationClient;
import essa.service.tag.TagService;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import org.jboss.logging.Logger;

import java.util.List;
import java.net.URL;
import java.util.UUID;

@ApplicationScoped
public class ImageClassificationService {

    @Inject
    @RestClient
    ImageClassificationClient client;

    @Inject
    TagService tagService;

    @Inject
    ManagedExecutor managedExecutor;

    private static final Logger LOG = Logger.getLogger(ImageClassificationService.class);

    public void classifyAsync(URL imageUrl, UUID uuid) {
        managedExecutor.execute(() -> {
            try {
                List<String> result = client.classifyByUrl(imageUrl.toString());
                LOG.infof("Tags assigned to %s with automatic classification: %s", uuid.toString(), String.join(", ", result));
                tagService.applyTagsToImage(uuid, result);
            } catch (ClientWebApplicationException e) {
                var resp = e.getResponse();
                int status = resp != null ? resp.getStatus() : -1;
                LOG.warnf(e, "Image classification HTTP error (%d) for %s", status, imageUrl);
            } catch (ProcessingException e) {
                LOG.errorf(e, "Image classification transport error for %s", imageUrl);
            } catch (Exception e) {
                LOG.errorf(e, "Unexpected error during image classification for %s", imageUrl);
            }
        });
    }
}
