package fu.he.training.fall2026.entity.consignment;

import fu.he.training.fall2026.entity.common.BaseEntity;
import fu.he.training.fall2026.entity.enums.AgreementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "consignment_agreements")
public class ConsignmentAgreement extends BaseEntity {

    @Column(nullable = false, length = 40)
    @Nationalized
    private String code;

    @OneToOne(optional = false)
    @JoinColumn(name = "consignment_id", nullable = false, unique = true)
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
    @Nationalized
    private String custodyTerms;

    @Column(length = 4000)
    @Nationalized
    private String terminationTerms;

    @Column(length = 4000)
    @Nationalized
    private String payoutTerms;

    @Column(length = 500)
    @Nationalized
    private String signatureEvidencePath;
}
