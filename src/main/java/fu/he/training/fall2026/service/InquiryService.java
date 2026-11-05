package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.inquiry.InquiryAssignmentRequest;
import fu.he.training.fall2026.dto.inquiry.InquiryHistoryResponse;
import fu.he.training.fall2026.dto.inquiry.InquirySaveRequest;
import fu.he.training.fall2026.dto.inquiry.InquiryResponse;
import fu.he.training.fall2026.dto.inquiry.InquiryDeleteRequest;
import fu.he.training.fall2026.dto.inquiry.InquiryDeleteResponse;
import fu.he.training.fall2026.dto.sales.SalesOptionResponse;
import fu.he.training.fall2026.entity.enums.InquiryStatus;
import java.util.List;
import org.springframework.data.domain.Page;

public interface InquiryService {
    Page<InquiryResponse> list(String search, InquiryStatus status, Long salespersonId, int page, int size);
    InquiryResponse getDetails(long id);
    long create(InquirySaveRequest form);
    InquirySaveRequest formFor(long id);
    void update(long id, InquirySaveRequest form);
    InquiryDeleteResponse deletionFor(long id);
    void delete(long id, InquiryDeleteRequest form);
    void assign(long id, InquiryAssignmentRequest form);
    InquiryHistoryResponse getBuyerHistory(long buyerId, int page, int size);
    List<SalesOptionResponse> buyers();
    List<SalesOptionResponse> vehicles(Long buyerId);
    List<SalesOptionResponse> salespeople();
    List<SalesOptionResponse> assignableSalespeople();
}
