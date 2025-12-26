package essa.entity;

import essa.entity.enums.AccessLevel;
import essa.entity.id.FileAccessId;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;


@Entity
@Table(name = "file_access")
public class FileAccess {

    @EmbeddedId
    private FileAccessId id;

    @ManyToOne
    @MapsId("fileId")
    @JoinColumn(name = "file_id")
    private File file;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "access_level", nullable = false)
    private AccessLevel accessLevel;

    @CreationTimestamp
    private OffsetDateTime created;

    public FileAccessId getId() {
        return id;
    }

    public void setId(FileAccessId id) {
        this.id = id;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }
}
