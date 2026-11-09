package fu.he.training.fall2026.entity.sales;

import fu.he.training.fall2026.entity.common.BaseEntity;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
@Table(name = "sales_contracts")
public class SalesContract extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ContractStatus status;

    @Column(precision = 18, scale = 2)
    private BigDecimal salePrice;

    @Column(precision = 18, scale = 2)
    private BigDecimal depositAmount;

    @Column(precision = 18, scale = 2)
    private BigDecimal remainingAmount;

    private LocalDate signedOn;

    @Nationalized
    @Column(columnDefinition = "nvarchar(max)")
    private String sellerSignaturePath;

    @Nationalized
    @Column(columnDefinition = "nvarchar(max)")
    private String buyerSignaturePath;

    @Nationalized
    @Column(columnDefinition = "nvarchar(max)")
    private String paymentTerms;

    @Nationalized
    @Column(columnDefinition = "nvarchar(max)")
    private String handoverTerms;

    @Nationalized
    @Column(columnDefinition = "nvarchar(max)")
    private String terminationTerms;

    @Column(nullable = false)
    private boolean saleClosed;
}
