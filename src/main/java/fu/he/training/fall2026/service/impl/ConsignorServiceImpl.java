package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.consignor.ConsignorForm;
import fu.he.training.fall2026.dto.consignor.ConsignorResponse;
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
    public Page<ConsignorResponse> search(String keyword, int page, int size) {
        String pattern = keyword == null || keyword.isBlank()
                ? "%"
                : "%" + keyword.trim().toLowerCase() + "%";
        return consignorRepository.search(
                        pattern, PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(ConsignorServiceImpl::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsignorResponse get(Long id) {
        Consignor consignor = consignorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chủ xe", id));
        return toDto(consignor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsignorResponse> listAll() {
        return consignorRepository.findAll(Sort.by("fullName")).stream().map(ConsignorServiceImpl::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConsignorForm formFor(Long id) {
        Consignor consignor = consignorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chủ xe", id));
        ConsignorForm form = new ConsignorForm();
        form.setFullName(consignor.getFullName());
        form.setPhone(consignor.getPhone());
        form.setEmail(consignor.getEmail());
        form.setIdNumber(consignor.getIdNumber());
        form.setAddress(consignor.getAddress());
        form.setBank(consignor.getBank());
        form.setAccountNumber(consignor.getAccountNumber());
        form.setAccountName(consignor.getAccountName());
        form.setConsentStored(consignor.isConsentStored());
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
        Consignor consignor = consignorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chủ xe", id));
        rejectDuplicate(form, id);
        apply(consignor, form);
        consignorRepository.save(consignor);
    }

    private static ConsignorResponse toDto(Consignor c) {
        return ConsignorResponse.builder()
                .id(c.getId())
                .fullName(c.getFullName())
                .phone(c.getPhone())
                .email(c.getEmail())
                .idNumber(c.getIdNumber())
                .address(c.getAddress())
                .bank(c.getBank())
                .accountNumber(c.getAccountNumber())
                .accountName(c.getAccountName())
                .consentStored(c.isConsentStored())
                .build();
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
