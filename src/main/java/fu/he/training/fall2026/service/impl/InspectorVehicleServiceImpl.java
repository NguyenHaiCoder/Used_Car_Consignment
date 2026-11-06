package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.inspection.InspectVehicleResponse;
import fu.he.training.fall2026.dto.inspection.InspectionQueueResponse;
import fu.he.training.fall2026.dto.inspection.InspectionResultRequest;
import fu.he.training.fall2026.dto.inspection.InspectionStepResultRequest;
import fu.he.training.fall2026.dto.storage.StoredFile;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.Inspection;
import fu.he.training.fall2026.entity.consignment.InspectionItem;
import fu.he.training.fall2026.entity.consignment.InspectionType;
import fu.he.training.fall2026.entity.consignment.InspectionTypeStep;
import fu.he.training.fall2026.entity.consignment.Valuation;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.enums.CatalogStatus;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.entity.enums.InspectionStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.ConsignmentRepository;
import fu.he.training.fall2026.repository.InspectionItemRepository;
import fu.he.training.fall2026.repository.InspectionRepository;
import fu.he.training.fall2026.repository.InspectionTypeRepository;
import fu.he.training.fall2026.repository.InspectionTypeStepRepository;
import fu.he.training.fall2026.repository.ValuationRepository;
import fu.he.training.fall2026.service.FileStorageService;
import fu.he.training.fall2026.service.InspectorVehicleService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InspectorVehicleServiceImpl implements InspectorVehicleService {

    private static final List<ConsignmentStatus> QUEUE_STATUSES = List.of(
            ConsignmentStatus.ACTIVE, ConsignmentStatus.INSPECTION_RECEIVED, ConsignmentStatus.INSPECTED);
    private static final String PHOTO_FOLDER = "inspections";
    /** InspectionItem.photoPath is 500 characters: five short storage keys fit. */
    private static final int MAX_PROOFS_PER_STEP = 5;
    private static final int PHOTO_PATH_LENGTH = 500;

    private final ConsignmentRepository consignmentRepository;
    private final InspectionRepository inspectionRepository;
    private final InspectionItemRepository inspectionItemRepository;
    private final ValuationRepository valuationRepository;
    private final InspectionTypeRepository inspectionTypeRepository;
    private final InspectionTypeStepRepository stepRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public Page<InspectionQueueResponse> searchQueue(String keyword, int page, int size) {
        String pattern = keyword == null || keyword.isBlank()
                ? "%"
                : "%" + keyword.trim().toLowerCase() + "%";
        Page<Consignment> result = consignmentRepository.searchByStatuses(
                pattern, QUEUE_STATUSES,
                PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<Long, LocalDate> inspectedOn = latestInspectedOn(result.getContent());
        return result.map(c -> toQueueDto(c, inspectedOn.get(c.getId())));
    }

    @Override
    @Transactional
    public void receive(Long consignmentId) {
        Consignment consignment = load(consignmentId);
        requireStatus(consignment, ConsignmentStatus.ACTIVE, "tiếp nhận");
        consignment.setStatus(ConsignmentStatus.INSPECTION_RECEIVED);
    }

    @Override
    @Transactional(readOnly = true)
    public InspectVehicleResponse getForInspection(Long consignmentId) {
        Consignment consignment = load(consignmentId);
        requireStatus(consignment, ConsignmentStatus.INSPECTION_RECEIVED, "kiểm định");
        Vehicle vehicle = consignment.getVehicle();
        return InspectVehicleResponse.builder()
                .id(consignment.getId())
                .code(consignment.getCode())
                .displayLabel(vehicle.getDisplayLabel())
                .vin(vehicle.getVin())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InspectionResultRequest formFor(Long consignmentId, Long inspectionTypeId) {
        InspectionResultRequest form = new InspectionResultRequest();
        form.setChecklistId(inspectionTypeId);
        if (inspectionTypeId != null) {
            int stepCount = stepRepository.findByInspectionTypeIdOrderByStepOrderAsc(inspectionTypeId).size();
            for (int i = 0; i < stepCount; i++) {
                form.getSteps().add(new InspectionStepResultRequest());
            }
        }
        valuationRepository.findFirstByConsignmentIdOrderByIdDesc(consignmentId).ifPresent(valuation -> {
            form.setProposedPrice(valuation.getProposedPrice());
            form.setRationale(valuation.getRationale());
        });
        return form;
    }

    @Override
    @Transactional
    public String submitInspection(Long consignmentId, InspectionResultRequest form) {
        Consignment consignment = load(consignmentId);
        requireStatus(consignment, ConsignmentStatus.INSPECTION_RECEIVED, "kiểm định");
        InspectionType type = inspectionTypeRepository.findDetailById(form.getChecklistId())
                .orElseThrow(() -> new ResourceNotFoundException("Loại kiểm định", form.getChecklistId()));
        if (type.getStatus() != CatalogStatus.ACTIVE) {
            throw new BusinessException("Loại kiểm định \"" + type.getName() + "\" đã ngưng hoạt động.");
        }
        List<InspectionTypeStep> steps = stepRepository.findByInspectionTypeIdOrderByStepOrderAsc(type.getId());
        if (form.getSteps().size() != steps.size()) {
            throw new BusinessException("Danh sách bước không khớp với loại kiểm định, vui lòng tải lại trang.");
        }
        for (int i = 0; i < steps.size(); i++) {
            if (selectedFiles(form.getSteps().get(i)) > MAX_PROOFS_PER_STEP) {
                throw new BusinessException("Bước " + (i + 1) + ": tối đa " + MAX_PROOFS_PER_STEP + " ảnh chứng minh.");
            }
        }

        Inspection inspection = new Inspection();
        inspection.setCode(temporaryCode());
        inspection.setConsignment(consignment);
        inspection.setVehicle(consignment.getVehicle());
        inspection.setInspectionType(type);
        inspection.setStatus(InspectionStatus.COMPLETED);
        inspection.setInspectedOn(LocalDate.now());
        inspectionRepository.save(inspection);
        inspection.setCode("KD-" + inspection.getId());

        List<StoredFile> uploaded = new ArrayList<>();
        try {
            List<InspectionItem> items = new ArrayList<>(steps.size());
            for (int i = 0; i < steps.size(); i++) {
                InspectionStepResultRequest row = form.getSteps().get(i);
                List<StoredFile> photos = fileStorageService.uploadImages(row.getProofs(), PHOTO_FOLDER);
                uploaded.addAll(photos);
                items.add(toItem(inspection, type, steps.get(i), row, photos));
            }
            inspectionItemRepository.saveAll(items);

            Valuation valuation = new Valuation();
            valuation.setCode(temporaryCode());
            valuation.setConsignment(consignment);
            valuation.setProposedPrice(form.getProposedPrice());
            valuation.setRationale(blankToNull(form.getRationale()));
            valuation.setValuedOn(LocalDate.now());
            valuationRepository.save(valuation);
            valuation.setCode("DG-" + valuation.getId());

            consignment.setStatus(ConsignmentStatus.INSPECTED);
            inspectionItemRepository.flush();
        } catch (RuntimeException ex) {
            uploaded.forEach(photo -> deleteQuietly(photo.getPath()));
            throw ex;
        }
        return consignment.getCode();
    }

    private InspectionItem toItem(
            Inspection inspection,
            InspectionType type,
            InspectionTypeStep step,
            InspectionStepResultRequest row,
            List<StoredFile> photos) {
        String photoPath = photos.isEmpty() ? null : fileStorageService.joinPaths(photos);
        if (photoPath != null && photoPath.length() > PHOTO_PATH_LENGTH) {
            throw new BusinessException("Bước " + step.getStepOrder() + ": quá nhiều ảnh chứng minh, hãy bớt ảnh.");
        }
        InspectionItem item = new InspectionItem();
        item.setInspection(inspection);
        item.setGroupName(type.getName());
        item.setItemName(step.getTitle());
        item.setResult(row.isPassed() ? "PASS" : "FAIL");
        item.setNotes(blankToNull(row.getNote()));
        item.setPhotoPath(photoPath);
        return item;
    }

    private Consignment load(Long id) {
        return consignmentRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ ký gửi", id));
    }

    private Map<Long, LocalDate> latestInspectedOn(List<Consignment> consignments) {
        Map<Long, LocalDate> dates = new HashMap<>();
        if (consignments.isEmpty()) {
            return dates;
        }
        List<Long> ids = consignments.stream().map(Consignment::getId).toList();
        for (Object[] row : inspectionRepository.findLatestInspectedOn(ids)) {
            dates.put((Long) row[0], (LocalDate) row[1]);
        }
        return dates;
    }

    private void deleteQuietly(String path) {
        try {
            fileStorageService.delete(path);
        } catch (BusinessException ignored) {
            // The photo stays on storage; the inspection itself was not saved.
        }
    }

    private static void requireStatus(Consignment consignment, ConsignmentStatus expected, String action) {
        if (consignment.getStatus() != expected) {
            throw new BusinessException(
                    "Hồ sơ " + consignment.getCode() + " không ở trạng thái phù hợp để " + action + ".");
        }
    }

    private static long selectedFiles(InspectionStepResultRequest row) {
        return row.getProofs() == null ? 0 : row.getProofs().stream().filter(f -> f != null && !f.isEmpty()).count();
    }

    private static InspectionQueueResponse toQueueDto(Consignment c, LocalDate inspectedOn) {
        Vehicle vehicle = c.getVehicle();
        return InspectionQueueResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .displayLabel(vehicle.getDisplayLabel())
                .year(vehicle.getYear())
                .vin(vehicle.getVin())
                .consignorName(c.getConsignor().getFullName())
                .odometerKm(vehicle.getOdometerKm())
                .intakeDate(c.getIntakeDate())
                .inspectedOn(inspectedOn)
                .status(c.getStatus())
                .build();
    }

    /** Replaced by the real code once the id is known. */
    private static String temporaryCode() {
        return "TMP-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
