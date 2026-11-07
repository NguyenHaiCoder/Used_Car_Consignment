package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.catalog.BrandRequest;
import fu.he.training.fall2026.dto.catalog.BrandResponse;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

public interface BrandService {

    Page<BrandResponse> search(String keyword, CatalogStatus status, int page, int size);

    Map<CatalogStatus, Long> countByStatus();

    BrandResponse get(Long id);

    /** Hãng ACTIVE theo tên — cho dropdown dòng xe. */
    List<BrandResponse> listActive();

    /** ACTIVE + giữ {@code keepId} (kể cả INACTIVE) khi sửa form. */
    List<BrandResponse> listSelectable(Long keepId);

    BrandRequest formFor(Long id);

    /** @throws fu.he.training.fall2026.exception.BusinessException khi trùng tên hãng */
    Long create(BrandRequest form);

    /** @throws fu.he.training.fall2026.exception.BusinessException khi trùng tên hãng */
    void update(Long id, BrandRequest form);

    BrandResponse activate(Long id);

    /**
     * @throws fu.he.training.fall2026.exception.BusinessException nếu còn dòng xe ACTIVE thuộc hãng
     */
    BrandResponse deactivate(Long id);
}
