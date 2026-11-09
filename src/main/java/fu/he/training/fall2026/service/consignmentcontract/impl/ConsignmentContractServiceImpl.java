package fu.he.training.fall2026.service.consignmentcontract.impl;

import fu.he.training.fall2026.dto.consignment.ConsignmentContractRequest;
import fu.he.training.fall2026.dto.consignment.ConsignmentContractResponse;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.ConsignmentAgreement;
import fu.he.training.fall2026.entity.enums.AgreementStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.ConsignmentAgreementRepository;
import fu.he.training.fall2026.repository.ConsignmentRepository;
import fu.he.training.fall2026.service.FileStorageService;
import fu.he.training.fall2026.service.consignmentcontract.ConsignmentContractService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ConsignmentContractServiceImpl implements ConsignmentContractService {


    private final ConsignmentAgreementRepository agreementRepository;
    private final ConsignmentRepository consignmentRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<ConsignmentContractResponse> listAll() {
        return agreementRepository.findAllWithConsignment().stream()
                .map(ConsignmentContractServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConsignmentContractResponse> findByConsignment(Long consignmentId) {
        return agreementRepository.findByConsignmentId(consignmentId)
                .map(ConsignmentContractServiceImpl::toDto);
    }

    @Override
    @Transactional
    public void create(ConsignmentContractRequest form) {
        Consignment consignment = consignmentRepository.findById(form.getConsignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ ký gửi", form.getConsignmentId()));
        if (agreementRepository.findByConsignmentId(consignment.getId()).isPresent()) {
            throw new BusinessException("Hồ sơ này đã có hợp đồng.", HttpStatus.CONFLICT);
        }
        ConsignmentAgreement agreement = new ConsignmentAgreement();
        agreement.setCode("HDKG-" + consignment.getId());
        agreement.setConsignment(consignment);
        agreement.setStatus(AgreementStatus.DRAFT);
        agreement.setCommissionRate(form.getCommissionRate());
        agreement.setPriceFloor(form.getPriceFloor());
        agreement.setValidFrom(form.getValidFrom());
        agreement.setValidTo(form.getValidTo());
        agreement.setCustodyTerms(form.getCustodyTerms().trim());
        agreement.setTerminationTerms(form.getTerminationTerms().trim());
        agreement.setPayoutTerms(form.getPayoutTerms().trim());
        storeEvidence(agreement, form.getSignatureEvidence());
        agreementRepository.save(agreement);
    }

    @Override
    @Transactional
    public void updateDraft(ConsignmentContractRequest form) {
        ConsignmentAgreement agreement = agreementRepository
                .findByConsignmentId(form.getConsignmentId())
                .orElseThrow(() -> new BusinessException("Hồ sơ này chưa có hợp đồng."));
        if (agreement.getStatus() != AgreementStatus.DRAFT) {
            throw new BusinessException("Chỉ hợp đồng lưu nháp mới được sửa hoặc gửi duyệt.", HttpStatus.CONFLICT);
        }
        agreement.setCommissionRate(form.getCommissionRate());
        agreement.setPriceFloor(form.getPriceFloor());
        agreement.setValidFrom(form.getValidFrom());
        agreement.setValidTo(form.getValidTo());
        agreement.setCustodyTerms(form.getCustodyTerms().trim());
        agreement.setTerminationTerms(form.getTerminationTerms().trim());
        agreement.setPayoutTerms(form.getPayoutTerms().trim());
        storeEvidence(agreement, form.getSignatureEvidence());
    }

    @Override
    @Transactional
    public void submit(Long consignmentId) {
        ConsignmentAgreement agreement = agreementRepository
                .findByConsignmentId(consignmentId)
                .orElseThrow(() -> new BusinessException("Hồ sơ này chưa có hợp đồng."));
        if (agreement.getStatus() != AgreementStatus.DRAFT) {
            throw new BusinessException("Chỉ hợp đồng lưu nháp mới được sửa hoặc gửi duyệt.", HttpStatus.CONFLICT);
        }
        agreement.setStatus(AgreementStatus.PENDING_SIGNATURE);
    }

    private static ConsignmentContractResponse toDto(ConsignmentAgreement a) {
        return ConsignmentContractResponse.builder()
                .id(a.getId())
                .code(a.getCode())
                .status(a.getStatus())
                .commissionRate(a.getCommissionRate())
                .fixedFee(a.getFixedFee())
                .priceFloor(a.getPriceFloor())
                .validFrom(a.getValidFrom())
                .validTo(a.getValidTo())
                .expired(a.getStatus() == AgreementStatus.ACTIVE
                        && a.getValidTo() != null
                        && a.getValidTo().isBefore(LocalDate.now()))
                .custodyTerms(a.getCustodyTerms())
                .terminationTerms(a.getTerminationTerms())
                .payoutTerms(a.getPayoutTerms())
                .signatureEvidencePath(a.getSignatureEvidencePath())
                .consignmentId(a.getConsignment().getId())
                .consignmentCode(a.getConsignment().getCode())
                .consignorName(a.getConsignment().getConsignor().getFullName())
                .build();
    }

    /** Có chọn tệp mới thì tải lên và thay tệp cũ; không chọn thì giữ nguyên tệp đã lưu. */
    private void storeEvidence(ConsignmentAgreement agreement, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        String oldPath = agreement.getSignatureEvidencePath();
        agreement.setSignatureEvidencePath(fileStorageService.uploadDocument(file, "contracts").getPath());
        if (oldPath != null) {
            fileStorageService.delete(oldPath);
        }
    }
}
