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
}
