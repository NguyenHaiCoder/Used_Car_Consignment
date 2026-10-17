package fu.he.training.fall2026.entity.consignment;

import fu.he.training.fall2026.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vehicles")
public class Vehicle extends BaseEntity {

    @Column(nullable = false, unique = true, length = 32)
    private String vin;

    @Column(length = 40)
    private String chassisNumber;

    @Column(length = 40)
    private String engineNumber;

    @Column(nullable = false, length = 80)
    private String make;

    @Column(nullable = false, length = 120)
    private String model;

    private Integer year;

    private Integer odometerKm;

    @Column(length = 40)
    private String color;

    @Column(length = 160)
    private String location;

    @Column(length = 1000)
    private String intakeCondition;

    @Column(precision = 18, scale = 2)
    private BigDecimal askingPrice;

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
