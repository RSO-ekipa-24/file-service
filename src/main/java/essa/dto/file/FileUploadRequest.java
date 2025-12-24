package essa.dto.file;

import jakarta.validation.constraints.*;

public class FileUploadRequest {
    
    @NotBlank(message = "Filename is required")
    @Size(max = 255, message = "Filename must be at most 255 characters")
    private String fileName;

    @NotBlank(message = "Content type is required")
    @Size(max = 100, message = "Content type must be at most 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9]+/[a-zA-Z0-9\\-+.]+$", message = "Content type must be a valid MIME type")
    private String contentType;
    
    @NotNull(message = "Size is required")
    @Min(value = 1, message = "Size must be non-negative")
    @Max(value = 1073741824, message = "Size must be less than or equal to 1GB")
    private Long size; // in bytes

    private String[] tagNames;
    
    public FileUploadRequest() {}
    
    public FileUploadRequest(String fileName, String contentType, Long size, String[] tagNames) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
        this.tagNames = tagNames;
    }
    
    // Getters and setters
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
    
    public String[] getTagNames() {
        return tagNames;
    }
    
    public void setTagNames(String[] tagNames) {
        this.tagNames = tagNames;
    }
}