package fu.he.training.fall2026.entity.sales;

import fu.he.training.fall2026.entity.common.BaseEntity;
import fu.he.training.fall2026.entity.enums.ViewingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "viewings")
public class Viewing extends BaseEntity {

    @Nationalized
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;

    @Column(nullable = false)
    private Instant scheduledAt;

    @Column(nullable = false)
    @ColumnDefault("45")
    private int durationMinutes = 45;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ViewingStatus status;

    /** VIEWING | TEST_DRIVE */
    @Nationalized
    @Column(length = 40)
    private String viewingType;

    @Nationalized
    @Column(length = 160)
    private String location;

    @Nationalized
    @Column(length = 1000)
    private String notes;
}
