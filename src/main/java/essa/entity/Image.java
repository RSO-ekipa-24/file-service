package essa.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "image")
@DiscriminatorValue("IMAGE")
@PrimaryKeyJoinColumn(name = "file_id")
public class Image extends File {
    
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "level_of_detail", columnDefinition = "level_of_detail_enum", nullable = false)
    private LevelOfDetail levelOfDetail;
    
    public Image() {
        super();
    }
    
    public LevelOfDetail getLevelOfDetail() {
        return levelOfDetail;
    }
    
    public void setLevelOfDetail(LevelOfDetail levelOfDetail) {
        this.levelOfDetail = levelOfDetail;
    }
}