package essa.resource.tag;

import essa.dto.tag.TagCreateRequest;
import essa.dto.tag.TagGetResponse;
import essa.service.tag.TagService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import java.util.List;
import java.util.UUID;

@Path("/tag")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "BearerAuth")
public class TagResource {

    private static final String METRIC_PREFIX = "essa.fileService.tags";

    @Inject
    TagService tagService;

    @Inject
    SecurityIdentity securityIdentity;

    @POST
    @Path("/create")
    @RolesAllowed({"user", "admin"})
    @Timed(value = METRIC_PREFIX + ".createTag.time", description = "Time spent creating tag (file-service)")
    @Counted(value = METRIC_PREFIX + ".createTag.count", description = "Number of calls to create tag (file-service)")
    @Operation(
            summary = "Create tag",
            description = "Creates a new tag."
    )
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Created"),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response createTag(
            @Parameter(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TagCreateRequest.class),
                            examples = @ExampleObject(
                                    name = "CreateTag",
                                    value = "{\"name\":\"Apartment\",\"color\":\"#FFAA00\"}"
                            )
                    )
            )
            TagCreateRequest request
    ) throws Exception {
        tagService.createTag(request);
        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/delete/{id}")
    @RolesAllowed({"user", "admin"})
    @Timed(value = METRIC_PREFIX + ".deleteTag.time", description = "Time spent deleting tag (file-service)")
    @Counted(value = METRIC_PREFIX + ".deleteTag.count", description = "Number of calls to delete tag (file-service)")
    @Operation(
            summary = "Delete tag",
            description = "Deletes a tag by its id."
    )
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Deleted"),
            @APIResponse(responseCode = "400", description = "Invalid UUID"),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "Tag not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response deleteTag(
            @Parameter(description = "Tag UUID", required = true, example = "2f3a6b7c-1f2e-4a5b-9c0d-1b2c3d4e5f60")
            @PathParam("id") String id
    ) throws Exception {
        tagService.deleteTag(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET
    @Path("/image")
    @RolesAllowed({"user", "admin"})
    @Timed(value = METRIC_PREFIX + ".getImageTags.time", description = "Time spent getting image tags (file-service)")
    @Counted(value = METRIC_PREFIX + ".getImageTags.count", description = "Number of calls to get image tags (file-service)")
    @Operation(
            summary = "Get image tags",
            description = "Returns tags available for images."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Image tags",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TagGetResponse.class)
                    )
            ),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getImageTags() throws Exception {
        TagGetResponse response = tagService.getImageTags();
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @GET
    @Path("/image-system")
    @RolesAllowed({"system", "admin"})
    @Timed(value = METRIC_PREFIX + ".getImageSystemTags.time", description = "Time spent getting image system tags (file-service)")
    @Counted(value = METRIC_PREFIX + ".getImageSystemTags.count", description = "Number of calls to get image system tags (file-service)")
    @Operation(
            summary = "Get image system tags",
            description = "Returns system tags for images (system/admin only)."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "System tags",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(type = SchemaType.ARRAY, implementation = String.class)
                    )
            ),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getImageSystemTags() throws Exception {
        List<String> response = tagService.getImageSystemTags();
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @GET
    @Path("/file")
    @RolesAllowed({"user", "admin"})
    @Timed(value = METRIC_PREFIX + ".getFileTags.time", description = "Time spent getting file tags (file-service)")
    @Counted(value = METRIC_PREFIX + ".getFileTags.count", description = "Number of calls to get file tags (file-service)")
    @Operation(
            summary = "Get file tags",
            description = "Returns tags available for files."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "File tags",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TagGetResponse.class)
                    )
            ),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getFileTags() throws Exception {
        TagGetResponse response = tagService.getFileTags();
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @POST
    @Path("{tagId}/add-to/{fileId}")
    @RolesAllowed({"user", "admin"})
    @Timed(value = METRIC_PREFIX + ".addTagToFile.time", description = "Time spent adding tag to file (file-service)")
    @Counted(value = METRIC_PREFIX + ".addTagToFile.count", description = "Number of calls to add tag to file (file-service)")
    @Operation(
            summary = "Add tag to file",
            description = "Adds a tag to a file."
    )
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Added"),
            @APIResponse(responseCode = "400", description = "Invalid UUID"),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "Tag or file not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response addTagToFile(
            @Parameter(description = "Tag UUID", required = true, example = "2f3a6b7c-1f2e-4a5b-9c0d-1b2c3d4e5f60")
            @PathParam("tagId") String tagId,
            @Parameter(description = "File UUID", required = true, example = "1a2b3c4d-5e6f-4a3b-9c8d-0e1f2a3b4c5d")
            @PathParam("fileId") String fileId
    ) {
        tagService.addTagToFile(UUID.fromString(tagId), UUID.fromString(fileId));
        return Response.status(Response.Status.OK).build();
    }

    @DELETE
    @Path("{tagId}/remove-from/{fileId}")
    @RolesAllowed({"user", "admin"})
    @Timed(value = METRIC_PREFIX + ".removeTagFromFile.time", description = "Time spent removing tag from file (file-service)")
    @Counted(value = METRIC_PREFIX + ".removeTagFromFile.count", description = "Number of calls to remove tag from file (file-service)")
    @Operation(
            summary = "Remove tag from file",
            description = "Removes a tag from a file."
    )
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Removed"),
            @APIResponse(responseCode = "400", description = "Invalid UUID"),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "Tag or file not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response removeTagFromFile(
            @Parameter(description = "Tag UUID", required = true, example = "2f3a6b7c-1f2e-4a5b-9c0d-1b2c3d4e5f60")
            @PathParam("tagId") String tagId,
            @Parameter(description = "File UUID", required = true, example = "1a2b3c4d-5e6f-4a3b-9c8d-0e1f2a3b4c5d")
            @PathParam("fileId") String fileId
    ) {
        tagService.removeTagFromFile(UUID.fromString(tagId), UUID.fromString(fileId));
        return Response.status(Response.Status.NO_CONTENT).build();
    }
}