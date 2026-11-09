package fu.he.training.fall2026.service.consignmentrequest;

import fu.he.training.fall2026.dto.consignment.ConsignmentCreateRequest;
import fu.he.training.fall2026.dto.consignment.ConsignmentRequestResponse;
import fu.he.training.fall2026.dto.consignment.WithdrawCheckResponse;
import fu.he.training.fall2026.dto.consignment.WithdrawRequest;
import java.util.List;

public interface ConsignmentRequestService {

    /** Mọi hồ sơ, mới nhất trước. Tìm kiếm / phân trang do DataTables lo. */
    List<ConsignmentRequestResponse> listAll();

    /** Một hồ sơ kèm chủ xe và xe. */
    ConsignmentRequestResponse get(Long id);

    /**
     * Tạo mới cùng lúc chủ xe, xe và hồ sơ ký gửi (DRAFT, mã KG-{id}).
     *
     * @return id hồ sơ vừa tạo
     * @throws fu.he.training.fall2026.exception.BusinessException khi dòng xe không tồn tại
     */
    Long create(ConsignmentCreateRequest form);

    /**
     * Sửa hồ sơ DRAFT: cập nhật luôn chủ xe và xe gắn với hồ sơ (cùng form với màn tạo).
     *
     * @throws fu.he.training.fall2026.exception.BusinessException khi hồ sơ không còn DRAFT
     *     hoặc dòng xe không tồn tại
     */
    void updateDraft(Long id, ConsignmentCreateRequest form);

    /**
     * DRAFT → SUBMITTED; sau đó nội dung hồ sơ bị khóa.
     *
     * @throws fu.he.training.fall2026.exception.BusinessException khi hồ sơ không còn DRAFT
     */
    void submit(Long id);

    /** 3 điều kiện rút: hồ sơ chưa bán/đóng, xe chưa bị khách giữ, chưa có yêu cầu đang chờ duyệt. */
    List<WithdrawCheckResponse> withdrawChecks(Long id);

    /** Lý do xe đang bị khách giữ (phiếu cọc còn hiệu lực, hợp đồng bán chưa hủy); rỗng nghĩa là rút được. */
    String buyerHoldDetail(Long id);

    /**
     * Gửi yêu cầu rút hồ sơ cho quản lý: lưu lý do, ngày trả xe, tình trạng, minh chứng vào hồ sơ, tạo yêu cầu
     * WITHDRAWAL chờ duyệt và chuyển hồ sơ sang WITHDRAW_REQUESTED.
     *
     * @throws fu.he.training.fall2026.exception.BusinessException khi còn điều kiện rút chưa đạt
     */
    void requestWithdraw(Long id, WithdrawRequest form, Long actorUserId);
}
