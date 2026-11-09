package fu.he.training.fall2026.entity.enums;

public enum ApprovalType {
    /** Nguyễn Việt Hải: duyệt hồ sơ ký gửi (SUBMITTED → ACTIVE). */
    CONSIGNMENT,
    /** Nguyễn Việt Hải: duyệt hợp đồng ký gửi (PENDING_SIGNATURE → ACTIVE). */
    AGREEMENT,
    LISTING_ELIGIBILITY,
    BELOW_FLOOR_PRICE,
    WITHDRAWAL,
    SETTLEMENT,
    OTHER
}
