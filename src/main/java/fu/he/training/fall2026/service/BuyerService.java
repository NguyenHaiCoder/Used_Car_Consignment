package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.buyer.BuyerDetailsResponse;
import fu.he.training.fall2026.dto.buyer.BuyerListResponse;
import fu.he.training.fall2026.dto.buyer.CreateBuyerRequest;
import fu.he.training.fall2026.dto.buyer.InquiryHistoryResponse;
import fu.he.training.fall2026.dto.buyer.UpdateBuyerRequest;


public interface BuyerService {
    BuyerDetailsResponse getDetails(long id);

    BuyerDetailsResponse create(CreateBuyerRequest request);

    BuyerDetailsResponse update(long id, UpdateBuyerRequest patch);

    void delete(long id);

    BuyerListResponse list(int page, int size);

    BuyerListResponse list(int page, int size, String search);

    InquiryHistoryResponse getInquiryHistory(long buyerId, int page, int size);
}
