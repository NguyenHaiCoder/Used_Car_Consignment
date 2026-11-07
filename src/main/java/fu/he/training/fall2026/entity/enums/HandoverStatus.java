package fu.he.training.fall2026.entity.enums;

/** Trạng thái bàn giao. {@link #FINALIZED} = đã hoàn tất giao dịch (sau bàn giao). */
public enum HandoverStatus {
    PENDING,
    READY,
    BLOCKED,
    COMPLETED,
    FINALIZED
}
