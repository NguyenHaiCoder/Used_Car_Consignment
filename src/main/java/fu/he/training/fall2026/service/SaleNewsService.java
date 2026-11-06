package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.listing.ListingVehicleResponse;
import fu.he.training.fall2026.dto.listing.SaleNewsFilter;
import fu.he.training.fall2026.dto.listing.SaleNewsRequest;
import fu.he.training.fall2026.dto.listing.SaleNewsResponse;
import fu.he.training.fall2026.dto.listing.StaffOptionResponse;
import fu.he.training.fall2026.entity.enums.ListingStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;

/** Nguyễn Vũ Trường An: Quản lý tin đăng / Đăng tin / Sửa tin. */
public interface SaleNewsService {

    Page<SaleNewsResponse> search(SaleNewsFilter filter, int page, int size);

    /** Number of listings per status (every status is present, 0 when empty). */
    Map<ListingStatus, Long> countByStatus();

    List<StaffOptionResponse> listStaffs();

    SaleNewsResponse get(Long id);

    List<String> imageUrls(Long id);

    /** INSPECTED consignments, for the "Chọn xe đã kiểm định" pick-list. */
    List<ListingVehicleResponse> listReadyVehicles();

    /** The consignment when it is INSPECTED, empty otherwise. */
    Optional<ListingVehicleResponse> findReadyVehicle(Long consignmentId);

    ListingVehicleResponse getVehicle(Long consignmentId);

    /** New form for the vehicle: title "Hãng Dòng Năm" and the valued price. */
    SaleNewsRequest newForm(ListingVehicleResponse vehicle);

    SaleNewsRequest formFor(Long id);

    /**
     * Publishes a listing owned by {@code actorUserId}, then INSPECTED → LISTED on the consignment.
     *
     * @throws fu.he.training.fall2026.exception.BusinessException unless the consignment is INSPECTED, the status is
     *     LISTED or PAUSED and 1–10 photos are chosen
     */
    Long create(SaleNewsRequest form, Long actorUserId);

    /**
     * Updates the content and status; the consignment and the owner never change. New photos replace the current
     * ones, no photo keeps them.
     *
     * @throws fu.he.training.fall2026.exception.BusinessException when the listing is SOLD or the status is invalid
     */
    void update(Long id, SaleNewsRequest form);

    /** LISTED → PAUSED. @throws fu.he.training.fall2026.exception.BusinessException unless LISTED */
    SaleNewsResponse pause(Long id);

    /** PAUSED → LISTED. @throws fu.he.training.fall2026.exception.BusinessException unless PAUSED */
    SaleNewsResponse resume(Long id);
}
