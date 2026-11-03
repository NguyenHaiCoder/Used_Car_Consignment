package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.buyer.BuyerResponse;
import fu.he.training.fall2026.dto.buyer.BuyerRequest;
import fu.he.training.fall2026.dto.buyer.BuyerListResponse;
import fu.he.training.fall2026.dto.buyer.InquiryHistoryResponse;


public interface BuyerService {
    BuyerResponse getDetails(long id);

    BuyerRequest formFor(long id);

    BuyerResponse create(BuyerRequest form);

    BuyerResponse update(long id, BuyerRequest form);

    void delete(long id, String reason);

    BuyerListResponse list(int page, int size);

    BuyerListResponse list(int page, int size, String search);

    BuyerListResponse list(int page, int size, String search, String idSort, String status, String createdSort);

    InquiryHistoryResponse getInquiryHistory(long buyerId, int page, int size);
}
