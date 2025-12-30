package essa.entity;

import essa.entity.enums.FileType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "tag", 
    uniqueConstraints = {@UniqueConstraint(columnNames = {"tag_name", "owner_keycloak_id", "file_type"})}
)
public class Tag {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tag_name", nullable = false)
    private String tagName;

    @Column(name = "owner_keycloak_id")
    private String ownerKeycloakId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;

    @CreationTimestamp
    private OffsetDateTime created;

    @PrePersist
    protected void onCreate() {
        this.created = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getOwnerKeycloakId() {
        return ownerKeycloakId;
    }

    public void setOwnerKeycloakId(String ownerKeycloakId) {
        this.ownerKeycloakId = ownerKeycloakId;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }
}