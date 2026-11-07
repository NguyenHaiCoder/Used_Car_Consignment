package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.catalog.VehicleModelRequest;
import fu.he.training.fall2026.dto.catalog.VehicleModelResponse;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

public interface VehicleModelService {

    Page<VehicleModelResponse> search(String keyword, CatalogStatus status, Long brandId, int page, int size);

    Map<CatalogStatus, Long> countByStatus();

    VehicleModelResponse get(Long id);

    /** Dòng xe ACTIVE (mọi hãng) — pick-list form ký gửi. */
    List<VehicleModelResponse> listActive();

    List<VehicleModelResponse> listActiveByBrand(Long brandId);

    VehicleModelRequest formFor(Long id);

    /** @throws fu.he.training.fall2026.exception.BusinessException khi trùng tên dòng trong cùng hãng */
    Long create(VehicleModelRequest form);

    /** @throws fu.he.training.fall2026.exception.BusinessException khi trùng tên dòng trong cùng hãng */
    void update(Long id, VehicleModelRequest form);

    VehicleModelResponse activate(Long id);

    VehicleModelResponse deactivate(Long id);
}
