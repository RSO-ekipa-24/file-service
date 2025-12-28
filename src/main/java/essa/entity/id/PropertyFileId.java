package essa.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class PropertyFileId implements Serializable {

    @Column(name = "property_id")
    private Long propertyId;

    @Column(name = "file_id", columnDefinition = "uuid")
    private UUID fileId;

    public PropertyFileId() {}

    public PropertyFileId(Long propertyId, UUID fileId) {
        this.propertyId = propertyId;
        this.fileId = fileId;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyFileId that = (PropertyFileId) o;
        return propertyId.equals(that.propertyId) && fileId.equals(that.fileId);
    }

    @Override
    public int hashCode() {
        int result = propertyId.hashCode();
        result = 113 * result + fileId.hashCode();
        return result;
    }
}
