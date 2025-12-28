package essa.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import essa.entity.enums.FileType;

public class TagCreateRequest {

    @NotBlank(message = "Tag name is required")
    private String tagName;

    @NotNull(message = "File type is required")
    private FileType fileType;
    
    public TagCreateRequest() {}

    public TagCreateRequest(String tagName, FileType fileType) {
        this.tagName = tagName;
        this.fileType = fileType;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }
}
