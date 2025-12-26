package essa.resource.file;

import essa.repository.file.TagRepository;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;


@Path("/tag")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TagResource {
    
    @Inject
    TagRepository tagRepository;
    
    @Inject
    SecurityIdentity securityIdentity;

    
    
}
