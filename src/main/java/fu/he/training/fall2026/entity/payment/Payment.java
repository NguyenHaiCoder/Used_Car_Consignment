package fu.he.training.fall2026.entity.payment;

import fu.he.training.fall2026.entity.common.BaseEntity;
import fu.he.training.fall2026.entity.enums.PaymentStatus;
import fu.he.training.fall2026.entity.sales.Reservation;
import fu.he.training.fall2026.entity.sales.SalesContract;
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
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    /** Vũ Mạnh Cường: cọc thu trước HĐ nên có thể null; gắn reservation. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_contract_id")
    private SalesContract salesContract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentStatus status;

    @Nationalized
    @Column(length = 80)
    private String payerName;

    @Column(length = 80)
    private String referenceCode;

    @Nationalized
    @Column(length = 40)
    private String method;

    private Instant paidAt;

    @Column(length = 500)
    private String evidencePath;

    /** Người ghi nhận, lấy từ phiên đăng nhập. */
    @Nationalized
    @Column(length = 80)
    private String recordedByName;
}
