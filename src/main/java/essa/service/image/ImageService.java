package essa.service.image;

import essa.entity.Image;
import essa.entity.LevelOfDetail;
import essa.entity.Tag;
import essa.repository.file.TagRepository;
import essa.repository.image.ImageRepository;
import essa.service.gcs.GcsService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ImageService {
    
    @Inject
    ImageRepository imageRepository;
    
    @Inject
    TagRepository tagRepository;
    
    @Inject
    GcsService gcsService;
    
    @Inject
    SecurityIdentity securityIdentity;
}
