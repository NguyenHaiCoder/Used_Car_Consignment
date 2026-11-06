package fu.he.training.fall2026.dto.sales;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.enums.SellerPartyType;
import fu.he.training.fall2026.entity.enums.ShopRole;
import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesContractResponse {
    private Long id;
    private String code;
    private Long reservationId;
    private ContractStatus status;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private Long ownerId;
    private String ownerName;
    private Long vehicleId;
    private String vin;
    private String make;
    private String model;
    private BigDecimal salePrice;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;
    private String terms;
    private LocalDate signedOn;
    private String sellerSignaturePath;
    private String buyerSignaturePath;
    private boolean saleClosed;
    private SellerPartyType sellerPartyType;
    private String sellerIdentityNumber;
    private String authorizationReference;
    private LocalDate authorizationValidUntil;
    private ShopRole shopRole;
    private BigDecimal appliedDepositAmount;
    private boolean depositReconciled;

}
