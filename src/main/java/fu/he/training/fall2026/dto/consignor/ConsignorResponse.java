package fu.he.training.fall2026.dto.consignor;

import lombok.Builder;
import lombok.Value;

/** A consignor as shown on the list and detail screens. */
@Value
@Builder
public class ConsignorResponse {

    Long id;
    String fullName;
    String phone;
    String email;
    String idNumber;
    String address;
    String bank;
    String accountNumber;
    String accountName;
    boolean consentStored;
}
