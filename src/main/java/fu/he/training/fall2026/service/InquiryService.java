package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.sales.InquiryRequest;

/** Yêu cầu tư vấn (Inquiry) từ trang Tin bán xe công khai. */
public interface InquiryService {

    /**
     * Tạo Inquiry OPEN gắn xe của tin LISTED (không tạo Buyer).
     *
     * @return mã TV-xxxxxx
     */
    String createFromListing(InquiryRequest form);
}
