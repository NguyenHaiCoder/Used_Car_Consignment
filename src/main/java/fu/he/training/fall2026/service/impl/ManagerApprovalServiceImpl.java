package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.manager.DecideApprovalRequest;
import fu.he.training.fall2026.entity.auth.Approval;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.enums.ApprovalStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.repository.ApprovalRepository;
import fu.he.training.fall2026.service.AuditService;
import fu.he.training.fall2026.service.ManagerApprovalService;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManagerApprovalServiceImpl implements ManagerApprovalService {

    private final ApprovalRepository approvalRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<Approval> listApprovals() {
        return approvalRepository.findAllFetched();
    }

    @Override
    @Transactional(readOnly = true)
    public Approval getApproval(Long id) {
        return approvalRepository.findByIdFetched(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy yêu cầu phê duyệt.", HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional
    public Approval decide(Long id, DecideApprovalRequest request, User actor) {
        Approval approval = getApproval(id);
        if (approval.getStatus() != ApprovalStatus.PENDING
                && approval.getStatus() != ApprovalStatus.NEED_MORE_INFO) {
            throw new BusinessException("Yêu cầu đã được quyết định trước đó.");
        }
        ApprovalStatus status;
        try {
            status = ApprovalStatus.valueOf(request.getDecision().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Quyết định không hợp lệ.");
        }
        if (status == ApprovalStatus.PENDING) {
            throw new BusinessException("Phải chọn phê duyệt, từ chối hoặc yêu cầu bổ sung.");
        }
        approval.setStatus(status);
        approval.setReason(request.getReason().trim());
        approval.setApprovedBy(actor);
        approval.setDecidedAt(Instant.now());
        approvalRepository.save(approval);

        auditService.record(actor, "DECIDE_APPROVAL", "Approval", approval.getId(),
                approval.getCode() + " → " + status.name() + ": " + approval.getReason());
        return approval;
    }
}
