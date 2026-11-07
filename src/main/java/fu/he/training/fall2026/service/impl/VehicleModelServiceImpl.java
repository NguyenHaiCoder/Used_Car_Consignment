package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.catalog.VehicleModelRequest;
import fu.he.training.fall2026.dto.catalog.VehicleModelResponse;
import fu.he.training.fall2026.entity.consignment.Brand;
import fu.he.training.fall2026.entity.consignment.VehicleModel;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.BrandRepository;
import fu.he.training.fall2026.repository.VehicleModelRepository;
import fu.he.training.fall2026.service.VehicleModelService;
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
public class VehicleModelServiceImpl implements VehicleModelService {

    private final VehicleModelRepository vehicleModelRepository;
    private final BrandRepository brandRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleModelResponse> search(
            String keyword, CatalogStatus status, Long brandId, int page, int size) {
        String pattern = keyword == null || keyword.isBlank()
                ? "%"
                : "%" + keyword.trim().toLowerCase() + "%";
        return vehicleModelRepository
                .search(pattern, status, brandId, PageRequest.of(Math.max(page, 0), size, Sort.by("id")))
                .map(VehicleModelServiceImpl::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CatalogStatus, Long> countByStatus() {
        Map<CatalogStatus, Long> counts = new EnumMap<>(CatalogStatus.class);
        for (CatalogStatus value : CatalogStatus.values()) {
            counts.put(value, 0L);
        }
        for (Object[] row : vehicleModelRepository.countByStatus()) {
            counts.put((CatalogStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleModelResponse get(Long id) {
        return toDto(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleModelResponse> listActive() {
        return vehicleModelRepository.findByStatusWithBrandOrderByNameAsc(CatalogStatus.ACTIVE).stream()
                .map(VehicleModelServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleModelResponse> listActiveByBrand(Long brandId) {
        return vehicleModelRepository.findByBrandIdAndStatusOrderByNameAsc(brandId, CatalogStatus.ACTIVE).stream()
                .map(VehicleModelServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleModelRequest formFor(Long id) {
        VehicleModel model = load(id);
        VehicleModelRequest form = new VehicleModelRequest();
        form.setId(model.getId());
        form.setBrandId(model.getBrand().getId());
        form.setName(model.getName());
        form.setStatus(model.getStatus());
        form.setNote(model.getNote());
        return form;
    }

    @Override
    @Transactional
    public Long create(VehicleModelRequest form) {
        Brand brand = loadBrand(form.getBrandId());
        rejectDuplicateName(brand.getId(), form.getName(), -1L);
        VehicleModel model = new VehicleModel();
        model.setCode(temporaryCode());
        apply(model, form, brand);
        vehicleModelRepository.save(model);
        model.setCode(generateCode(model.getId()));
        return model.getId();
    }

    @Override
    @Transactional
    public void update(Long id, VehicleModelRequest form) {
        VehicleModel model = load(id);
        Brand brand = loadBrand(form.getBrandId());
        rejectDuplicateName(brand.getId(), form.getName(), id);
        apply(model, form, brand);
        vehicleModelRepository.save(model);
    }

    @Override
    @Transactional
    public VehicleModelResponse activate(Long id) {
        VehicleModel model = load(id);
        model.setStatus(CatalogStatus.ACTIVE);
        return toDto(model);
    }

    @Override
    @Transactional
    public VehicleModelResponse deactivate(Long id) {
        VehicleModel model = load(id);
        model.setStatus(CatalogStatus.INACTIVE);
        return toDto(model);
    }

    /** Mã hiển thị dòng xe: DX-{id}. */
    private static String generateCode(Long id) {
        return "DX-" + id;
    }

    private static String temporaryCode() {
        return "TMP-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private VehicleModel load(Long id) {
        return vehicleModelRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dòng xe", id));
    }

    private Brand loadBrand(Long brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Hãng xe", brandId));
    }

    private void rejectDuplicateName(Long brandId, String name, Long excludeId) {
        if (vehicleModelRepository.existsDuplicateName(brandId, name.trim(), excludeId)) {
            throw new BusinessException(
                    "Dòng xe \"" + name.trim() + "\" đã tồn tại trong hãng này.", HttpStatus.CONFLICT);
        }
    }

    private static VehicleModelResponse toDto(VehicleModel model) {
        return VehicleModelResponse.builder()
                .id(model.getId())
                .code(model.getCode())
                .brandId(model.getBrand().getId())
                .brandName(model.getBrand().getName())
                .name(model.getName())
                .status(model.getStatus())
                .note(model.getNote())
                .build();
    }

    private static void apply(VehicleModel target, VehicleModelRequest form, Brand brand) {
        target.setBrand(brand);
        target.setName(form.getName().trim());
        target.setStatus(form.getStatus());
        target.setNote(blankToNull(form.getNote()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
