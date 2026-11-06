package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.inspection.InspectionStepRequest;
import fu.he.training.fall2026.dto.inspection.InspectionStepResponse;
import fu.he.training.fall2026.dto.inspection.InspectionTypeRequest;
import fu.he.training.fall2026.dto.inspection.InspectionTypeResponse;
import fu.he.training.fall2026.entity.consignment.InspectionType;
import fu.he.training.fall2026.entity.consignment.InspectionTypeStep;
import fu.he.training.fall2026.entity.consignment.VehicleType;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.InspectionTypeRepository;
import fu.he.training.fall2026.repository.InspectionTypeStepRepository;
import fu.he.training.fall2026.repository.VehicleTypeRepository;
import fu.he.training.fall2026.service.InspectionTypeService;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InspectionTypeServiceImpl implements InspectionTypeService {

    private final InspectionTypeRepository inspectionTypeRepository;
    private final InspectionTypeStepRepository stepRepository;
    private final VehicleTypeRepository vehicleTypeRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<InspectionTypeResponse> search(String keyword, CatalogStatus status, int page, int size) {
        String pattern = keyword == null || keyword.isBlank()
                ? "%"
                : "%" + keyword.trim().toLowerCase() + "%";
        Page<InspectionType> result = inspectionTypeRepository.search(
                pattern, status, PageRequest.of(Math.max(page, 0), size, Sort.by("id")));
        Map<Long, Long> stepCounts = stepCounts(result.getContent());
        return result.map(t -> toDto(t, stepCounts.getOrDefault(t.getId(), 0L)));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CatalogStatus, Long> countByStatus() {
        Map<CatalogStatus, Long> counts = new EnumMap<>(CatalogStatus.class);
        for (CatalogStatus status : CatalogStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : inspectionTypeRepository.countByStatus()) {
            counts.put((CatalogStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public InspectionTypeResponse get(Long id) {
        InspectionType type = load(id);
        return toDto(type, stepCounts(List.of(type)).getOrDefault(id, 0L));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InspectionStepResponse> steps(Long id) {
        if (!inspectionTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Loại kiểm định", id);
        }
        return stepRepository.findByInspectionTypeIdOrderByStepOrderAsc(id).stream()
                .map(InspectionTypeServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InspectionTypeResponse> listActive() {
        List<InspectionType> types = inspectionTypeRepository.findAllByStatusWithVehicleType(CatalogStatus.ACTIVE);
        Map<Long, Long> stepCounts = stepCounts(types);
        return types.stream().map(t -> toDto(t, stepCounts.getOrDefault(t.getId(), 0L))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InspectionTypeRequest formFor(Long id) {
        InspectionType type = load(id);
        InspectionTypeRequest form = new InspectionTypeRequest();
        form.setId(type.getId());
        form.setName(type.getName());
        form.setVehicleTypeId(type.getVehicleType().getId());
        form.setStatus(type.getStatus());
        for (InspectionTypeStep step : stepRepository.findByInspectionTypeIdOrderByStepOrderAsc(id)) {
            InspectionStepRequest row = new InspectionStepRequest();
            row.setTitle(step.getTitle());
            row.setHint(step.getHint());
            form.getSteps().add(row);
        }
        return form;
    }

    @Override
    @Transactional
    public Long create(InspectionTypeRequest form) {
        VehicleType vehicleType = loadVehicleType(form.getVehicleTypeId());
        validate(form, vehicleType, -1L);
        InspectionType type = new InspectionType();
        apply(type, form, vehicleType);
        inspectionTypeRepository.save(type);
        saveSteps(type, form.getSteps());
        return type.getId();
    }

    @Override
    @Transactional
    public void update(Long id, InspectionTypeRequest form) {
        InspectionType type = load(id);
        VehicleType vehicleType = loadVehicleType(form.getVehicleTypeId());
        validate(form, vehicleType, id);
        apply(type, form, vehicleType);
        inspectionTypeRepository.save(type);
        stepRepository.deleteByInspectionTypeId(id);
        saveSteps(type, form.getSteps());
    }

    @Override
    @Transactional
    public InspectionTypeResponse activate(Long id) {
        InspectionType type = load(id);
        requireActive(type.getVehicleType());
        type.setStatus(CatalogStatus.ACTIVE);
        return toDto(type, stepCounts(List.of(type)).getOrDefault(id, 0L));
    }

    @Override
    @Transactional
    public InspectionTypeResponse deactivate(Long id) {
        InspectionType type = load(id);
        type.setStatus(CatalogStatus.INACTIVE);
        return toDto(type, stepCounts(List.of(type)).getOrDefault(id, 0L));
    }

    private InspectionType load(Long id) {
        return inspectionTypeRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loại kiểm định", id));
    }

    private VehicleType loadVehicleType(Long id) {
        return vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loại xe", id));
    }

    private void validate(InspectionTypeRequest form, VehicleType vehicleType, Long excludeId) {
        if (inspectionTypeRepository.existsDuplicateName(form.getName().trim(), excludeId)) {
            throw new BusinessException("Tên loại kiểm định đã tồn tại.", HttpStatus.CONFLICT);
        }
        if (form.getStatus() == CatalogStatus.ACTIVE) {
            requireActive(vehicleType);
        }
    }

    private static void requireActive(VehicleType vehicleType) {
        if (vehicleType.getStatus() != CatalogStatus.ACTIVE) {
            throw new BusinessException(
                    "Loại xe \"" + vehicleType.getLabel() + "\" đang ngưng hoạt động, không thể dùng cho loại kiểm định đang dùng.");
        }
    }

    private void saveSteps(InspectionType type, List<InspectionStepRequest> rows) {
        List<InspectionTypeStep> steps = new ArrayList<>(rows.size());
        for (InspectionStepRequest row : rows) {
            InspectionTypeStep step = new InspectionTypeStep();
            step.setInspectionType(type);
            step.setStepOrder(steps.size() + 1);
            step.setTitle(row.getTitle().trim());
            step.setHint(blankToNull(row.getHint()));
            steps.add(step);
        }
        stepRepository.saveAll(steps);
    }

    private Map<Long, Long> stepCounts(List<InspectionType> types) {
        Map<Long, Long> counts = new HashMap<>();
        if (types.isEmpty()) {
            return counts;
        }
        List<Long> ids = types.stream().map(InspectionType::getId).toList();
        for (Object[] row : stepRepository.countByInspectionTypeIds(ids)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private static InspectionTypeResponse toDto(InspectionType t, long stepCount) {
        VehicleType vehicleType = t.getVehicleType();
        return InspectionTypeResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .vehicleTypeId(vehicleType.getId())
                .vehicleTypeCode(vehicleType.getCode())
                .vehicleTypeLabel(vehicleType.getLabel())
                .status(t.getStatus())
                .stepCount(stepCount)
                .build();
    }

    private static InspectionStepResponse toDto(InspectionTypeStep s) {
        return InspectionStepResponse.builder()
                .order(s.getStepOrder())
                .title(s.getTitle())
                .hint(s.getHint())
                .build();
    }

    private static void apply(InspectionType target, InspectionTypeRequest form, VehicleType vehicleType) {
        target.setName(form.getName().trim());
        target.setVehicleType(vehicleType);
        target.setStatus(form.getStatus());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
