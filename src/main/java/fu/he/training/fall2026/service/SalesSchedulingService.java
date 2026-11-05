package fu.he.training.fall2026.service;
import fu.he.training.fall2026.dto.sales.*;
import org.springframework.data.domain.Page;
import java.time.LocalDate;
import java.util.List;
public interface SalesSchedulingService {
    long scheduleViewingFromForm(ViewingFormRequest form);
    long scheduleViewing(ViewingRequest form);
    ViewingFormRequest formForViewing(long id);
    ViewingResponse viewingForDelete(long id);
    void updateViewing(long id, ViewingFormRequest form);
    void deleteViewing(long id, String revision);
    Page<ViewingResponse> viewings(LocalDate date, String status, Long salespersonId, int page, int size);
    Page<ViewingResponse> viewings(LocalDate date, String status, Long salespersonId, String search, String type,
                                   int page, int size);
    List<ViewingResponse> viewingsForInquiry(long inquiryId);
    List<SalesOptionResponse> salespeople();
    List<SalesOptionResponse> assignableSalespeople();
}
