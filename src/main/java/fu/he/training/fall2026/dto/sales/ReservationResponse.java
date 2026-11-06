package fu.he.training.fall2026.dto.sales;
import fu.he.training.fall2026.entity.enums.*;
import java.math.BigDecimal;
import java.time.*;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    private Long id;
    private String code;
    private Long inquiryId;
    private Long vehicleId;
    private Long buyerId;
    private Long assignedToId;
    private ReservationStatus status;
    private BigDecimal depositAmount;
    private LocalDate holdUntil;
    private String terms;
    private boolean consentStored;
    private Instant cancelledAt;
    private String cancelReason;
    private DepositDisposition depositDisposition;
    private Long transferReservationId;
    private String buyerName;
    private String vehicleVin;
    private String vehicleMake;
    private String vehicleModel;
    private String assignedToName;
    private DepositDisposition cancelDepositPolicy;
    private DepositDisposition expiryDepositPolicy;
    private BigDecimal confirmedCollectedAmount;
    private BigDecimal processedLiabilityAmount;
    private Long depositDecisionActorId;
    private String depositOperationKey;
    private String depositBalanceReference;
    private String depositProcessingReference;
    private String vehicleTransitionReference;

}
