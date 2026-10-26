package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.consignment.ConsignmentForm;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.Consignor;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.ConsignmentRepository;
import fu.he.training.fall2026.repository.ConsignorRepository;
import fu.he.training.fall2026.repository.VehicleRepository;
import fu.he.training.fall2026.service.ConsignmentService;
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
public class ConsignmentServiceImpl implements ConsignmentService {

    private final ConsignmentRepository consignmentRepository;
    private final ConsignorRepository consignorRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Consignment> search(String keyword, ConsignmentStatus status, int page, int size) {
        String pattern = keyword == null || keyword.isBlank()
                ? "%"
                : "%" + keyword.trim().toLowerCase() + "%";
        return consignmentRepository.search(
                pattern, status, PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ConsignmentStatus, Long> countByStatus() {
        Map<ConsignmentStatus, Long> counts = new EnumMap<>(ConsignmentStatus.class);
        for (ConsignmentStatus status : ConsignmentStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : consignmentRepository.countByStatus()) {
            counts.put((ConsignmentStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public Consignment get(Long id) {
        return consignmentRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ ký gửi", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> listVehicles() {
        return vehicleRepository.findAll(Sort.by("make", "model"));
    }

    @Override
    @Transactional(readOnly = true)
    public ConsignmentForm formFor(Long id) {
        Consignment c = get(id);
        requireDraft(c);
        ConsignmentForm form = new ConsignmentForm();
        form.setConsignorId(c.getConsignor().getId());
        form.setVehicleId(c.getVehicle().getId());
        form.setExpectedPrice(c.getExpectedPrice());
        form.setIntakeDate(c.getIntakeDate());
        form.setTermNotes(c.getTermNotes());
        form.setOwnerConsent(c.isOwnerConsent());
        return form;
    }

    @Override
    @Transactional
    public Long createDraft(ConsignmentForm form) {
        Consignment consignment = new Consignment();
        consignment.setCode("TMP-" + UUID.randomUUID().toString().substring(0, 12)); // replaced by KG-{id} below
        consignment.setStatus(ConsignmentStatus.DRAFT);
        applyForm(consignment, form);
        consignmentRepository.save(consignment);
        consignment.setCode("KG-" + consignment.getId());
        return consignment.getId();
    }

    @Override
    @Transactional
    public void updateDraft(Long id, ConsignmentForm form) {
        Consignment consignment = get(id);
        requireDraft(consignment);
        applyForm(consignment, form);
    }

    @Override
    @Transactional
    public void submit(Long id) {
        Consignment consignment = get(id);
        requireDraft(consignment);
        consignment.setStatus(ConsignmentStatus.SUBMITTED);
    }

    /** BR-11: only a DRAFT can be edited or submitted. */
    private static void requireDraft(Consignment consignment) {
        if (consignment.getStatus() != ConsignmentStatus.DRAFT) {
            throw new BusinessException("Chỉ hồ sơ DRAFT mới được sửa hoặc gửi duyệt.", HttpStatus.CONFLICT);
        }
    }

    private void applyForm(Consignment target, ConsignmentForm form) {
        Consignor consignor = consignorRepository.findById(form.getConsignorId())
                .orElseThrow(() -> new BusinessException("Chủ xe không tồn tại."));
        Vehicle vehicle = vehicleRepository.findById(form.getVehicleId())
                .orElseThrow(() -> new BusinessException("Xe không tồn tại."));
        target.setConsignor(consignor);
        target.setVehicle(vehicle);
        target.setExpectedPrice(form.getExpectedPrice());
        target.setIntakeDate(form.getIntakeDate());
        target.setTermNotes(form.getTermNotes() == null || form.getTermNotes().isBlank() ? null : form.getTermNotes().trim());
        target.setOwnerConsent(form.isOwnerConsent());
    }
}
