package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.manager.DecideApprovalRequest;
import fu.he.training.fall2026.entity.auth.Approval;
import fu.he.training.fall2026.entity.auth.User;
import java.util.List;

public interface ManagerApprovalService {

    List<Approval> listApprovals();

    Approval getApproval(Long id);

    Approval decide(Long id, DecideApprovalRequest request, User actor);
}
