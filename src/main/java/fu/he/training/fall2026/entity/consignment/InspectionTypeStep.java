package fu.he.training.fall2026.entity.consignment;

import fu.he.training.fall2026.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/** Nguyễn Vũ Trường An: một bước trong loại kiểm định ("Kiểm định gì" + "Mô tả / gợi ý"). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inspection_type_steps")
public class InspectionTypeStep extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_type_id", nullable = false)
    private InspectionType inspectionType;

    @Column(nullable = false)
    private int stepOrder;

    @Nationalized
    @Column(nullable = false, length = 160)
    private String title;

    @Nationalized
    @Column(length = 500)
    private String hint;
}
