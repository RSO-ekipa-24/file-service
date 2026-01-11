package essa.resource.file;

import essa.dto.file.FileMetadataResponse;
import essa.dto.file.FileUploadRequest;
import essa.dto.file.FileUploadResponse;
import essa.service.file.FileService;
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

@Path("/file")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "BearerAuth")
public class FileResource {

    private static final String METRIC_PREFIX = "essa.files";

    @Inject
    FileService fileService;

    @GET
    @RolesAllowed({"user", "admin"})
    @Timed(value = METRIC_PREFIX + ".getAllFilesOfUser.time", description = "Time spent getting all files of authenticated user")
    @Counted(value = METRIC_PREFIX + ".getAllFilesOfUser.count", description = "Number of calls to get all files of authenticated user")
    @Operation(
            summary = "Get all files of authenticated user",
            description = "Returns metadata for all files belonging to the authenticated user."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "List of files",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FileMetadataResponse.class, type = SchemaType.ARRAY)
                    )
            ),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getAllFilesOfUser() throws Exception {
        List<FileMetadataResponse> files = fileService.getAllFilesOfUser();
        return Response.status(Response.Status.OK).entity(files).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"user", "system", "admin"})
    @Timed(value = METRIC_PREFIX + ".getFileMetadata.time", description = "Time spent getting file metadata")
    @Counted(value = METRIC_PREFIX + ".getFileMetadata.count", description = "Number of calls to get file metadata")
    @Operation(
            summary = "Get file metadata",
            description = "Returns metadata for a file by its id."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "File metadata",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FileMetadataResponse.class)
                    )
            ),
            @APIResponse(responseCode = "400", description = "Invalid UUID"),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "File not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getFileMetadata(
            @Parameter(description = "File UUID", required = true, example = "2f3a6b7c-1f2e-4a5b-9c0d-1b2c3d4e5f60")
            @PathParam("id") String id
    ) throws Exception {
        FileMetadataResponse response = fileService.getFileMetadata(UUID.fromString(id));
        return Response.ok(response).build();
    }

    @POST
    @Path("/upload")
    @RolesAllowed({"user", "admin"})
    @Timed(value = METRIC_PREFIX + ".uploadFile.time", description = "Time spent uploading file (create metadata + upload flow)")
    @Counted(value = METRIC_PREFIX + ".uploadFile.count", description = "Number of calls to upload file (create metadata + upload flow)")
    @Operation(
            summary = "Upload file (create metadata + upload flow)",
            description = "Creates file metadata and/or initiates upload flow (implementation-specific)."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "201",
                    description = "Upload created",
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
    public Response uploadFile(
            @Valid @NotNull FileUploadRequest request
    ) throws Exception {
        FileUploadResponse response = fileService.uploadFile(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @DELETE
    @Path("/delete/{id}")
    @RolesAllowed({"user", "admin"})
    @Timed(value = METRIC_PREFIX + ".deleteFile.time", description = "Time spent soft deleting file")
    @Counted(value = METRIC_PREFIX + ".deleteFile.count", description = "Number of calls to soft delete file")
    @Operation(
            summary = "Soft delete file",
            description = "Marks a file as deleted (implementation-specific)."
    )
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Deleted"),
            @APIResponse(responseCode = "400", description = "Invalid UUID"),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "File not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response deleteFile(
            @Parameter(description = "File UUID", required = true, example = "2f3a6b7c-1f2e-4a5b-9c0d-1b2c3d4e5f60")
            @PathParam("id") String id
    ) throws Exception {
        fileService.deleteFile(UUID.fromString(id));
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET
    @Path("/deleted")
    @RolesAllowed({"user", "admin"})
    @Timed(value = METRIC_PREFIX + ".getDeletedFiles.time", description = "Time spent getting deleted files")
    @Counted(value = METRIC_PREFIX + ".getDeletedFiles.count", description = "Number of calls to get deleted files")
    @Operation(
            summary = "Get deleted files",
            description = "Returns metadata for deleted files belonging to the authenticated user."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "List of deleted files",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FileMetadataResponse.class, type = SchemaType.ARRAY)
                    )
            ),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getDeletedFiles() throws Exception {
        List<FileMetadataResponse> deletedFiles = fileService.getDeletedFiles();
        return Response.status(Response.Status.OK).entity(deletedFiles).build();
    }

    @POST
    @Path("/restore/{id}")
    @RolesAllowed({"user", "admin"})
    @Timed(value = METRIC_PREFIX + ".restoreFile.time", description = "Time spent restoring deleted file")
    @Counted(value = METRIC_PREFIX + ".restoreFile.count", description = "Number of calls to restore deleted file")
    @Operation(
            summary = "Restore deleted file",
            description = "Restores a previously deleted file."
    )
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Restored"),
            @APIResponse(responseCode = "400", description = "Invalid UUID"),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "File not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response restoreFile(
            @Parameter(description = "File UUID", required = true, example = "2f3a6b7c-1f2e-4a5b-9c0d-1b2c3d4e5f60")
            @PathParam("id") String id
    ) throws Exception {
        fileService.restoreFile(UUID.fromString(id));
        return Response.status(Response.Status.OK).build();
    }

    @PUT
    @Path("/confirm/{id}")
    @RolesAllowed({"system", "admin"})
    @Timed(value = METRIC_PREFIX + ".confirmUpload.time", description = "Time spent confirming upload")
    @Counted(value = METRIC_PREFIX + ".confirmUpload.count", description = "Number of calls to confirm upload")
    @Operation(
            summary = "Confirm upload",
            description = "Confirms that a file upload has completed (system/admin only)."
    )
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Confirmed"),
            @APIResponse(responseCode = "400", description = "Invalid UUID"),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "File not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response confirmUpload(
            @Parameter(description = "File UUID", required = true, example = "2f3a6b7c-1f2e-4a5b-9c0d-1b2c3d4e5f60")
            @PathParam("id") String id
    ) throws Exception {
        fileService.confirmUpload(UUID.fromString(id));
        return Response.ok().build();
    }

    @GET
    @Path("/download/{id}")
    @RolesAllowed({"user", "admin"})
    @Timed(value = METRIC_PREFIX + ".downloadFile.time", description = "Time spent generating download URL")
    @Counted(value = METRIC_PREFIX + ".downloadFile.count", description = "Number of calls to generate download URL")
    @Operation(
            summary = "Get download URL",
            description = "Returns a signed download URL for the file."
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
                                    value = "\"https://example.com/signed-url\""
                            )
                    )
            ),
            @APIResponse(responseCode = "400", description = "Invalid UUID"),
            @APIResponse(responseCode = "401", description = "Missing/invalid token"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "File not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response downloadFile(
            @Parameter(description = "File UUID", required = true, example = "2f3a6b7c-1f2e-4a5b-9c0d-1b2c3d4e5f60")
            @PathParam("id") String id
    ) throws Exception {
        URL downloadUrl = fileService.downloadFile(UUID.fromString(id));
        return Response.ok(downloadUrl).build();
    }
}