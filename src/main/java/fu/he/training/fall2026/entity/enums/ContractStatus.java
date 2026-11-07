package fu.he.training.fall2026.entity.enums;

/** Trạng thái hợp đồng bán xe. {@link #COMPLETED} = đã giao dịch xong, hợp đồng vẫn còn giá trị. */
public enum ContractStatus {
    DRAFT,
    SIGNED,
    ACTIVE,
    COMPLETED,
    CANCELLED
}
