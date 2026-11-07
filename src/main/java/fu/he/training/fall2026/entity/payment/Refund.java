package fu.he.training.fall2026.entity.payment;

import fu.he.training.fall2026.entity.common.BaseEntity;
import fu.he.training.fall2026.entity.enums.RefundStatus;
import fu.he.training.fall2026.entity.sales.Reservation;
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
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "refunds")
public class Refund extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RefundStatus status;

    @Nationalized
    @Column(length = 2000)
    private String reason;

    @Nationalized
    @Column(length = 40)
    private String method;

    @Column(length = 80)
    private String referenceCode;

    @Nationalized
    @Column(length = 80)
    private String processedByName;

    @Column(length = 500)
    private String evidencePath;

    private Instant processedAt;
}
