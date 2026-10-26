package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.consignment.AgreementForm;
import fu.he.training.fall2026.dto.consignment.AgreementResponse;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.ConsignmentAgreement;
import fu.he.training.fall2026.entity.enums.AgreementStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.ConsignmentAgreementRepository;
import fu.he.training.fall2026.repository.ConsignmentRepository;
import fu.he.training.fall2026.service.ConsignmentAgreementService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsignmentAgreementServiceImpl implements ConsignmentAgreementService {

    private final ConsignmentAgreementRepository agreementRepository;
    private final ConsignmentRepository consignmentRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<AgreementResponse> findLatest(Long consignmentId) {
        return agreementRepository.findFirstByConsignmentIdOrderByVersionNoDesc(consignmentId)
                .map(ConsignmentAgreementServiceImpl::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgreementResponse> versions(Long consignmentId) {
        return agreementRepository.findByConsignmentIdOrderByVersionNoDesc(consignmentId).stream()
                .map(ConsignmentAgreementServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AgreementForm formFor(Long consignmentId) {
        ConsignmentAgreement agreement = agreementRepository.findFirstByConsignmentIdOrderByVersionNoDesc(consignmentId)
                .orElseThrow(() -> new BusinessException("Hồ sơ này chưa có hợp đồng."));
        requireDraft(agreement);
        AgreementForm form = new AgreementForm();
        form.setId(agreement.getId());
        form.setConsignmentId(consignmentId);
        form.setCommissionRate(agreement.getCommissionRate());
        form.setFixedFee(agreement.getFixedFee());
        form.setPriceFloor(agreement.getPriceFloor());
        form.setValidFrom(agreement.getValidFrom());
        form.setValidTo(agreement.getValidTo());
        form.setCustodyTerms(agreement.getCustodyTerms());
        form.setTerminationTerms(agreement.getTerminationTerms());
        form.setPayoutTerms(agreement.getPayoutTerms());
        form.setSignatureEvidencePath(agreement.getSignatureEvidencePath());
        return form;
    }

    @Override
    @Transactional
    public void createDraft(Long consignmentId, AgreementForm form) {
        validateTerms(form);
        Consignment consignment = consignmentRepository.findById(consignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ ký gửi", consignmentId));
        if (agreementRepository.findFirstByConsignmentIdOrderByVersionNoDesc(consignmentId).isPresent()) {
            throw new BusinessException("Hồ sơ này đã có hợp đồng.", HttpStatus.CONFLICT);
        }
        ConsignmentAgreement agreement = new ConsignmentAgreement();
        agreement.setCode("HDKG-" + consignment.getId());
        agreement.setVersionNo(1);
        agreement.setConsignment(consignment);
        agreement.setStatus(AgreementStatus.DRAFT);
        apply(agreement, form);
        agreementRepository.save(agreement);
    }

    @Override
    @Transactional
    public void updateDraft(Long consignmentId, AgreementForm form) {
        validateTerms(form);
        ConsignmentAgreement agreement = agreementRepository.findFirstByConsignmentIdOrderByVersionNoDesc(consignmentId)
                .orElseThrow(() -> new BusinessException("Hồ sơ này chưa có hợp đồng."));
        requireDraft(agreement);
        apply(agreement, form);
        agreementRepository.save(agreement);
    }

    @Override
    @Transactional
    public void submit(Long consignmentId) {
        ConsignmentAgreement agreement = agreementRepository.findFirstByConsignmentIdOrderByVersionNoDesc(consignmentId)
                .orElseThrow(() -> new BusinessException("Hồ sơ này chưa có hợp đồng."));
        requireDraft(agreement);
        agreement.setStatus(AgreementStatus.PENDING_SIGNATURE);
    }

    @Override
    @Transactional
    public void createAmendment(Long consignmentId) {
        ConsignmentAgreement current = agreementRepository.findFirstByConsignmentIdOrderByVersionNoDesc(consignmentId)
                .orElseThrow(() -> new BusinessException("Chưa có hợp đồng để sửa đổi."));
        if (current.getStatus() != AgreementStatus.ACTIVE) {
            throw new BusinessException("Chỉ hợp đồng ACTIVE mới tạo được phiên bản sửa đổi.", HttpStatus.CONFLICT);
        }
        ConsignmentAgreement next = new ConsignmentAgreement();
        next.setCode(current.getCode());
        next.setVersionNo(current.getVersionNo() + 1);
        next.setConsignment(current.getConsignment());
        next.setStatus(AgreementStatus.DRAFT);
        next.setCommissionRate(current.getCommissionRate());
        next.setFixedFee(current.getFixedFee());
        next.setPriceFloor(current.getPriceFloor());
        next.setValidFrom(current.getValidFrom());
        next.setValidTo(current.getValidTo());
        next.setCustodyTerms(current.getCustodyTerms());
        next.setTerminationTerms(current.getTerminationTerms());
        next.setPayoutTerms(current.getPayoutTerms());
        agreementRepository.save(next);
    }

    private static AgreementResponse toDto(ConsignmentAgreement a) {
        return AgreementResponse.builder()
                .id(a.getId())
                .code(a.getCode())
                .versionNo(a.getVersionNo())
                .status(a.getStatus())
                .commissionRate(a.getCommissionRate())
                .fixedFee(a.getFixedFee())
                .priceFloor(a.getPriceFloor())
                .validFrom(a.getValidFrom())
                .validTo(a.getValidTo())
                .custodyTerms(a.getCustodyTerms())
                .terminationTerms(a.getTerminationTerms())
                .payoutTerms(a.getPayoutTerms())
                .signatureEvidencePath(a.getSignatureEvidencePath())
                .build();
    }

    private static void apply(ConsignmentAgreement target, AgreementForm form) {
        target.setCommissionRate(form.getCommissionRate());
        target.setFixedFee(form.getFixedFee());
        target.setPriceFloor(form.getPriceFloor());
        target.setValidFrom(form.getValidFrom());
        target.setValidTo(form.getValidTo());
        target.setCustodyTerms(form.getCustodyTerms().trim());
        target.setTerminationTerms(form.getTerminationTerms().trim());
        target.setPayoutTerms(form.getPayoutTerms().trim());
        target.setSignatureEvidencePath(blankToNull(form.getSignatureEvidencePath()));
    }

    /** Only a DRAFT may be edited or submitted; signed terms change only through a new version. */
    private static void requireDraft(ConsignmentAgreement agreement) {
        if (agreement.getStatus() != AgreementStatus.DRAFT) {
            throw new BusinessException(
                    "Hợp đồng đã gửi duyệt hoặc đã ký không sửa trực tiếp được. Hãy tạo phiên bản sửa đổi.",
                    HttpStatus.CONFLICT);
        }
    }

    /** BR-03: a commission (rate or fixed fee) is required, and the validity period must make sense. */
    private static void validateTerms(AgreementForm form) {
        boolean hasRate = form.getCommissionRate() != null && form.getCommissionRate().signum() > 0;
        boolean hasFee = form.getFixedFee() != null && form.getFixedFee().compareTo(BigDecimal.ZERO) > 0;
        if (hasRate == hasFee) {
            throw new BusinessException("Nhập hoa hồng theo % hoặc phí cố định (chỉ một trong hai).");
        }
        if (form.getValidTo().isBefore(form.getValidFrom())) {
            throw new BusinessException("Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
