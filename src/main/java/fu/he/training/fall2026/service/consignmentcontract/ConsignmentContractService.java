package fu.he.training.fall2026.service.consignmentcontract;

import fu.he.training.fall2026.dto.consignment.ConsignmentContractRequest;
import fu.he.training.fall2026.dto.consignment.ConsignmentContractResponse;
import java.util.List;
import java.util.Optional;

public interface ConsignmentContractService {

    /** Mọi hợp đồng, mới nhất trước. Tìm kiếm / phân trang do DataTables lo. */
    List<ConsignmentContractResponse> listAll();

    /** Hợp đồng của hồ sơ (1-1), nếu hồ sơ đã soạn hợp đồng. */
    Optional<ConsignmentContractResponse> findByConsignment(Long consignmentId);

    /**
     * Tạo hợp đồng ở trạng thái DRAFT (mã HDKG-{id hồ sơ}).
     *
     * @throws fu.he.training.fall2026.exception.BusinessException khi hồ sơ đã có hợp đồng
     */
    void create(ConsignmentContractRequest form);

    /**
     * Sửa hợp đồng của hồ sơ khi còn DRAFT.
     *
     * @throws fu.he.training.fall2026.exception.BusinessException khi chưa có hợp đồng hoặc đã gửi duyệt
     */
    void updateDraft(ConsignmentContractRequest form);

    /**
     * DRAFT → PENDING_SIGNATURE; từ đây nội dung bị khóa.
     *
     * @throws fu.he.training.fall2026.exception.BusinessException khi chưa có hợp đồng hoặc không còn DRAFT
     */
    void submit(Long consignmentId);
}
