package fu.he.training.fall2026.dto.consignment;

import fu.he.training.fall2026.entity.enums.AgreementStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

/** One version of a consignment agreement. */
@Value
@Builder
public class AgreementResponse {

    Long id;
    String code;
    int versionNo;
    AgreementStatus status;
    BigDecimal commissionRate;
    BigDecimal fixedFee;
    BigDecimal priceFloor;
    LocalDate validFrom;
    LocalDate validTo;
    String custodyTerms;
    String terminationTerms;
    String payoutTerms;
    String signatureEvidencePath;
}
