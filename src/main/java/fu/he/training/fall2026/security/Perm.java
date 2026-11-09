package fu.he.training.fall2026.security;

import fu.he.training.fall2026.entity.enums.ApprovalType;
import fu.he.training.fall2026.entity.enums.RoleCode;

/**
 * Nguyễn Việt Hải: danh mục chức năng để phân quyền, là nguồn gốc duy nhất.
 * Khởi động app thì {@code PermissionCatalogSync} đồng bộ xuống bảng permissions.
 *
 * <p>Mỗi chức năng thuộc đúng một role (không cấp chéo). Admin chỉ bật/tắt chức năng trong phạm vi role đó.
 * Danh mục dùng chung (hãng, dòng xe, loại xe, loại kiểm định) không nằm ở đây vì role nào cũng dùng được.
 */
public enum Perm {

    // ---------------------------------------------------------------- Quản trị viên
    ADMIN_ACCOUNT_VIEW(RoleCode.ADMIN, "Tài khoản", "Xem tài khoản", "Xem danh sách và chi tiết nhân viên.", true),
    ADMIN_ACCOUNT_CREATE(RoleCode.ADMIN, "Tài khoản", "Tạo tài khoản", "Thêm nhân viên mới và gán vai trò."),
    ADMIN_ACCOUNT_EDIT(RoleCode.ADMIN, "Tài khoản", "Sửa tài khoản", "Sửa họ tên, email, SĐT, vai trò."),
    ADMIN_ACCOUNT_LOCK(RoleCode.ADMIN, "Tài khoản", "Khóa / mở tài khoản", "Khóa hoặc mở lại quyền đăng nhập."),
    ADMIN_ACCOUNT_DELETE(RoleCode.ADMIN, "Tài khoản", "Xóa tài khoản", "Xóa vĩnh viễn tài khoản nhân viên."),
    ADMIN_PERMISSION_MANAGE(RoleCode.ADMIN, "Phân quyền", "Phân quyền", "Bật/tắt chức năng của từng vai trò.", true),
    ADMIN_SESSION_VIEW(RoleCode.ADMIN, "Giám sát", "Xem phiên đăng nhập", "Xem các phiên đăng nhập đang hoạt động."),
    ADMIN_AUDIT_VIEW(RoleCode.ADMIN, "Giám sát", "Xem nhật ký hệ thống", "Xem lịch sử thao tác của nhân viên."),
    ADMIN_REPORT_VIEW(RoleCode.ADMIN, "Giám sát", "Xem báo cáo", "Xem báo cáo tổng hợp."),

    // ---------------------------------------------------------------- Quản lý
    MANAGER_APPROVAL_VIEW(RoleCode.MANAGER, "Hàng đợi phê duyệt", "Xem hàng đợi phê duyệt", "Xem danh sách và chi tiết yêu cầu duyệt."),
    MANAGER_APPROVE_CONSIGNMENT(RoleCode.MANAGER, "Hàng đợi phê duyệt", "Duyệt hồ sơ ký gửi", "Duyệt, từ chối, yêu cầu bổ sung hồ sơ ký gửi."),
    MANAGER_APPROVE_AGREEMENT(RoleCode.MANAGER, "Hàng đợi phê duyệt", "Duyệt hợp đồng ký gửi", "Quyết định hợp đồng ký gửi có hiệu lực."),
    MANAGER_APPROVE_WITHDRAWAL(RoleCode.MANAGER, "Hàng đợi phê duyệt", "Duyệt rút ký gửi", "Quyết định yêu cầu rút xe của chủ xe."),
    MANAGER_APPROVE_BELOW_FLOOR(RoleCode.MANAGER, "Hàng đợi phê duyệt", "Duyệt giá dưới sàn", "Cho phép bán thấp hơn giá sàn của chủ xe."),
    MANAGER_APPROVE_OTHER(RoleCode.MANAGER, "Hàng đợi phê duyệt", "Duyệt yêu cầu khác", "Tin đăng và các yêu cầu khác."),
    MANAGER_SETTLEMENT_VIEW(RoleCode.MANAGER, "Quyết toán", "Xem quyết toán", "Xem quyết toán Tài chính gửi duyệt."),
    MANAGER_SETTLEMENT_DECIDE(RoleCode.MANAGER, "Quyết toán", "Duyệt quyết toán", "Duyệt hoặc trả lại quyết toán."),
    MANAGER_CONSIGNMENT_VIEW(RoleCode.MANAGER, "Giám sát", "Theo dõi hồ sơ ký gửi", "Xem toàn bộ hồ sơ ký gửi."),
    MANAGER_AUDIT_VIEW(RoleCode.MANAGER, "Giám sát", "Xem nhật ký hoạt động", "Xem lịch sử thao tác."),
    MANAGER_REPORT_VIEW(RoleCode.MANAGER, "Giám sát", "Xem báo cáo vận hành", "Xem báo cáo vận hành."),

