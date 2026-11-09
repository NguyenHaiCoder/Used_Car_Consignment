package fu.he.training.fall2026.service;

import fu.he.training.fall2026.entity.enums.ApprovalType;
import java.util.List;
import java.util.Map;

/**
 * Nguyễn Việt Hải: hàng đợi phê duyệt của Quản lý.
 * Gom việc chờ duyệt của các luồng khác, quyết định xong thì đổi luôn trạng thái của hồ sơ, hợp đồng, tin đăng.
 */
public interface ManagerApprovalService {

    String PENDING = "PENDING";

    /** Nhãn, màu, icon theo trạng thái yêu cầu. */
    Map<String, Map<String, String>> statusMeta();

    /** Nhãn, icon theo loại yêu cầu. */
    Map<String, Map<String, String>> typeMeta();

    /**
     * Toàn bộ yêu cầu, việc còn chờ đứng trước. Hồ sơ và hợp đồng đang gửi duyệt mà chưa có dòng chờ thì tạo dòng.
     * Quyết toán lấy từ màn Duyệt quyết toán của Tài chính.
     */
    List<Map<String, Object>> queue(Long viewerUserId);

    /** Chi tiết một yêu cầu: thông tin đối tượng, điều kiện duyệt và tác động của từng quyết định. */
    Map<String, Object> detail(Long id);

    /** Loại của yêu cầu, để kiểm tra Quản lý có được bật quyền duyệt loại đó không. */
    ApprovalType typeOf(Long id);

    /**
     * Duyệt, từ chối hoặc yêu cầu bổ sung. Còn điều kiện chưa đạt thì không duyệt được,
     * từ chối và yêu cầu bổ sung phải có lý do.
     *
     * @return mã yêu cầu vừa quyết định
     */
    String decide(Long id, String decision, String note, Long actorUserId);
}
