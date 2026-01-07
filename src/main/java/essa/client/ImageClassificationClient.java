package essa.client;

import io.quarkus.oidc.client.filter.OidcClientRequestFilter;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

@Path("/")
@RegisterRestClient(configKey = "image-classification")
@RegisterProvider(OidcClientRequestFilter.class) // adds Bearer <service-token>
public interface ImageClassificationClient {

    @POST
    @Path("/classify/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> classifyByUuid(@PathParam("uuid") String uuid);

    @POST
    @Path("/classify-url")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> classifyByUrl(Map<String, String> body);
}