package fu.he.training.fall2026.entity.consignment;

import fu.he.training.fall2026.entity.common.BaseEntity;
import fu.he.training.fall2026.entity.enums.ListingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vehicle_sale_news")
public class VehicleSaleNews extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "consignment_id", nullable = false)
    private Consignment consignment;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 8000)
    private String body;

    @Column(precision = 18, scale = 2)
    private BigDecimal listPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ListingStatus status;

    @Column(length = 1000)
    private String imagePaths;

    private Instant publishedAt;

    private Instant stoppedAt;
}
