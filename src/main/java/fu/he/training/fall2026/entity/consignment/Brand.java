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

/** Hãng xe (Hyundai, Toyota…). Mã hiển thị {@code HX-{id}} do BrandServiceImpl.generateCode tạo. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vehicle_brands")
public class Brand extends BaseEntity {

    /** Hệ thống gán sau khi save, dạng HX-1, HX-2… */
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Nationalized
    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CatalogStatus status;

    @Nationalized
    @Column(length = 500)
    private String note;
}
