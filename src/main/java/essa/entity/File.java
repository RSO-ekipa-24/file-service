package essa.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import essa.entity.enums.FileType;
import essa.entity.enums.FileStatus;

@Entity
@Table(name = "files")
@Inheritance(strategy = InheritanceType.JOINED)
public class File {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "bucket_name", nullable = false, length = 255)
    private String bucketName;

    @Column(name = "object_name", nullable = false, length = 1024)
    private String objectName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "file_status_enum")
    private FileStatus status = FileStatus.PENDING;

    @Column(name = "owner_keycloak_id", nullable = false, length = 255)
    private String ownerKeycloakId;

    @Column(name = "created", nullable = false)
    private OffsetDateTime created;

    @Column(name = "modified", nullable = false)
    private OffsetDateTime modified;

    /** Image levels of detail (only for IMAGE files) */
    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<ImageLevelOfDetail> imageLOD = new HashSet<>();

    /** Tags assigned to this file */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "file_tag", joinColumns = @JoinColumn(name = "file_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    public Set<Tag> tags = new HashSet<>();

    /** Access grants for other users */
    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<FileAccess> accessEntries = new HashSet<>();

    /** Links to properties */
    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<PropertyFile> propertyLinks = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.created = OffsetDateTime.now();
        this.modified = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.modified = OffsetDateTime.now();
    }

    public void softDelete() {
        this.status = FileStatus.DELETED;
        this.modified = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        this.status = status;
    }

    public String getOwnerKeycloakId() {
        return ownerKeycloakId;
    }

    public void setOwnerKeycloakId(String ownerKeycloakId) {
        this.ownerKeycloakId = ownerKeycloakId;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public OffsetDateTime getModified() {
        return modified;
    }

    public void setModified(OffsetDateTime modified) {
        this.modified = modified;
    }

    public Set<ImageLevelOfDetail> getImageLOD() {
        return imageLOD;
    }

    public void addImageLOD(ImageLevelOfDetail imageLevelOfDetail) {
        imageLOD.add(imageLevelOfDetail);
        imageLevelOfDetail.setFile(this);
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public Set<FileAccess> getAccessEntries() {
        return accessEntries;
    }

    public void addAccessEntry(FileAccess fileAccess) {
        accessEntries.add(fileAccess);
        fileAccess.setFile(this);
    }

    public Set<PropertyFile> getPropertyLinks() {
        return propertyLinks;
    }

    public void addPropertyLink(PropertyFile propertyFile) {
        propertyLinks.add(propertyFile);
        propertyFile.setFile(this);
    }
}