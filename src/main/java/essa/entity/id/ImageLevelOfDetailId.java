package essa.entity.id;

import essa.entity.enums.LevelOfDetail;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class ImageLevelOfDetailId implements Serializable {

    @Column(name = "file_id")
    private UUID fileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "level_of_detail")
    private LevelOfDetail levelOfDetail;

    public ImageLevelOfDetailId() {}

    public ImageLevelOfDetailId(UUID fileId, LevelOfDetail levelOfDetail) {
        this.fileId = fileId;
        this.levelOfDetail = levelOfDetail;
    }

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    public LevelOfDetail getLevelOfDetail() {
        return levelOfDetail;
    }

    public void setLevelOfDetail(LevelOfDetail levelOfDetail) {
        this.levelOfDetail = levelOfDetail;
    }

    @Override
    public int hashCode() {
        int result = ((fileId == null) ? 0 : fileId.hashCode());
        result = 113 * result + ((levelOfDetail == null) ? 0 : levelOfDetail.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ImageLevelOfDetailId other = (ImageLevelOfDetailId) obj;
        if (fileId == null) {
            if (other.fileId != null)
                return false;
        } else if (!fileId.equals(other.fileId))
            return false;
        if (levelOfDetail != other.levelOfDetail)
            return false;
        return true;
    }
}