package fu.he.training.fall2026.entity.consignment;

import fu.he.training.fall2026.entity.common.BaseEntity;
import fu.he.training.fall2026.entity.enums.ChecklistKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "document_checklists")
public class DocumentChecklist extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ChecklistKind kind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consignment_id")
    private Consignment consignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_case_id")
    private TransferCase transferCase;

    @Column(length = 40)
    private String status;

    @Column(length = 1000)
    private String notes;
}
