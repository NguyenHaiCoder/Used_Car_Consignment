package fu.he.training.fall2026.dto.sales;

import fu.he.training.fall2026.entity.enums.DepositDisposition;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class ReservationEditRequest {
    @NotBlank
    private String revision;

    @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 16, fraction = 2)
    private BigDecimal depositAmount;

    @NotNull @FutureOrPresent @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate holdUntil;

    @NotBlank @Size(max = 2000)
    private String terms;

    @NotNull
    private DepositDisposition cancelDepositPolicy;

    @NotNull
    private DepositDisposition expiryDepositPolicy;

    @NotNull @AssertTrue
    private Boolean consentStored;
}
