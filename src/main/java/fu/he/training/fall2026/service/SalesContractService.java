package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.sales.DealListFilter;
import fu.he.training.fall2026.dto.sales.ReservationResponse;
import fu.he.training.fall2026.dto.sales.SalesContractForm;
import fu.he.training.fall2026.dto.sales.SalesContractResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.ui.Model;

/** Luồng Hợp đồng bán xe. */
public interface SalesContractService {

    void bindList(Model model, String base, DealListFilter filter);

    SalesContractResponse findById(Long id);

    Optional<SalesContractResponse> findByReservationId(Long reservationId);

    /** Phiếu ACTIVE chưa có hợp đồng — để chọn khi tạo HĐ. */
    List<ReservationResponse> activeReservations();

    /** Bản nháp chưa lưu (id = null), điền sẵn từ phiếu cọc. */
    SalesContractResponse draftFromReservation(Long reservationId);

    Long create(Long reservationId, SalesContractForm form);

    void update(Long id, SalesContractForm form);

    List<java.util.Map<String, String>> steps(SalesContractResponse contract);
}
