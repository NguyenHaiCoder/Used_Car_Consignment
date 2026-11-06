package fu.he.training.fall2026.dto.sales;
import fu.he.training.fall2026.entity.enums.*;
import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositPolicyResponse {
    private Long reservationId;
    private ReservationStatus status;
    private BigDecimal depositAmount;
    private String terms;
    private LocalDate holdUntil;
    private boolean overdue;
    private DepositDisposition suggestedDisposition;
    private DepositDisposition recordedDisposition;
    private Long transferReservationId;
    private String note;

}
