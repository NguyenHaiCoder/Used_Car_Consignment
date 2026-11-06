package fu.he.training.fall2026.dto.sales;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReservationEligibilityResponse {
    private final Long inquiryId;
    private final String vehicleStatus;
    private final boolean vehicleEligible;
    private final String vehicleReason;
    private final boolean noActiveReservation;
}
