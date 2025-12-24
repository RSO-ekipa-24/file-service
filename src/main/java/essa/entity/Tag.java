package essa.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tag", uniqueConstraints = @UniqueConstraint(name = "uq_tag_name_owner", columnNames = {"tag_name", "owner_keycloak_id"}))
public class Tag {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(name = "tag_name", nullable = false, length = 255)
    private String tagName;
    
    @Column(name = "owner_keycloak_id", length = 255)
    private String ownerKeycloakId; // NULL = system tag
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @ManyToMany(mappedBy = "tags")
    private Set<File> files = new HashSet<>();
    
    @ElementCollection
    @CollectionTable(
        name = "tag_category",
        joinColumns = @JoinColumn(name = "tag_id")
    )
    @Column(name = "content_category", nullable = false, length = 255)
    private Set<String> contentCategories = new HashSet<>();
    
    public Tag() {}
    
    public Tag(String tagName, String ownerKeycloakId) {
        this.tagName = tagName;
        this.ownerKeycloakId = ownerKeycloakId;
        this.createdAt = OffsetDateTime.now();
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
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Set<File> getFiles() {
        return files;
    }
    
    public void setFiles(Set<File> files) {
        this.files = files;
    }
    
    public Set<String> getContentCategories() {
        return contentCategories;
    }
    
    public void setContentCategories(Set<String> contentCategories) {
        this.contentCategories = contentCategories;
    }
}