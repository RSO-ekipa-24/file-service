package essa.resource.tag;

import essa.service.tag.TagService;
import essa.entity.Tag;
import essa.dto.tag.TagCreateRequest;
import essa.dto.tag.TagGetResponse;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.security.RolesAllowed;

import java.util.UUID;

@Path("/tag")
@Produces(MediaType.APPLICATION_JSON)
public class TagResource {
    
    @Inject
    TagService tagService;
    
    @Inject
    SecurityIdentity securityIdentity;

    @POST
    @Path("/create")
    @RolesAllowed({"user", "admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createTag(TagCreateRequest request) throws Exception {
        tagService.createTag(request);
        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/delete/{id}")
    @RolesAllowed({"user", "admin"})
    public Response deleteTag(@PathParam("id") String id) throws Exception {
        tagService.deleteTag(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET
    @Path("/image")
    @RolesAllowed({"user", "admin"})
    public Response getImageTags() throws Exception {
        TagGetResponse response = tagService.getImageTags();
        return Response.status(Response.Status.OK).entity(response).build();
    }
    
    @GET
    @Path("/file")
    @RolesAllowed({"user", "admin"})
    public Response getFileTags() throws Exception {
        TagGetResponse response = tagService.getFileTags();
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @POST
    @Path("{tagId}/add-to/{fileId}")
    @RolesAllowed({"user", "admin"})
    public Response addTagToFile(@PathParam("tagId") String tagId, @PathParam("fileId") String fileId) {
        tagService.addTagToFile(UUID.fromString(tagId), UUID.fromString(fileId));
        return Response.status(Response.Status.OK).build();
    }

    @DELETE
    @Path("{tagId}/remove-from/{fileId}")
    @RolesAllowed({"user", "admin"})
    public Response removeTagFromFile(@PathParam("tagId") String tagId, @PathParam("fileId") String fileId) {
        tagService.removeTagFromFile(UUID.fromString(tagId), UUID.fromString(fileId));
        return Response.status(Response.Status.NO_CONTENT).build();
    }
}