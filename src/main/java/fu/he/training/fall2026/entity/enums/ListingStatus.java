package fu.he.training.fall2026.entity.enums;

public enum ListingStatus {
    DRAFT,
    READY,
    LISTED,
    PAUSED,
    /** Đã có phiếu đặt cọc: vẫn hiện trên trang bán xe, không nhận liên hệ. */
    RESERVED,
    SOLD,
    STOPPED
}
