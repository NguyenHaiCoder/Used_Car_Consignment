package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.handover.HandoverConfirmRequest;
import fu.he.training.fall2026.dto.handover.HandoverFilter;
import fu.he.training.fall2026.dto.handover.HandoverView;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

/** Vũ Mạnh Cường: bàn giao xe và đóng sale. */
public interface HandoverService {

    List<String> ITEMS = List.of(
            "Đăng ký xe mới", "Sổ bảo hiểm", "Sổ bảo dưỡng", "Lốp dự phòng", "Bộ dụng cụ");

    Map<String, Map<String, String>> statusMeta();

    Page<HandoverView> search(HandoverFilter filter, int page, Integer size);

    Map<String, Long> countByStatus();

    List<String> listStaffNames();

    HandoverView getByContractId(Long contractId);

    List<Map<String, String>> saleMilestones(Long contractId);

    void confirm(Long contractId, HandoverConfirmRequest form, Long actorUserId, String actorName);

    void closeSale(Long contractId);

    HandoverView receipt(Long contractId);

    void ensureRowsForSignedContracts();
}
