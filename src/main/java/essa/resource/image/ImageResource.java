package essa.resource.image;

import essa.dto.file.FileUploadRequest;
import essa.dto.file.FileUploadResponse;
import essa.dto.image.ImageAddLODRequest;
import essa.dto.image.ImagePropertyResponse;
import essa.dto.image.PropertyThumbnailsResponse;
import essa.entity.enums.LevelOfDetail;
import essa.service.file.FileService;
import essa.service.image.ImageService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

import java.net.URL;
import java.util.List;
import java.util.UUID;

@Path("/image")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "BearerAuth")
public class ImageResource {

    private static final String METRIC_PREFIX = "essa.images";

    @Inject
    ImageService imageService;

    @Inject
    FileService fileService;

    @GET
    @Path("/download/{id}")
    @Timed(value = METRIC_PREFIX + ".downloadImage.time", description = "Time spent generating image download URL")
    @Counted(value = METRIC_PREFIX + ".downloadImage.count", description = "Number of calls to download image (public)")
    @Operation(
            summary = "Download image",
            description = "Returns a download URL for an image. Public endpoint."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Download URL",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = URL.class),
                            examples = @ExampleObject(
                                    name = "DownloadUrl",
                                    value = "\"https://storage.googleapis.com/bucket/path/to/file\""
                            )
                    )
            ),
            @APIResponse(responseCode = "400", description = "Invalid UUID or parameters"),
            @APIResponse(responseCode = "404", description = "Image not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response downloadFile(
            @Parameter(description = "Image UUID", required = true, example = "1a2b3c4d-5e6f-4a3b-9c8d-0e1f2a3b4c5d")
            @PathParam("id") String id,
            @Parameter(description = "Optional level of detail", required = false)
            @QueryParam("lod") LevelOfDetail levelOfDetail
    ) throws Exception {
        URL downloadUrl = imageService.downloadImage(UUID.fromString(id), levelOfDetail);
        return Response.status(Response.Status.OK).entity(downloadUrl).build();
    }

    @GET
    @Path("/property/{propertyId}")
    @Timed(value = METRIC_PREFIX + ".getImagesOfProperty.time", description = "Time spent getting images of a property")
    @Counted(value = METRIC_PREFIX + ".getImagesOfProperty.count", description = "Number of calls to get images of a property (public)")
    @Operation(
            summary = "Get images of property",
            description = "Returns images for a property. Public endpoint."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Images",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ImagePropertyResponse.class, type = SchemaType.ARRAY)
                    )
            ),
            @APIResponse(responseCode = "400", description = "Invalid parameters"),
            @APIResponse(responseCode = "404", description = "Property/images not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getImagesOfProperty(
            @Parameter(description = "Property id", required = true, example = "10")
            @PathParam("propertyId") Long propertyId,
            @Parameter(description = "Optional level of detail", required = false)
            @QueryParam("lod") LevelOfDetail levelOfDetail
    ) throws Exception {
        List<ImagePropertyResponse> imageIds = imageService.getImagesOfProperty(propertyId, levelOfDetail);
        return Response.status(Response.Status.OK).entity(imageIds).build();
    }

    @POST
    @Path("/upload")
    @RolesAllowed({"user", "admin"})
    @Timed(value = METRIC_PREFIX + ".uploadImage.time", description = "Time spent uploading image")
    @Counted(value = METRIC_PREFIX + ".uploadImage.count", description = "Number of calls to upload image")
    @Operation(
            summary = "Upload image",
            description = "Creates an image upload entry and returns upload metadata/URL."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "201",
                    description = "Created",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FileUploadResponse.class)
                    )
            ),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response uploadImage(
            @Parameter(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FileUploadRequest.class),
                            examples = @ExampleObject(
                                    name = "UploadImage",
                                    value = "{\"fileName\":\"front.jpg\",\"contentType\":\"image/jpeg\",\"size\":123456,\"propertyId\":10}"
                            )
                    )
            )
            @Valid @NotNull FileUploadRequest request
    ) throws Exception {
        FileUploadResponse response = imageService.uploadImage(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @DELETE
    @Path("/delete/{id}")
    @RolesAllowed({"user", "admin"})
    @Timed(value = METRIC_PREFIX + ".deleteImage.time", description = "Time spent deleting image")
    @Counted(value = METRIC_PREFIX + ".deleteImage.count", description = "Number of calls to delete image")
    @Operation(
            summary = "Delete image",
            description = "Hard-deletes an image by id."
    )
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Deleted"),
            @APIResponse(responseCode = "400", description = "Invalid UUID"),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "Image not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response deleteImage(
            @Parameter(description = "Image UUID", required = true, example = "1a2b3c4d-5e6f-4a3b-9c8d-0e1f2a3b4c5d")
            @PathParam("id") String id
    ) throws Exception {
        imageService.hardDeleteImage(UUID.fromString(id));
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @DELETE
    @Path("/delete-property-images/{propertyId}")
    @RolesAllowed({"user", "admin"})
    @Timed(value = METRIC_PREFIX + ".deletePropertyImages.time", description = "Time spent deleting property images")
    @Counted(value = METRIC_PREFIX + ".deletePropertyImages.count", description = "Number of calls to delete property images")
    @Operation(
            summary = "Delete property images",
            description = "Deletes all images associated with a property."
    )
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Deleted"),
            @APIResponse(responseCode = "400", description = "Invalid parameters"),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "Property/images not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response deletePropertyImages(
            @Parameter(description = "Property id", required = true, example = "10")
            @PathParam("propertyId") Long propertyId
    ) throws Exception {
        imageService.deleteImagesOfProperty(propertyId);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @POST
    @Path("/add-level-of-detail")
    @RolesAllowed({"system", "admin"})
    @Timed(value = METRIC_PREFIX + ".addLevelOfDetail.time", description = "Time spent adding level of detail to image")
    @Counted(value = METRIC_PREFIX + ".addLevelOfDetail.count", description = "Number of calls to add level of detail to image")
    @Operation(
            summary = "Add level of detail",
            description = "Adds a new level-of-detail (LOD) for an existing image."
    )
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Updated"),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "Image not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response addLevelOfDetail(
            @Parameter(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ImageAddLODRequest.class),
                            examples = @ExampleObject(
                                    name = "AddLOD",
                                    value = "{\"imageId\":\"1a2b3c4d-5e6f-4a3b-9c8d-0e1f2a3b4c5d\",\"lod\":\"THUMBNAIL\",\"fileName\":\"thumb.jpg\",\"contentType\":\"image/jpeg\",\"size\":12345}"
                            )
                    )
            )
            @Valid @NotNull ImageAddLODRequest request
    ) throws Exception {
        imageService.addLevelOfDetailToImage(request);
        return Response.status(Response.Status.OK).build();
    }

    @GET
    @Path("/thumbnails")
    @RolesAllowed({"user", "admin"})
    @Timed(value = METRIC_PREFIX + ".getThumbnailsForProperties.time", description = "Time spent getting thumbnails for properties")
    @Counted(value = METRIC_PREFIX + ".getThumbnailsForProperties.count", description = "Number of calls to get thumbnails for properties")
    @Operation(
            summary = "Get thumbnails for properties",
            description = "Returns thumbnails for the provided property ids."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Thumbnails",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PropertyThumbnailsResponse.class, type = SchemaType.ARRAY)
                    )
            ),
            @APIResponse(responseCode = "400", description = "Invalid parameters"),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getThumbnailsForProperties(
            @Parameter(description = "Property ids (repeat query param: ?propertyId=1&propertyId=2)", required = true)
            @QueryParam("propertyId") List<Long> propertyIds
    ) throws Exception {
        List<PropertyThumbnailsResponse> response = imageService.getThumbnailsForProperties(propertyIds);
        return Response.status(Response.Status.OK).entity(response).build();
    }
}