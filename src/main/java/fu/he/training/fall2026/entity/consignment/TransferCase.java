package fu.he.training.fall2026.entity.consignment;

import fu.he.training.fall2026.entity.common.BaseEntity;
import fu.he.training.fall2026.entity.enums.TransferStatus;
import fu.he.training.fall2026.entity.sales.SalesContract;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "transfer_cases")
public class TransferCase extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_contract_id", nullable = false, unique = true)
    private SalesContract salesContract;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TransferStatus status;

    @Column(length = 80)
    private String stage;

    @Column(length = 160)
    private String responsibleParty;

    private LocalDate deadline;

    @Column(length = 1000)
    private String evidencePath;

    @Column(length = 2000)
    private String notes;
}
