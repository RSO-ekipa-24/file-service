package essa.resource.image;

import java.net.URL;
import java.util.UUID;
import java.util.List;

import essa.dto.file.FileUploadRequest;
import essa.dto.file.FileUploadResponse;
import essa.dto.image.ImageAddLODRequest;
import essa.dto.image.PropertyThumbnailsResponse;
import essa.dto.image.ImagePropertyResponse;
import essa.entity.enums.LevelOfDetail;
import essa.service.image.ImageService;
import essa.service.file.FileService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/image")
@Produces(MediaType.APPLICATION_JSON)
public class ImageResource {
    
    @Inject
    ImageService imageService;

    @Inject 
    FileService fileService;

    @POST
    @Path("/upload")
    @RolesAllowed({"user", "admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response uploadImage(@Valid @NotNull FileUploadRequest request) throws Exception {
        FileUploadResponse response = imageService.uploadImage(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @DELETE
    @Path("/delete/{id}")
    @RolesAllowed({"user", "admin"})
    public Response deleteImage(@PathParam("id") String id) throws Exception {
        imageService.hardDeleteImage(UUID.fromString(id));
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @POST
    @Path("/add-levels-of-detail")
    @RolesAllowed({"system", "admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addLevelsOfDetail(@Valid @NotNull ImageAddLODRequest request) throws Exception {
        imageService.addLevelOfDetailToImage(request);
        return Response.status(Response.Status.OK).build();
    }

    @GET
    @Path("/download/{id}")
    public Response downloadFile(@PathParam("id") String id, @QueryParam("lod") LevelOfDetail levelOfDetail) throws Exception {
        URL downloadUrl = imageService.downloadImage(UUID.fromString(id), levelOfDetail);
        return Response.status(Response.Status.OK).entity(downloadUrl).build();
    }

    @GET
    @Path("/thumbnails")
    @RolesAllowed({"user", "admin"})
    public Response getThumbnailsForProperties(@QueryParam("propertyId") List<Long> propertyIds) throws Exception {
        List<PropertyThumbnailsResponse> response = imageService.getThumbnailsForProperties(propertyIds);
        return Response.status(Response.Status.OK).entity(response).build();
    }

    @GET
    @Path("/property/{propertyId}")
    public Response getImagesForProperty(@PathParam("propertyId") Long propertyId, @QueryParam("lod") LevelOfDetail levelOfDetail) throws Exception {
        List<ImagePropertyResponse> imageIds = imageService.getImagesForProperty(propertyId, levelOfDetail);
        return Response.status(Response.Status.OK).entity(imageIds).build();
    }
    
} 