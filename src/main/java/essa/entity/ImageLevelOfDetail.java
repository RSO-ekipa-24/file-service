package essa.entity;

import essa.entity.id.ImageLevelOfDetailId;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;


@Entity
@Table(name = "image_level_of_detail")
public class ImageLevelOfDetail {

    @EmbeddedId
    private ImageLevelOfDetailId id;

    @ManyToOne
    @MapsId("fileId")
    @JoinColumn(name = "file_id")
    private File file;

    @Column(name = "object_name", nullable = false, length = 1024)
    private String objectName;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @CreationTimestamp
    private OffsetDateTime created;

    // Getters and Setters
    public ImageLevelOfDetailId getId() {
        return id;
    }
    public void setId(ImageLevelOfDetailId id) {
        this.id = id;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }
}
