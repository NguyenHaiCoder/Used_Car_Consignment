package fu.he.training.fall2026.entity.consignment;

import fu.he.training.fall2026.entity.common.BaseEntity;
import fu.he.training.fall2026.entity.enums.AgreementStatus;
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
@Table(name = "consignment_agreements")
public class ConsignmentAgreement extends BaseEntity {

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false)
    private int versionNo;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "consignment_id", nullable = false)
    private Consignment consignment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AgreementStatus status;

    @Column(precision = 8, scale = 4)
    private BigDecimal commissionRate;

    @Column(precision = 18, scale = 2)
    private BigDecimal fixedFee;

    @Column(precision = 18, scale = 2)
    private BigDecimal priceFloor;

    private LocalDate validFrom;

    private LocalDate validTo;

    @Column(length = 4000)
    private String custodyTerms;

    @Column(length = 4000)
    private String terminationTerms;

    @Column(length = 4000)
    private String payoutTerms;

    @Column(length = 500)
    private String signatureEvidencePath;
}
