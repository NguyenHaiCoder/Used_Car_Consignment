package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.consignor.ConsignorForm;
import fu.he.training.fall2026.entity.consignment.Consignor;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.ConsignorRepository;
import fu.he.training.fall2026.service.ConsignorService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsignorServiceImpl implements ConsignorService {

    private final ConsignorRepository consignorRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Consignor> search(String keyword, int page, int size) {
        String pattern = keyword == null || keyword.isBlank()
                ? "%"
                : "%" + keyword.trim().toLowerCase() + "%";
        return consignorRepository.search(
                pattern, PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Override
    @Transactional(readOnly = true)
    public Consignor get(Long id) {
        return consignorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chủ xe", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Consignor> listAll() {
        return consignorRepository.findAll(Sort.by("fullName"));
    }

    @Override
    @Transactional(readOnly = true)
    public ConsignorForm formFor(Long id) {
        Consignor c = get(id);
        ConsignorForm form = new ConsignorForm();
        form.setFullName(c.getFullName());
        form.setPhone(c.getPhone());
        form.setEmail(c.getEmail());
        form.setIdNumber(c.getIdNumber());
        form.setAddress(c.getAddress());
        form.setBank(c.getBank());
        form.setAccountNumber(c.getAccountNumber());
        form.setAccountName(c.getAccountName());
        form.setConsentStored(c.isConsentStored());
        return form;
    }

    @Override
    @Transactional
    public Long create(ConsignorForm form) {
        rejectDuplicate(form, -1L);
        Consignor consignor = new Consignor();
        apply(consignor, form);
        return consignorRepository.save(consignor).getId();
    }

    @Override
    @Transactional
    public void update(Long id, ConsignorForm form) {
        Consignor consignor = get(id);
        rejectDuplicate(form, id);
        apply(consignor, form);
        consignorRepository.save(consignor);
    }

    private void rejectDuplicate(ConsignorForm form, Long excludeId) {
        if (consignorRepository.existsDuplicate(form.getPhone().trim(), form.getIdNumber().trim(), excludeId)) {
            throw new BusinessException(
                    "Số điện thoại hoặc CCCD này đã thuộc một chủ xe khác.", HttpStatus.CONFLICT);
        }
    }

    private static void apply(Consignor target, ConsignorForm form) {
        target.setFullName(form.getFullName().trim());
        target.setPhone(form.getPhone().trim());
        target.setEmail(blankToNull(form.getEmail()));
        target.setIdNumber(form.getIdNumber().trim());
        target.setAddress(blankToNull(form.getAddress()));
        target.setBank(form.getBank().trim());
        target.setAccountNumber(form.getAccountNumber().trim());
        target.setAccountName(form.getAccountName().trim());
        target.setConsentStored(form.isConsentStored());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
