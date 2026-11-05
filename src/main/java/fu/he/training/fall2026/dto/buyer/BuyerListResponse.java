package fu.he.training.fall2026.dto.buyer;

import fu.he.training.fall2026.dto.common.PaginationResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuyerListResponse {
    private List<BuyerResponse> data;
    private PaginationResponse pagination;
}
