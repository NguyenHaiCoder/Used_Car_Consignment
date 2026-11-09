package fu.he.training.fall2026.entity.consignment;

import fu.he.training.fall2026.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vehicles")
public class Vehicle extends BaseEntity {

    @Column(nullable = false, unique = true, length = 32)
    @Nationalized
    private String vin;

    @Column(length = 40)
    @Nationalized
    private String chassisNumber;

    @Column(length = 40)
    @Nationalized
    private String engineNumber;

    @Column(nullable = false, length = 80)
    @Nationalized
    private String make;

    @Column(nullable = false, length = 120)
    @Nationalized
    private String model;

    private Integer year;

    private Integer odometerKm;

    @Column(length = 40)
    @Nationalized
    private String color;

    @Column(length = 160)
    @Nationalized
    private String location;

    @Column(length = 1000)
    @Nationalized
    private String intakeCondition;

    @Column(precision = 18, scale = 2)
    private BigDecimal askingPrice;

    /**
     * Nguyễn Vũ Trường An: biển số, loại xe, hộp số, nhiên liệu hiện trên tin đăng.
     * Biển số không đặt unique: SQL Server chỉ cho một dòng NULL, mà xe cũ chưa có biển số.
     */
    @Column(length = 20)
    @Nationalized
    private String licensePlate;

    @Column(length = 40)
    @Nationalized
    private String vehicleType;

    @Column(length = 40)
    @Nationalized
    private String transmission;

    @Column(length = 40)
    @Nationalized
    private String fuelType;

    public String getDisplayLabel() {
        StringBuilder sb = new StringBuilder();
        if (make != null) {
            sb.append(make);
        }
        if (model != null) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(model);
        }
        return sb.toString();
    }
}
