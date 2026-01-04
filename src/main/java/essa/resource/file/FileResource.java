package essa.resource.file;

import essa.dto.file.FileUploadResponse;

import java.util.UUID;
import java.net.URL;
import java.util.List;

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
public class FileResource {

    @Inject
    FileService fileService;

    @GET
    @RolesAllowed({"user", "admin"})
    public Response getAllFilesOfUser() throws Exception {
        List<FileMetadataResponse> files = fileService.getAllFilesOfUser();
        return Response.status(Response.Status.OK).entity(files).build();
    }

    @POST
    @Path("/upload")
    @RolesAllowed({"user", "admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response uploadFile(@Valid @NotNull FileUploadRequest request) throws Exception {
        FileUploadResponse response = fileService.uploadFile(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @DELETE
    @Path("/delete/{id}")
    @RolesAllowed({"user", "admin"})
    public Response deleteFile(@PathParam("id") String id) throws Exception {
        fileService.deleteFile(UUID.fromString(id));
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET
    @Path("/deleted")
    @RolesAllowed({"user", "admin"})
    public Response getDeletedFiles() throws Exception {
        List<FileMetadataResponse> deletedFiles = fileService.getDeletedFiles();
        return Response.status(Response.Status.OK).entity(deletedFiles).build();
    }

    @POST
    @Path("/restore/{id}")
    @RolesAllowed({"user", "admin"})
    public Response restoreFile(@PathParam("id") String id) throws Exception {
        fileService.restoreFile(UUID.fromString(id));
        return Response.status(Response.Status.OK).build();
    }

    @PUT
    @Path("/confirm/{id}")
    @RolesAllowed({"system", "admin"})
    public Response confirmUpload(@PathParam("id") String id) throws Exception {
        fileService.confirmUpload(UUID.fromString(id));
        return Response.ok().build();
    }

    @GET
    @Path("/download/{id}")
    @RolesAllowed({"user", "admin"})
    public Response downloadFile(@PathParam("id") String id) throws Exception {
        URL downloadUrl = fileService.downloadFile(UUID.fromString(id));
        return Response.ok(downloadUrl).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"user", "admin"})
    public Response getFileMetadata(@PathParam("id") String id) throws Exception {
        FileMetadataResponse response = fileService.getFileMetadata(UUID.fromString(id));
        return Response.ok(response).build();
    }
}
 