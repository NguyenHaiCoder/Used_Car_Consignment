package fu.he.training.fall2026.entity.sales;

import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.common.BaseEntity;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.enums.ReservationStatus;
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
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reservations")
public class Reservation extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private fu.he.training.fall2026.entity.enums.DepositDisposition cancelDepositPolicy;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private fu.he.training.fall2026.entity.enums.DepositDisposition expiryDepositPolicy;

    @Column(precision = 18, scale = 2)
    private BigDecimal confirmedCollectedAmount;

    @Column(precision = 18, scale = 2)
    private BigDecimal processedLiabilityAmount;

    private Long depositDecisionActorId;

    @Column(length = 100)
    private String depositOperationKey;

    @Column(length = 200)
    private String depositBalanceReference;

    @Column(length = 200)
    private String depositProcessingReference;

    @Column(length = 200)
    private String vehicleTransitionReference;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private fu.he.training.fall2026.entity.enums.DepositDisposition depositDisposition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_reservation_id")
    private Reservation transferReservation;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = true)
    private Inquiry inquiry;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReservationStatus status;

    @Column(precision = 18, scale = 2)
    private BigDecimal depositAmount;

    private LocalDate holdUntil;

    @Column(length = 2000)
    private String terms;

    @Column(nullable = false)
    private boolean consentStored;

    private Instant cancelledAt;

    @Column(length = 2000)
    private String cancelReason;
}
