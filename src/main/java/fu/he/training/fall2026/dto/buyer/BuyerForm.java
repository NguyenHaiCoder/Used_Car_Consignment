package fu.he.training.fall2026.dto.buyer;

import lombok.Getter;
import lombok.Setter;

/** HTML binding only; business validation remains in BuyerService. */
@Getter
@Setter
public class BuyerForm {
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private boolean consentStored;
    private String revision;
}
