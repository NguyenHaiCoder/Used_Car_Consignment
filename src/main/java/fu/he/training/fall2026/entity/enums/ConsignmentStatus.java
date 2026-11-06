package fu.he.training.fall2026.entity.enums;

/**
 * Trạng thái hồ sơ ký gửi — thứ tự đúng luồng nghiệp vụ.
 *
 * <ul>
 *   <li>{@link #DRAFT} — Lưu nháp</li>
 *   <li>{@link #SUBMITTED} — Chờ phê duyệt</li>
 *   <li>{@link #ACTIVE} — Đã duyệt</li>
 *   <li>{@link #INSPECTION_RECEIVED} — Đang kiểm định</li>
 *   <li>{@link #INSPECTED} — Đã kiểm định</li>
 *   <li>{@link #LISTED} — Đang đăng bán</li>
 *   <li>{@link #RESERVED} — Đã đặt cọc</li>
 *   <li>{@link #SOLD} — Đã bán</li>
 *   <li>{@link #COMPLETED} — Hoàn thành</li>
 *   <li>{@link #WITHDRAW_REQUESTED} — Đang yêu cầu rút ký gửi</li>
 *   <li>{@link #WITHDRAWN} — Đã rút ký gửi</li>
 *   <li>{@link #CLOSED} — Đã đóng ký gửi</li>
 * </ul>
 */
public enum ConsignmentStatus {
    DRAFT,
    SUBMITTED,
    ACTIVE,
    INSPECTION_RECEIVED,
    INSPECTED,
    LISTED,
    RESERVED,
    SOLD,
    COMPLETED,
    WITHDRAW_REQUESTED,
    WITHDRAWN,
    CLOSED
}
