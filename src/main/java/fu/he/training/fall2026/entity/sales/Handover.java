package fu.he.training.fall2026.entity.sales;

import fu.he.training.fall2026.entity.common.BaseEntity;
import fu.he.training.fall2026.entity.enums.HandoverStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "handovers")
public class Handover extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_contract_id", nullable = false, unique = true)
    private SalesContract salesContract;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private HandoverStatus status;

    private Integer odometerKm;

    private Integer keyCount;

    @Column(length = 2000)
    private String accessories;

    @Column(length = 1000)
    private String photoPaths;

    @Column(length = 500)
    private String receiverSignaturePath;

    @Column(length = 500)
    private String staffSignaturePath;

    private Instant handedOverAt;

    @Column(nullable = false)
    private boolean receiptIssued;

    @Column(nullable = false)
    private boolean saleCloseChecked;
}
