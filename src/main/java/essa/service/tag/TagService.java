package essa.service.tag;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;
import java.util.List;

import essa.dto.tag.TagCreateRequest;
import essa.dto.tag.TagGetResponse;
import essa.entity.Tag;
import essa.entity.File;
import essa.entity.enums.FileType;
import essa.repository.tag.TagRepository;
import essa.repository.file.FileRepository;

@ApplicationScoped
public class TagService {

    @Inject
    TagRepository tagRepository;

    @Inject 
    FileRepository fileRepository;

    @Inject
    SecurityIdentity securityIdentity;

    @Transactional
    public void createTag(TagCreateRequest request) throws Exception {
        String keycloakId = securityIdentity.getPrincipal().getName();
        UUID uuid = UUID.randomUUID();

        Tag tag = new Tag();
        tag.setId(uuid);
        tag.setTagName(request.getTagName());
        tag.setOwnerKeycloakId(keycloakId);
        tag.setFileType(request.getFileType());

        tagRepository.persist(tag);
    }

    @Transactional
    public void deleteTag(String id) throws Exception {
        String keycloakId = securityIdentity.getPrincipal().getName();
        UUID uuid = UUID.fromString(id);

        Tag tag = tagRepository.findById(uuid);
        if (tag == null) {
            throw new WebApplicationException("Tag not found", Response.Status.NOT_FOUND);
        }

        if (!tag.getOwnerKeycloakId().equals(keycloakId)) {
            throw new WebApplicationException("Forbidden", Response.Status.FORBIDDEN);
        }

        tagRepository.delete(tag);
    }

    @Transactional
    public TagGetResponse getTags(FileType fileType) throws Exception {
        String keycloakId = securityIdentity.getPrincipal().getName();

        TagGetResponse response = new TagGetResponse();

        List<String> systemTags = tagRepository.listSystemTagsForFileType(fileType.name().toLowerCase());

        List<String> userTags = tagRepository.listUserTagsForFileType(fileType.name().toLowerCase(), keycloakId);

        response.setSystemTags(systemTags);
        response.setUserTags(userTags);

        return response;
    }

    @Transactional 
    public TagGetResponse getImageTags() throws Exception {
        return getTags(FileType.IMAGE);
    }

    @Transactional 
    public TagGetResponse getFileTags() throws Exception {
        return getTags(FileType.FILE);
    }

    @Transactional
    public void addTagToFile(UUID tagId, UUID fileId) {
        String keycloakId = securityIdentity.getPrincipal().getName();

        Tag tag = tagRepository.findById(tagId);
        if (tag == null) {
            throw new WebApplicationException("Tag not found", Response.Status.NOT_FOUND);
        }
        if (tag.getOwnerKeycloakId() != null && !tag.getOwnerKeycloakId().equals(keycloakId)) {
            throw new WebApplicationException("Forbidden", Response.Status.FORBIDDEN);
        }

        File file = fileRepository.findById(fileId);
        if (file == null) {
            throw new WebApplicationException("File not found", Response.Status.NOT_FOUND);
        }
        if (!file.getOwnerKeycloakId().equals(keycloakId)) {
            throw new WebApplicationException("Forbidden", Response.Status.FORBIDDEN);
        }

        if (tag.getFileType() != file.getFileType()) {
            throw new WebApplicationException("Tag is not compatible with the file type", Response.Status.BAD_REQUEST);
        }

        file.getTags().add(tag);
    }

    @Transactional
    public void removeTagFromFile(UUID tagId, UUID fileId) {
        String keycloakId = securityIdentity.getPrincipal().getName();

        Tag tag = tagRepository.findById(tagId);
        if (tag == null) {
            throw new WebApplicationException("Tag not found", Response.Status.NOT_FOUND);
        }

        File file = fileRepository.findById(fileId);
        if (file == null) {
            throw new WebApplicationException("File not found", Response.Status.NOT_FOUND);
        }
        if (!file.getOwnerKeycloakId().equals(keycloakId)) {
            throw new WebApplicationException("Forbidden", Response.Status.FORBIDDEN);
        }

        file.getTags().remove(tag);
    }
}
