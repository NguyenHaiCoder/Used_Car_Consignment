package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.inspection.VehicleTypeRequest;
import fu.he.training.fall2026.dto.inspection.VehicleTypeResponse;
import fu.he.training.fall2026.entity.consignment.VehicleType;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.InspectionTypeRepository;
import fu.he.training.fall2026.repository.VehicleTypeRepository;
import fu.he.training.fall2026.service.VehicleTypeService;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VehicleTypeServiceImpl implements VehicleTypeService {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final InspectionTypeRepository inspectionTypeRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleTypeResponse> search(String keyword, CatalogStatus status, int page, int size) {
        String pattern = keyword == null || keyword.isBlank()
                ? "%"
                : "%" + keyword.trim().toLowerCase() + "%";
        return vehicleTypeRepository.search(pattern, status, PageRequest.of(Math.max(page, 0), size, Sort.by("id")))
                .map(VehicleTypeServiceImpl::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CatalogStatus, Long> countByStatus() {
        Map<CatalogStatus, Long> counts = new EnumMap<>(CatalogStatus.class);
        for (CatalogStatus status : CatalogStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : vehicleTypeRepository.countByStatus()) {
            counts.put((CatalogStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleTypeResponse> listActive() {
        return vehicleTypeRepository.findByStatusOrderByLabelAsc(CatalogStatus.ACTIVE).stream()
                .map(VehicleTypeServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleTypeResponse> listSelectable(Long keepId) {
        List<VehicleTypeResponse> types = new ArrayList<>(listActive());
        if (keepId != null && types.stream().noneMatch(t -> keepId.equals(t.getId()))) {
            vehicleTypeRepository.findById(keepId).map(VehicleTypeServiceImpl::toDto).ifPresent(types::add);
        }
        return types;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VehicleTypeResponse> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return vehicleTypeRepository.findByCode(code.trim()).map(VehicleTypeServiceImpl::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleTypeRequest formFor(Long id) {
        VehicleType type = load(id);
        VehicleTypeRequest form = new VehicleTypeRequest();
        form.setId(type.getId());
        form.setCode(type.getCode());
        form.setLabel(type.getLabel());
        form.setStatus(type.getStatus());
        return form;
    }

    @Override
    @Transactional
    public Long create(VehicleTypeRequest form) {
        rejectDuplicate(form, -1L);
        VehicleType type = new VehicleType();
        apply(type, form);
        return vehicleTypeRepository.save(type).getId();
    }

    @Override
    @Transactional
    public void update(Long id, VehicleTypeRequest form) {
        VehicleType type = load(id);
        rejectDuplicate(form, id);
        if (form.getStatus() == CatalogStatus.INACTIVE) {
            requireNotInUse(type);
        }
        apply(type, form);
        vehicleTypeRepository.save(type);
    }

    @Override
    @Transactional
    public VehicleTypeResponse activate(Long id) {
        VehicleType type = load(id);
        type.setStatus(CatalogStatus.ACTIVE);
        return toDto(type);
    }

    @Override
    @Transactional
    public VehicleTypeResponse deactivate(Long id) {
        VehicleType type = load(id);
        requireNotInUse(type);
        type.setStatus(CatalogStatus.INACTIVE);
        return toDto(type);
    }

    private VehicleType load(Long id) {
        return vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loại xe", id));
    }

    private void rejectDuplicate(VehicleTypeRequest form, Long excludeId) {
        String code = normalizeCode(form.getCode());
        if (vehicleTypeRepository.existsDuplicateCode(code, excludeId)) {
            throw new BusinessException("Mã loại xe đã tồn tại: " + code, HttpStatus.CONFLICT);
        }
    }

    private void requireNotInUse(VehicleType type) {
        if (inspectionTypeRepository.existsByVehicleTypeIdAndStatus(type.getId(), CatalogStatus.ACTIVE)) {
            throw new BusinessException(
                    "Loại xe \"" + type.getLabel() + "\" còn loại kiểm định đang dùng, hãy ngưng các loại kiểm định đó trước.",
                    HttpStatus.CONFLICT);
        }
    }

    private static VehicleTypeResponse toDto(VehicleType t) {
        return VehicleTypeResponse.builder()
                .id(t.getId())
                .code(t.getCode())
                .label(t.getLabel())
                .status(t.getStatus())
                .build();
    }

    private static void apply(VehicleType target, VehicleTypeRequest form) {
        target.setCode(normalizeCode(form.getCode()));
        target.setLabel(form.getLabel().trim());
        target.setStatus(form.getStatus());
    }

    /** "suv mini" → "SUV_MINI". */
    private static String normalizeCode(String code) {
        return code.trim().toUpperCase().replace(' ', '_');
    }
}