    // ---------------------------------------------------------------- Nhân viên ký gửi
    CONSIGNMENT_VIEW(RoleCode.CONSIGNMENT, "Hồ sơ ký gửi", "Xem hồ sơ ký gửi", "Xem danh sách và chi tiết hồ sơ."),
    CONSIGNMENT_EDIT(RoleCode.CONSIGNMENT, "Hồ sơ ký gửi", "Tạo / sửa hồ sơ ký gửi", "Tạo hồ sơ mới và sửa bản nháp."),
    CONSIGNMENT_SUBMIT(RoleCode.CONSIGNMENT, "Hồ sơ ký gửi", "Gửi duyệt hồ sơ", "Gửi hồ sơ cho Quản lý duyệt."),
    CONSIGNMENT_WITHDRAW(RoleCode.CONSIGNMENT, "Hồ sơ ký gửi", "Gửi yêu cầu rút ký gửi", "Lập yêu cầu rút xe gửi Quản lý."),
    AGREEMENT_VIEW(RoleCode.CONSIGNMENT, "Hợp đồng ký gửi", "Xem hợp đồng ký gửi", "Xem danh sách hợp đồng ký gửi."),
    AGREEMENT_EDIT(RoleCode.CONSIGNMENT, "Hợp đồng ký gửi", "Lập / sửa hợp đồng ký gửi", "Lập hợp đồng và sửa bản nháp."),
    AGREEMENT_SUBMIT(RoleCode.CONSIGNMENT, "Hợp đồng ký gửi", "Gửi duyệt hợp đồng ký gửi", "Gửi hợp đồng cho Quản lý duyệt."),

    // ---------------------------------------------------------------- Kiểm định viên
    INSPECTION_VIEW(RoleCode.INSPECTOR, "Kiểm định", "Xem hàng chờ kiểm định", "Xem xe chờ tiếp nhận và kiểm định."),
    INSPECTION_RECEIVE(RoleCode.INSPECTOR, "Kiểm định", "Tiếp nhận xe", "Nhận xe vào kiểm định."),
    INSPECTION_PERFORM(RoleCode.INSPECTOR, "Kiểm định", "Kiểm định & định giá", "Ghi kết quả kiểm định và giá đề xuất."),
    LISTING_VIEW(RoleCode.INSPECTOR, "Tin đăng", "Xem tin đăng", "Xem danh sách tin bán xe."),
    LISTING_EDIT(RoleCode.INSPECTOR, "Tin đăng", "Đăng / sửa tin", "Đăng tin mới và sửa tin."),
    LISTING_TOGGLE(RoleCode.INSPECTOR, "Tin đăng", "Tạm ẩn / hiện lại tin", "Tạm ẩn hoặc hiển thị lại tin đăng."),

    // ---------------------------------------------------------------- Nhân viên kinh doanh
    INQUIRY_VIEW(RoleCode.SALES, "Tư vấn & lịch xem", "Xem tư vấn", "Xem danh sách và chi tiết tư vấn."),
    INQUIRY_EDIT(RoleCode.SALES, "Tư vấn & lịch xem", "Tạo / sửa / nhận tư vấn", "Tạo tư vấn, sửa, nhận việc, ghi chú."),
    INQUIRY_CLOSE(RoleCode.SALES, "Tư vấn & lịch xem", "Đóng tư vấn", "Kết thúc tư vấn khi khách không mua."),
    VIEWING_MANAGE(RoleCode.SALES, "Tư vấn & lịch xem", "Quản lý lịch xem xe", "Đặt, đổi, xác nhận, hoàn tất, hủy lịch."),
    RESERVATION_VIEW(RoleCode.SALES, "Đặt cọc giữ xe", "Xem phiếu đặt cọc", "Xem danh sách và chi tiết phiếu cọc."),
    RESERVATION_CREATE(RoleCode.SALES, "Đặt cọc giữ xe", "Tạo phiếu đặt cọc", "Lập phiếu cọc giữ xe cho khách."),
    RESERVATION_EDIT(RoleCode.SALES, "Đặt cọc giữ xe", "Sửa phiếu đặt cọc", "Sửa tiền cọc, hạn giữ, điều khoản."),
    RESERVATION_CANCEL(RoleCode.SALES, "Đặt cọc giữ xe", "Hủy cọc (hoàn / giữ cọc)", "Hủy phiếu cọc và chọn hoàn hay giữ cọc."),
    SALES_CONTRACT_VIEW(RoleCode.SALES, "Hợp đồng bán xe", "Xem hợp đồng bán", "Xem danh sách và chi tiết hợp đồng bán."),
    SALES_CONTRACT_CREATE(RoleCode.SALES, "Hợp đồng bán xe", "Lập hợp đồng bán (nháp)", "Lập hợp đồng từ phiếu cọc, lưu nháp."),
    SALES_CONTRACT_SIGN(RoleCode.SALES, "Hợp đồng bán xe", "Ký hợp đồng bán", "Ký hợp đồng với khách mua."),
    SALES_PAYMENT_REQUEST(RoleCode.SALES, "Hợp đồng bán xe", "Gửi yêu cầu thu tiền", "Gửi hợp đồng đã ký sang Tài chính thu tiền."),

