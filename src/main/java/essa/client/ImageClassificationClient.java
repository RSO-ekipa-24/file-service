package essa.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.client.filter.OidcClientFilter;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/")
@RegisterRestClient(configKey = "image-classification")
@OidcClientFilter
public interface ImageClassificationClient {

    @POST
    @Path("/classify/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    List<String> classifyByUuid(@PathParam("uuid") String uuid);

    @POST
    @Path("/classify-url")
    @Produces(MediaType.APPLICATION_JSON)
    List<String> classifyByUrl(@QueryParam("image_url") String imageUrl);
}