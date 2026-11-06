package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.inspection.InspectVehicleResponse;
import fu.he.training.fall2026.dto.inspection.InspectionQueueResponse;
import fu.he.training.fall2026.dto.inspection.InspectionResultRequest;
import org.springframework.data.domain.Page;

/** Nguyễn Vũ Trường An: Kiểm định xe — hàng đợi, tiếp nhận, kiểm định + định giá. */
public interface InspectorVehicleService {

    /** Consignments that are ACTIVE, INSPECTION_RECEIVED or INSPECTED, newest first. */
    Page<InspectionQueueResponse> searchQueue(String keyword, int page, int size);

    /** ACTIVE → INSPECTION_RECEIVED. @throws fu.he.training.fall2026.exception.BusinessException unless ACTIVE */
    void receive(Long consignmentId);

    /** @throws fu.he.training.fall2026.exception.BusinessException unless INSPECTION_RECEIVED */
    InspectVehicleResponse getForInspection(Long consignmentId);

    /**
     * Empty result form with one row per step of {@code inspectionTypeId} (none when null), prefilled with the
     * latest valuation of the consignment.
     */
    InspectionResultRequest formFor(Long consignmentId, Long inspectionTypeId);

    /**
     * Saves the inspection with one item per step, the proof photos and the valuation, then INSPECTION_RECEIVED →
     * INSPECTED.
     *
     * @return the consignment code
     * @throws fu.he.training.fall2026.exception.BusinessException unless INSPECTION_RECEIVED, when the inspection type
     *     is not active, the steps do not match it, or a step has too many photos
     */
    String submitInspection(Long consignmentId, InspectionResultRequest form);
}