    // ---------------------------------------------------------------- Nhân viên tài chính
    PAYMENT_VIEW(RoleCode.FINANCE, "Thanh toán", "Xem giao dịch thanh toán", "Xem khoản cần thu và cọc cần hoàn."),
    PAYMENT_RECORD(RoleCode.FINANCE, "Thanh toán", "Ghi nhận khoản thu", "Ghi nhận tiền khách trả."),
    REFUND_PROCESS(RoleCode.FINANCE, "Thanh toán", "Hoàn cọc", "Ghi nhận hoàn tiền cọc cho khách."),
    SETTLEMENT_VIEW(RoleCode.FINANCE, "Quyết toán", "Xem quyết toán", "Xem quyết toán và bảng tính tiền chủ xe."),
    SETTLEMENT_SUBMIT(RoleCode.FINANCE, "Quyết toán", "Gửi duyệt quyết toán", "Gửi quyết toán cho Quản lý duyệt."),
    SETTLEMENT_PAYOUT(RoleCode.FINANCE, "Quyết toán", "Chi trả chủ xe", "Chi tiền cho chủ xe sau khi được duyệt."),

    // ---------------------------------------------------------------- Nhân viên bàn giao
    HANDOVER_VIEW(RoleCode.HANDOVER, "Bàn giao", "Xem hàng đợi bàn giao", "Xem xe chờ bàn giao, checklist, biên nhận."),
    HANDOVER_CONFIRM(RoleCode.HANDOVER, "Bàn giao", "Xác nhận bàn giao", "Lập biên bản và ký bàn giao xe."),
    HANDOVER_CLOSE_SALE(RoleCode.HANDOVER, "Bàn giao", "Đóng sale", "Đóng giao dịch sau khi bàn giao."),

    // ---------------------------------------------------------------- Nhân viên hồ sơ
    TRANSFER_VIEW(RoleCode.DOCUMENTS, "Sang tên", "Xem hồ sơ sang tên", "Xem danh sách, chi tiết, bằng chứng."),
    TRANSFER_CREATE(RoleCode.DOCUMENTS, "Sang tên", "Tạo hồ sơ sang tên", "Lập hồ sơ sang tên từ hợp đồng đã ký."),
    TRANSFER_START(RoleCode.DOCUMENTS, "Sang tên", "Nhận xử lý hồ sơ", "Nhận hồ sơ để làm thủ tục."),
    TRANSFER_COMPLETE(RoleCode.DOCUMENTS, "Sang tên", "Hoàn tất sang tên", "Tải bằng chứng và hoàn tất sang tên.");

    private final RoleCode role;
    private final String module;
    private final String label;
    private final String description;
    /** Không cho tắt, để Admin không tự khóa mình khỏi màn phân quyền. */
    private final boolean locked;

    Perm(RoleCode role, String module, String label, String description) {
        this(role, module, label, description, false);
    }

    Perm(RoleCode role, String module, String label, String description, boolean locked) {
        this.role = role;
        this.module = module;
        this.label = label;
        this.description = description;
        this.locked = locked;
    }

    public RoleCode role() {
        return role;
    }

    public String module() {
        return module;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public boolean locked() {
        return locked;
    }

    /** Chức năng Quản lý cần có để quyết định một loại yêu cầu trong hàng đợi phê duyệt. */
    public static Perm forApproval(ApprovalType type) {
        if (type == null) {
            return MANAGER_APPROVE_OTHER;
        }
        return switch (type) {
            case CONSIGNMENT -> MANAGER_APPROVE_CONSIGNMENT;
            case AGREEMENT -> MANAGER_APPROVE_AGREEMENT;
            case WITHDRAWAL -> MANAGER_APPROVE_WITHDRAWAL;
            case BELOW_FLOOR_PRICE -> MANAGER_APPROVE_BELOW_FLOOR;
            case SETTLEMENT -> MANAGER_SETTLEMENT_DECIDE;
            default -> MANAGER_APPROVE_OTHER;
        };
    }
}
