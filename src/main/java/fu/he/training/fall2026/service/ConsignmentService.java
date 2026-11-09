package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.consignment.ConsignmentResponse;
import fu.he.training.fall2026.dto.consignment.WithdrawResponse;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface ConsignmentService {

    Page<ConsignmentResponse> search(String keyword, ConsignmentStatus status, int page, int size);

    /** Number of consignments per status (every status is present, 0 when empty). */
    Map<ConsignmentStatus, Long> countByStatus();

    /** The latest withdraw request sent to the manager for this consignment, if any. */
    Optional<WithdrawResponse> findWithdrawRequest(Long id);

}
