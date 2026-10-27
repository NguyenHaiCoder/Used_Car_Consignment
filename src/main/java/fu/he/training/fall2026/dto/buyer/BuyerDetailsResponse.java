package fu.he.training.fall2026.dto.buyer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuyerDetailsResponse {
    private Long buyerId;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private Instant createdAt;
    private Instant revision;

    public BuyerDetailsResponse(Long buyerId, String fullName, String phone, String email,
                                String address, Instant createdAt) {
        this(buyerId, fullName, phone, email, address, createdAt, null);
    }
}