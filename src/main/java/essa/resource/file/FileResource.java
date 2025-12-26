package essa.resource.file;

import essa.dto.file.FileUploadResponse;

import java.util.UUID;
import java.net.URL;

import essa.dto.file.FileUploadRequest;
import essa.service.file.FileService;
import essa.dto.file.FileMetadataResponse;

import jakarta.validation.constraints.NotNull;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/file")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FileResource {

    @Inject
    FileService fileService;

    @POST
    @Path("/upload")
    @RolesAllowed({"user", "admin"})
    public Response uploadFile(@Valid @NotNull FileUploadRequest request) throws Exception {
        FileUploadResponse response = fileService.uploadFile(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @PUT
    @Path("/confirm/{id}")
    @RolesAllowed({"system", "admin"})
    public Response confirmUpload(@PathParam("id") String id) throws Exception {
        boolean success = fileService.confirmUpload(UUID.fromString(id));
        if (!success) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok().build();
    }

    @GET
    @Path("/download/{id}")
    @RolesAllowed({"user", "admin"})
    public Response downloadFile(@PathParam("id") String id) throws Exception {
        URL downloadUrl = fileService.downloadFile(UUID.fromString(id));
        if (downloadUrl == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(downloadUrl).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"user", "admin"})
    public Response getFileMetadata(@PathParam("id") String id) throws Exception {
        FileMetadataResponse response = fileService.getFileMetadata(UUID.fromString(id));
        if (response == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(response).build();
    }
}
 