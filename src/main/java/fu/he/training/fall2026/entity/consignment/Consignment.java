package fu.he.training.fall2026.entity.consignment;

import fu.he.training.fall2026.entity.common.BaseEntity;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "consignments")
public class Consignment extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "consignor_id", nullable = false)
    private Consignor consignor;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ConsignmentStatus status;

    @Column(precision = 18, scale = 2)
    private BigDecimal expectedPrice;

    private LocalDate intakeDate;

    @Column(length = 2000)
    private String termNotes;

    @Column(length = 160)
    private String custodyLocation;

    @Column(nullable = false)
    private boolean ownerConsent;

    @Column(length = 2000)
    private String withdrawReason;

    private LocalDate withdrawReturnDate;

    @Column(length = 2000)
    private String withdrawCondition;

    @Column(length = 500)
    private String withdrawEvidencePath;
}
