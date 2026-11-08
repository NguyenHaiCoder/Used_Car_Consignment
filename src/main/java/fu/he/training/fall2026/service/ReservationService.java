package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.sales.DealListFilter;
import fu.he.training.fall2026.dto.sales.ReservationForm;
import fu.he.training.fall2026.dto.sales.ReservationResponse;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.ui.Model;

/** Luồng Đặt cọc giữ xe (Reservation). */
public interface ReservationService {

    void bindList(Model model, String base, DealListFilter filter);

    Page<ReservationResponse> search(DealListFilter filter);

    Map<String, Long> countByStatus();

    ReservationResponse findById(Long id);

    /** Xe còn bán được (LISTED) và chưa có phiếu ACTIVE / hợp đồng. key = vehicleId. */
    Map<String, Map<String, Object>> availableCars();

    /** Khách đang tư vấn xe này (OPEN/SCHEDULED/VIEWED) và chưa có phiếu ACTIVE. */
    List<ReservationResponse> holdableBuyers(Long vehicleId);

    Long create(ReservationForm form, Long staffUserId);

    void update(Long id, ReservationForm form);

    void cancel(Long id, String reason, String decision);

    /** reservationId → contractId (nếu đã lập HĐ). */
    Map<Long, Long> contractIdsByReservation();

    java.util.Optional<Long> contractIdOf(Long reservationId);
}
