package fu.he.training.fall2026.dto.sales;

import fu.he.training.fall2026.entity.enums.DepositDisposition;
import jakarta.validation.constraints.*;
import jakarta.validation.groups.Default;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {
    @NotNull(groups = Create.class) @Positive private Long inquiryId;
    @NotNull(groups = Create.class) @Positive private Long vehicleId;
    @NotNull(groups = Create.class) @Positive private Long buyerId;
    @Positive private Long assignedToId;
    @NotNull(groups = Create.class) @DecimalMin(value = "0", inclusive = false) @Digits(integer = 16, fraction = 2) private BigDecimal depositAmount;
    @NotNull(groups = Create.class) @FutureOrPresent @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate holdUntil;
    @NotBlank(groups = Create.class) @Size(max = 2000) private String terms;
    @NotNull(groups = Create.class) @AssertTrue private Boolean consentStored;
    @NotNull(groups = Create.class) private DepositDisposition cancelDepositPolicy;
    @NotNull(groups = Create.class) private DepositDisposition expiryDepositPolicy;
    @NotNull(groups = Finish.class) private DepositDisposition disposition;
    @NotBlank(groups = Finish.class) @Size(max = 2000) private String reason;
    @Positive private Long transferReservationId;

    public interface Create extends Default {}
    public interface Finish extends Default {}

    public ReservationRequest(Long inquiryId, Long vehicleId, Long buyerId, Long assignedToId,
            BigDecimal depositAmount, LocalDate holdUntil, String terms, Boolean consentStored,
            DepositDisposition cancelDepositPolicy, DepositDisposition expiryDepositPolicy) {
        this(inquiryId, vehicleId, buyerId, assignedToId, depositAmount, holdUntil, terms,
                consentStored, cancelDepositPolicy, expiryDepositPolicy, null, null, null);
    }

    public ReservationRequest(Long inquiryId, Long vehicleId, Long buyerId, Long assignedToId,
            BigDecimal depositAmount, LocalDate holdUntil, String terms, Boolean consentStored) {
        this(inquiryId, vehicleId, buyerId, assignedToId, depositAmount, holdUntil, terms,
                consentStored, null, null);
    }

    public ReservationRequest(DepositDisposition disposition, String reason, Long transferReservationId) {
        this(null, null, null, null, null, null, null, null, null, null,
                disposition, reason, transferReservationId);
    }

}
