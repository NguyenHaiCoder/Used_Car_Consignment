package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.consignment.ConsignmentForm;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

public interface ConsignmentService {

    Page<Consignment> search(String keyword, ConsignmentStatus status, int page, int size);

    /** Number of consignments per status (every status is present, 0 when empty). */
    Map<ConsignmentStatus, Long> countByStatus();

    Consignment get(Long id);

    /** Vehicles that can be picked for a consignment (registered at vehicle intake). */
    List<Vehicle> listVehicles();

    /** Form for editing; only a DRAFT may be edited. */
    ConsignmentForm formFor(Long id);

    /** Creates a DRAFT for an existing consignor and vehicle. */
    Long createDraft(ConsignmentForm form);

    /** @throws fu.he.training.fall2026.exception.BusinessException unless the consignment is still a DRAFT */
    void updateDraft(Long id, ConsignmentForm form);

    /** DRAFT → SUBMITTED; after this the content is locked. */
    void submit(Long id);
}
