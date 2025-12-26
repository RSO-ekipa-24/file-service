package essa.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class PropertyFileId implements Serializable {

    @Column(name = "property_id")
    public Long propertyId;

    @Column(name = "file_id", columnDefinition = "uuid")
    public UUID fileId;
}
