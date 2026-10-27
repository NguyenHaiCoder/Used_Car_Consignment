package fu.he.training.fall2026.dto.buyer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuyerItemResponse {
    private Long buyerId;
    private String fullName;
    private String phone;
    private String email;
    private Instant createdAt;
}