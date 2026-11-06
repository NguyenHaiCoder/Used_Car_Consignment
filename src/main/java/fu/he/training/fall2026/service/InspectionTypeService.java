package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.inspection.InspectionStepResponse;
import fu.he.training.fall2026.dto.inspection.InspectionTypeRequest;
import fu.he.training.fall2026.dto.inspection.InspectionTypeResponse;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

public interface InspectionTypeService {

    Page<InspectionTypeResponse> search(String keyword, CatalogStatus status, int page, int size);

    /** Number of inspection types per status (every status is present, 0 when empty). */
    Map<CatalogStatus, Long> countByStatus();

    InspectionTypeResponse get(Long id);

    /** Steps of the inspection type, in order. */
    List<InspectionStepResponse> steps(Long id);

    /** Active inspection types, for the "Kiểm định xe" screen. */
    List<InspectionTypeResponse> listActive();

    InspectionTypeRequest formFor(Long id);

    /**
     * Saves the inspection type and its steps, numbered 1..N in form order.
     *
     * @throws fu.he.training.fall2026.exception.BusinessException when the name belongs to another inspection type,
     *     or the status is ACTIVE while the vehicle type is inactive
     */
    Long create(InspectionTypeRequest form);

    /** Same rules as {@link #create}; the previous steps are replaced by the steps of the form. */
    void update(Long id, InspectionTypeRequest form);

    /** @throws fu.he.training.fall2026.exception.BusinessException while its vehicle type is inactive */
    InspectionTypeResponse activate(Long id);

    InspectionTypeResponse deactivate(Long id);
}
