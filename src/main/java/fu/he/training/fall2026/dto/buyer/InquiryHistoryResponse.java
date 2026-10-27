package fu.he.training.fall2026.dto.buyer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InquiryHistoryResponse {
    private List<InquiryHistoryItemResponse> data;
    private BuyerListResponse.Pagination pagination;
}