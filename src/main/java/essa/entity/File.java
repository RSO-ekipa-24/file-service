package essa.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @Column(name = "status", nullable = false, columnDefinition = "file_status_enum")
    private Status status = Status.PENDING;

    @Column(name = "owner_keycloak_id", nullable = false, length = 255)
    private String ownerKeycloakId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "file_tag",
        joinColumns = @JoinColumn(name = "file_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @ElementCollection
    @CollectionTable(
        name = "file_access",
        joinColumns = @JoinColumn(name = "file_id")
    )
    @Column(name = "keycloak_id")
    private Set<String> accessKeycloakIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(
        name = "property_file",
        joinColumns = @JoinColumn(name = "file_id")
    )
    @Column(name = "property_id")
    private Set<Long> propertyIds = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters and Setters
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getOwnerKeycloakId() {
        return ownerKeycloakId;
    }

    public void setOwnerKeycloakId(String ownerKeycloakId) {
        this.ownerKeycloakId = ownerKeycloakId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public void addTag(Tag tag) {
        this.tags.add(tag);
        tag.getFiles().add(this);
    }

    public void removeTag(Tag tag) {
        this.tags.remove(tag);
        tag.getFiles().remove(this);
    }

    public Set<String> getAccessKeycloakIds() {
        return accessKeycloakIds;
    }

    public void setAccessKeycloakIds(Set<String> accessKeycloakIds) {
        this.accessKeycloakIds = accessKeycloakIds;
    }

    public Set<Long> getPropertyIds() {
        return propertyIds;
    }

    public void setPropertyIds(Set<Long> propertyIds) {
        this.propertyIds = propertyIds;
    }
}