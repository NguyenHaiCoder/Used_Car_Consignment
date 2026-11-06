package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.inspection.VehicleTypeRequest;
import fu.he.training.fall2026.dto.inspection.VehicleTypeResponse;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface VehicleTypeService {

    Page<VehicleTypeResponse> search(String keyword, CatalogStatus status, int page, int size);

    /** Number of vehicle types per status (every status is present, 0 when empty). */
    Map<CatalogStatus, Long> countByStatus();

    /** Active vehicle types by label, for pick-lists. */
    List<VehicleTypeResponse> listActive();

    /** Active vehicle types plus {@code keepId} even when it is inactive, so an edit form keeps its current value. */
    List<VehicleTypeResponse> listSelectable(Long keepId);

    Optional<VehicleTypeResponse> findByCode(String code);

    VehicleTypeRequest formFor(Long id);

    /** @throws fu.he.training.fall2026.exception.BusinessException when the code belongs to another vehicle type */
    Long create(VehicleTypeRequest form);

    /** @throws fu.he.training.fall2026.exception.BusinessException when the code belongs to another vehicle type */
    void update(Long id, VehicleTypeRequest form);

    VehicleTypeResponse activate(Long id);

    /** @throws fu.he.training.fall2026.exception.BusinessException while an active inspection type still uses it */
    VehicleTypeResponse deactivate(Long id);
}
