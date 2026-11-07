package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.documents.TransferCheckResponse;
import fu.he.training.fall2026.dto.documents.TransferResponse;
import fu.he.training.fall2026.entity.enums.TransferStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface TransferService {

    /** Meta trạng thái cho KPI/pill: key = PENDING|…, value = label/tone/icon/pill. */
    Map<String, Map<String, String>> statusMeta();

    Page<TransferResponse> search(
            String keyword,
            TransferStatus status,
            String staff,
            LocalDate from,
            LocalDate to,
            Long amountMinMillion,
            Long amountMaxMillion,
            int page,
            int size);

    Map<TransferStatus, Long> countByStatus();

    List<String> listStaffNames();

    TransferResponse get(Long id);

    /** Hợp đồng đủ điều kiện tạo hồ sơ, chưa có TransferCase. */
    List<TransferResponse> candidates();

    /** Preview trước khi tạo — theo contractId. */
    TransferResponse preview(Long salesContractId);

    List<TransferCheckResponse> transferChecks(Long salesContractId);

    List<TransferCheckResponse> saleMilestones(Long salesContractId);

    List<Map<String, String>> steps(TransferStatus status);

    /** @throws fu.he.training.fall2026.exception.BusinessException khi chưa đủ điều kiện / đã có hồ sơ */
    Long create(Long salesContractId);

    /** PENDING → IN_PROGRESS; gán responsibleUser = actorUserId. */
    void start(Long id, Long actorUserId);

    /** IN_PROGRESS → COMPLETED; upload bằng chứng. */
    void complete(Long id, String notes, MultipartFile evidence);

    String evidenceUrl(Long id);
}
