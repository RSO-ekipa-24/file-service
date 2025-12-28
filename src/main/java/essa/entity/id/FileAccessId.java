package essa.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class FileAccessId implements Serializable {

    @Column(name = "file_id", columnDefinition = "uuid")
    public UUID fileId;

    @Column(name = "keycloak_id", length = 255)
    public String keycloakId;

    public FileAccessId() {}

    public FileAccessId(UUID fileId, String keycloakId) {
        this.fileId = fileId;
        this.keycloakId = keycloakId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileAccessId that = (FileAccessId) o;
        return fileId.equals(that.fileId) && keycloakId.equals(that.keycloakId);
    }

    @Override
    public int hashCode() {
        int result = fileId.hashCode();
        result = 113 * result + keycloakId.hashCode();
        return result;
    }
}
