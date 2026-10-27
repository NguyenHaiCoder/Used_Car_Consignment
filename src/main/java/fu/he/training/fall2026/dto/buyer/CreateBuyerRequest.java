package fu.he.training.fall2026.dto.buyer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBuyerRequest {
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private Boolean consentStored;
}