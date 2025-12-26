package essa.entity;

import essa.entity.id.PropertyFileId;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;


@Entity
@Table(name = "property_file")
public class PropertyFile {

    @EmbeddedId
    private PropertyFileId id;

    @ManyToOne
    @MapsId("fileId")
    @JoinColumn(name = "file_id")
    private File file;

    @CreationTimestamp
    private OffsetDateTime created;

    public PropertyFileId getId() {
        return id;
    }

    public void setId(PropertyFileId id) {
        this.id = id;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }
}
