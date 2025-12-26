package essa.resource.image;

import java.net.URL;
import java.util.UUID;

import essa.dto.file.FileUploadRequest;
import essa.dto.file.FileUploadResponse;
import essa.dto.image.ImageDownloadRequest;
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
@Consumes(MediaType.APPLICATION_JSON)
public class ImageResource {
    
    @Inject
    ImageService imageService;

    @Inject 
    FileService fileService;

    @POST
    @Path("/upload")
    @RolesAllowed({"user", "admin"})
    public Response uploadImage(@Valid @NotNull FileUploadRequest request) throws Exception {
        FileUploadResponse response = imageService.uploadImage(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/download")
    @RolesAllowed({"user", "admin"})
    public Response downloadFile(@Valid @NotNull ImageDownloadRequest request) throws Exception {
        URL downloadUrl = imageService.downloadImage(UUID.fromString(request.getId()), request.getLevelOfDetail());
        if (downloadUrl == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(downloadUrl).build();
    }    
}
