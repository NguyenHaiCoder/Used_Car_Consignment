package fu.he.training.fall2026.entity.consignment;

import fu.he.training.fall2026.entity.common.BaseEntity;
import fu.he.training.fall2026.entity.enums.InspectionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inspections")
public class Inspection extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    /**
     * Nguyễn Vũ Trường An: hồ sơ ký gửi được kiểm định (một xe có thể ký gửi nhiều lần).
     * Cột cho phép null vì bảng inspections đã có dòng cũ trước khi thêm cột; phiếu mới luôn có giá trị.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consignment_id")
    private Consignment consignment;

    /** Nguyễn Vũ Trường An: loại kiểm định đã chọn trên màn Tiến hành kiểm định (null với dòng cũ như trên). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_type_id")
    private InspectionType inspectionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private InspectionStatus status;

    @Column(length = 40)
    private String overallResult;

    private LocalDate inspectedOn;

    @Column(length = 2000)
    private String notes;
}
