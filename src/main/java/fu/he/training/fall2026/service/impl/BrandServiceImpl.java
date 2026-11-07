package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.catalog.BrandRequest;
import fu.he.training.fall2026.dto.catalog.BrandResponse;
import fu.he.training.fall2026.entity.consignment.Brand;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.BrandRepository;
import fu.he.training.fall2026.repository.VehicleModelRepository;
import fu.he.training.fall2026.service.BrandService;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final VehicleModelRepository vehicleModelRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<BrandResponse> search(String keyword, CatalogStatus status, int page, int size) {
        String pattern = keyword == null || keyword.isBlank()
                ? "%"
                : "%" + keyword.trim().toLowerCase() + "%";
        return brandRepository
                .search(pattern, status, PageRequest.of(Math.max(page, 0), size, Sort.by("id")))
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CatalogStatus, Long> countByStatus() {
        Map<CatalogStatus, Long> counts = new EnumMap<>(CatalogStatus.class);
        for (CatalogStatus value : CatalogStatus.values()) {
            counts.put(value, 0L);
        }
        for (Object[] row : brandRepository.countByStatus()) {
            counts.put((CatalogStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse get(Long id) {
        return toDto(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> listActive() {
        return brandRepository.findByStatusOrderByNameAsc(CatalogStatus.ACTIVE).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> listSelectable(Long keepId) {
        List<BrandResponse> brands = new ArrayList<>(listActive());
        if (keepId != null && brands.stream().noneMatch(b -> keepId.equals(b.getId()))) {
            brandRepository.findById(keepId).map(this::toDto).ifPresent(brands::add);
        }
        return brands;
    }

    @Override
    @Transactional(readOnly = true)
    public BrandRequest formFor(Long id) {
        Brand brand = load(id);
        BrandRequest form = new BrandRequest();
        form.setId(brand.getId());
        form.setName(brand.getName());
        form.setStatus(brand.getStatus());
        form.setNote(brand.getNote());
        return form;
    }

    @Override
    @Transactional
    public Long create(BrandRequest form) {
        rejectDuplicateName(form.getName(), -1L);
        Brand brand = new Brand();
        brand.setCode(temporaryCode());
        apply(brand, form);
        brandRepository.save(brand);
        brand.setCode(generateCode(brand.getId()));
        return brand.getId();
    }

    @Override
    @Transactional
    public void update(Long id, BrandRequest form) {
        Brand brand = load(id);
        rejectDuplicateName(form.getName(), id);
        if (form.getStatus() == CatalogStatus.INACTIVE) {
            requireNoActiveModels(brand);
        }
        apply(brand, form);
        brandRepository.save(brand);
    }

    @Override
    @Transactional
    public BrandResponse activate(Long id) {
        Brand brand = load(id);
        brand.setStatus(CatalogStatus.ACTIVE);
        return toDto(brand);
    }

    @Override
    @Transactional
    public BrandResponse deactivate(Long id) {
        Brand brand = load(id);
        requireNoActiveModels(brand);
        brand.setStatus(CatalogStatus.INACTIVE);
        return toDto(brand);
    }

    /** Mã hiển thị hãng xe: HX-{id}. */
    private static String generateCode(Long id) {
        return "HX-" + id;
    }

    private static String temporaryCode() {
        return "TMP-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private Brand load(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hãng xe", id));
    }

    private void rejectDuplicateName(String name, Long excludeId) {
        if (brandRepository.existsDuplicateName(name.trim(), excludeId)) {
            throw new BusinessException("Tên hãng xe đã tồn tại: " + name.trim(), HttpStatus.CONFLICT);
        }
    }

    private void requireNoActiveModels(Brand brand) {
        if (vehicleModelRepository.existsByBrandIdAndStatus(brand.getId(), CatalogStatus.ACTIVE)) {
            throw new BusinessException(
                    "Hãng \"" + brand.getName() + "\" còn dòng xe đang dùng, hãy ngưng các dòng xe đó trước.",
                    HttpStatus.CONFLICT);
        }
    }

    private BrandResponse toDto(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .code(brand.getCode())
                .name(brand.getName())
                .status(brand.getStatus())
                .note(brand.getNote())
                .modelCount(vehicleModelRepository.countByBrandId(brand.getId()))
                .build();
    }

    private static void apply(Brand target, BrandRequest form) {
        target.setName(form.getName().trim());
        target.setStatus(form.getStatus());
        target.setNote(blankToNull(form.getNote()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
