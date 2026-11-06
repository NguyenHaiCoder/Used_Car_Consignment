package fu.he.training.fall2026.entity.consignment;

import fu.he.training.fall2026.entity.common.BaseEntity;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/** Nguyễn Vũ Trường An: Quản lý loại xe (SUV, SEDAN...). {@code code} khớp với {@link Vehicle#getVehicleType()}. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vehicle_types")
public class VehicleType extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Nationalized
    @Column(nullable = false, length = 120)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CatalogStatus status;
}
