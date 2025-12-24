package essa.resource.file;

import essa.dto.file.FileUploadResponse;
import essa.dto.file.FileUploadRequest;
import essa.service.file.FileService;
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
}
