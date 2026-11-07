package fu.he.training.fall2026.entity.consignment;

import fu.he.training.fall2026.entity.common.BaseEntity;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/**
 * Dòng xe (Tucson, Camry…) thuộc một {@link Brand}.
 * Mã hiển thị {@code DX-{id}} do VehicleModelServiceImpl.generateCode tạo.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "vehicle_models",
        uniqueConstraints = @UniqueConstraint(name = "uk_vehicle_models_brand_name", columnNames = {"brand_id", "name"}))
public class VehicleModel extends BaseEntity {

    /** Hệ thống gán sau khi save, dạng DX-1, DX-2… */
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Nationalized
    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CatalogStatus status;

    @Nationalized
    @Column(length = 500)
    private String note;
}
