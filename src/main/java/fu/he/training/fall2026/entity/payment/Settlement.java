package fu.he.training.fall2026.entity.payment;

import fu.he.training.fall2026.entity.common.BaseEntity;
import fu.he.training.fall2026.entity.consignment.ConsignmentAgreement;
import fu.he.training.fall2026.entity.enums.SettlementStatus;
import fu.he.training.fall2026.entity.sales.SalesContract;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "settlements")
public class Settlement extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_contract_id", nullable = false, unique = true)
    private SalesContract salesContract;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "consignment_agreement_id", nullable = false)
    private ConsignmentAgreement consignmentAgreement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SettlementStatus status;

    @Column(precision = 18, scale = 2)
    private BigDecimal baseAmount;

    @Column(precision = 18, scale = 2)
    private BigDecimal commissionAmount;

    @Column(precision = 18, scale = 2)
    private BigDecimal feeAmount;

    @Column(precision = 18, scale = 2)
    private BigDecimal approvedCostAmount;

    @Column(precision = 18, scale = 2)
    private BigDecimal refundHeldAmount;

    @Column(precision = 18, scale = 2)
    private BigDecimal depositHeldAmount;

    @Column(precision = 18, scale = 2)
    private BigDecimal netPayoutAmount;

    @Column(length = 2000)
    private String notes;
}
