package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.consignment.ConsignmentRequest;
import fu.he.training.fall2026.dto.consignment.ConsignmentResponse;
import fu.he.training.fall2026.dto.consignment.WithdrawCheckResponse;
import fu.he.training.fall2026.dto.consignment.VehicleResponse;
import fu.he.training.fall2026.dto.consignment.WithdrawRequest;
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

    ConsignmentResponse get(Long id);

    /** Vehicles that can be picked for a consignment (registered at vehicle intake). */
    List<VehicleResponse> listVehicles();

    /** Form for editing; only a DRAFT may be edited. */
    ConsignmentRequest formFor(Long id);

    /** Creates a DRAFT for an existing consignor and vehicle. */
    Long createDraft(ConsignmentRequest form);

    /** @throws fu.he.training.fall2026.exception.BusinessException unless the consignment is still a DRAFT */
    void updateDraft(Long id, ConsignmentRequest form);

    /** DRAFT → SUBMITTED; after this the content is locked. */
    void submit(Long id);

    /** The conditions to check before withdrawing; the consignment can be withdrawn when every one passes. */
    List<WithdrawCheckResponse> withdrawChecks(Long id);

    /** The latest withdraw request sent to the manager for this consignment, if any. */
    Optional<WithdrawResponse> findWithdrawRequest(Long id);

    /**
     * Sends a withdraw request to the manager: saves the reason, return date, condition and evidence on the
     * consignment and creates a PENDING WITHDRAWAL approval. The consignment status is not changed here.
     *
     * @throws fu.he.training.fall2026.exception.BusinessException while any withdraw check fails
     */
    void requestWithdraw(Long id, WithdrawRequest form, Long actorUserId);
}
