package essa.service.file;

import essa.dto.file.FileMetadataResponse;
import essa.dto.file.FileUploadRequest;
import essa.dto.file.FileUploadResponse;
import essa.entity.File;
import essa.entity.Tag;
import essa.entity.enums.FileStatus;
import essa.entity.enums.FileType;
import essa.repository.file.FileRepository;
import essa.repository.tag.TagRepository;
import essa.service.gcs.GcsService;
import essa.service.image.ImageClassificationService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URL;
import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    FileRepository fileRepository;
    @Mock
    TagRepository tagRepository;
    @Mock
    GcsService gcsService;
    @Mock
    ImageClassificationService imageClassificationService;
    @Mock
    SecurityIdentity securityIdentity;

    @Spy
    @InjectMocks
    FileService fileService;

    private static final String USER_ID = "user-123";

    @BeforeEach
    void setupPrincipal() {
        Principal principal = () -> USER_ID;
        lenient().when(securityIdentity.getPrincipal()).thenReturn(principal);
    }

    private FileUploadRequest buildUploadRequest(String fileName, String contentType, long size) {
        FileUploadRequest req = new FileUploadRequest();
        req.setFileName(fileName);
        req.setContentType(contentType);
        req.setSize(size);
        return req;
    }

    private File buildFile(UUID id, String ownerId, FileType type, FileStatus status, String bucket, String object) {
        File f = new File();
        f.setId(id);
        f.setOwnerKeycloakId(ownerId);
        f.setFileType(type);
        f.setStatus(status);
        f.setBucketName(bucket);
        f.setObjectName(object);
        f.setFileName("name");
        f.setContentType("application/octet-stream");
        f.setFileSize(10L);
        f.setCreated(OffsetDateTime.now());
        f.setModified(OffsetDateTime.now());
        return f;
    }

    @Test
    void uploadFile_success_generatesSignedUrl_andPersists() throws Exception {
        FileUploadRequest req = buildUploadRequest("doc.txt", "text/plain", 123L);
        when(gcsService.getPrivateBucketName()).thenReturn("private-bucket");
        URL putUrl = new URL("https://example.com/signed-put");
        when(gcsService.generateV4PutObjectSignedUrl(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(putUrl);

        doNothing().when(fileRepository).persist(any(File.class));

        LocalDateTime before = LocalDateTime.now();
        FileUploadResponse resp = fileService.uploadFile(req);
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(resp.getId());
        assertEquals(putUrl, resp.getUploadUrl());
        assertNotNull(resp.getExpiresAt());
        assertTrue(!resp.getExpiresAt().isBefore(before.plusMinutes(4)) && !resp.getExpiresAt().isAfter(after.plusMinutes(6)));

        verify(fileRepository, times(1)).persist(any(File.class));
        verify(gcsService, times(1)).generateV4PutObjectSignedUrl(eq("private-bucket"), anyString(), eq("text/plain"), eq(5));
    }

    @Test
    void handleFileUpload_invalidPropertyId_throwsBadRequest() throws Exception {
        FileUploadRequest req = buildUploadRequest("a.jpg", "image/jpeg", 10L);
        req.setPropertyId("abc"); // invalid
        URL putUrl = new URL("https://example.com/put");
        when(gcsService.generateV4PutObjectSignedUrl(anyString(), anyString(), anyString(), anyInt())).thenReturn(putUrl);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () ->
            fileService.handleFileUpload(USER_ID, UUID.randomUUID(), "bucket", "obj", FileType.FILE, req));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    void handleFileUpload_invalidTags_throwsBadRequest() throws Exception {
        FileUploadRequest req = buildUploadRequest("a.jpg", "image/jpeg", 10L);
        req.setTagNames(Arrays.asList("tag1", "tag2"));
        URL putUrl = new URL("https://example.com/put");
        when(gcsService.generateV4PutObjectSignedUrl(anyString(), anyString(), anyString(), anyInt())).thenReturn(putUrl);

        Tag t1 = new Tag();
        t1.setTagName("tag1");
        when(tagRepository.findApplicableTags(anyList(), eq(FileType.FILE), eq(USER_ID)))
                .thenReturn(new HashSet<>(Collections.singletonList(t1)));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () ->
            fileService.handleFileUpload(USER_ID, UUID.randomUUID(), "bucket", "obj", FileType.FILE, req));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    void deleteFile_storageFailure_throwsInternalError() throws Exception {
        UUID id = UUID.randomUUID();
        File file = buildFile(id, USER_ID, FileType.FILE, FileStatus.AVAILABLE, "bucket", "obj");
        when(fileRepository.findById(id)).thenReturn(file);
        when(gcsService.deleteObject("bucket", "obj")).thenReturn(false);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> fileService.deleteFile(id));
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    void confirmUploadTransaction_pending_becomesAvailable_andReturnsFile() throws Exception {
        UUID id = UUID.randomUUID();
        File file = buildFile(id, USER_ID, FileType.FILE, FileStatus.PENDING, "b", "o");
        when(fileRepository.findById(id)).thenReturn(file);

        File changed = fileService.confirmUploadTransaction(id);
        assertNotNull(changed);
        assertEquals(FileStatus.AVAILABLE, file.getStatus());
    }

    @Test
    void confirmUploadTransaction_available_returnsNull() throws Exception {
        UUID id = UUID.randomUUID();
        File file = buildFile(id, USER_ID, FileType.FILE, FileStatus.AVAILABLE, "b", "o");
        when(fileRepository.findById(id)).thenReturn(file);

        assertNull(fileService.confirmUploadTransaction(id));
    }

    @Test
    void confirmUploadTransaction_invalidStatus_throwsBadRequest() throws Exception {
        UUID id = UUID.randomUUID();
        File file = buildFile(id, USER_ID, FileType.FILE, FileStatus.DELETED, "b", "o");
        when(fileRepository.findById(id)).thenReturn(file);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> fileService.confirmUploadTransaction(id));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    void confirmUpload_triggersImageClassificationForImage() throws Exception {
        UUID id = UUID.randomUUID();
        File file = buildFile(id, USER_ID, FileType.IMAGE, FileStatus.PENDING, "bucket", "obj");
        // Spy: stub internal call to avoid actual repo usage
        doReturn(file).when(fileService).confirmUploadTransaction(id);
        URL publicUrl = new URL("https://example.com/public/img");
        when(gcsService.generatePublicObjectUrl("bucket", "obj")).thenReturn(publicUrl);

        fileService.confirmUpload(id);

        verify(imageClassificationService, times(1)).classifyAsync(publicUrl, id);
    }

    @Test
    void downloadFile_notAvailable_throwsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        File file = buildFile(id, USER_ID, FileType.FILE, FileStatus.PENDING, "b", "o");
        when(fileRepository.findById(id)).thenReturn(file);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> fileService.downloadFile(id));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    void downloadFile_signedUrlNull_throwsInternalError() throws Exception {
        UUID id = UUID.randomUUID();
        File file = buildFile(id, USER_ID, FileType.FILE, FileStatus.AVAILABLE, "b", "o");
        when(fileRepository.findById(id)).thenReturn(file);
        when(gcsService.generateV4GetObjectSignedUrl("b", "o", 5)).thenReturn(null);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> fileService.downloadFile(id));
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    void findFileWithPermissionCheck_forbiddenOwner_throwsForbidden() throws Exception {
        UUID id = UUID.randomUUID();
        File file = buildFile(id, "another-user", FileType.FILE, FileStatus.AVAILABLE, "b", "o");
        when(fileRepository.findById(id)).thenReturn(file);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> fileService.findFileWithPermissionCheck(id));
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    void findFileWithPermissionCheck_imageType_throwsBadRequest() throws Exception {
        UUID id = UUID.randomUUID();
        File file = buildFile(id, USER_ID, FileType.IMAGE, FileStatus.AVAILABLE, "b", "o");
        when(fileRepository.findById(id)).thenReturn(file);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> fileService.findFileWithPermissionCheck(id));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    void restoreFile_notDeleted_throwsBadRequest() throws Exception {
        UUID id = UUID.randomUUID();
        File file = buildFile(id, USER_ID, FileType.FILE, FileStatus.AVAILABLE, "b", "o");
        when(fileRepository.findById(id)).thenReturn(file);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> fileService.restoreFile(id));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    void getDeletedFiles_returnsMetadataList() throws Exception {
        File f1 = buildFile(UUID.randomUUID(), USER_ID, FileType.FILE, FileStatus.DELETED, "b", "o1");
        File f2 = buildFile(UUID.randomUUID(), USER_ID, FileType.FILE, FileStatus.DELETED, "b", "o2");
        when(fileRepository.findDeletedFilesByOwner(USER_ID)).thenReturn(Arrays.asList(f1, f2));

        List<FileMetadataResponse> list = fileService.getDeletedFiles();
        assertEquals(2, list.size());
    }

    @Test
    void getFileMetadata_returnsResponse() throws Exception {
        UUID id = UUID.randomUUID();
        File file = buildFile(id, USER_ID, FileType.FILE, FileStatus.AVAILABLE, "b", "o");
        when(fileRepository.findById(id)).thenReturn(file);

        FileMetadataResponse resp = fileService.getFileMetadata(id);
        assertNotNull(resp);
        assertEquals(id, resp.getId());
    }

    @Test
    void getAllFilesOfUser_returnsMetadataList() throws Exception {
        File f1 = buildFile(UUID.randomUUID(), USER_ID, FileType.FILE, FileStatus.AVAILABLE, "b", "o1");
        File f2 = buildFile(UUID.randomUUID(), USER_ID, FileType.FILE, FileStatus.AVAILABLE, "b", "o2");
        when(fileRepository.findAllFilesByOwner(USER_ID)).thenReturn(Arrays.asList(f1, f2));

        List<FileMetadataResponse> list = fileService.getAllFilesOfUser();
        assertEquals(2, list.size());
    }
}
