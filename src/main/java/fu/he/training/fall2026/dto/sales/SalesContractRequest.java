package fu.he.training.fall2026.dto.sales;

import fu.he.training.fall2026.entity.enums.ContractStatus;
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
public class SalesContractRequest {
    @NotNull(groups = Create.class) @Positive private Long reservationId;
    @NotNull(groups = Create.class) @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 16, fraction = 2) private BigDecimal salePrice;
    @Size(max = 4000) private String terms;
    @Size(max = 500) private String sellerSignaturePath;
    @Size(max = 500) private String buyerSignaturePath;
    @PastOrPresent @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate signedOn;
    private ContractStatus status;

    public interface Create extends Default {}

    public SalesContractRequest(Long reservationId, BigDecimal salePrice, String terms) {
        this(reservationId, salePrice, terms, null, null, null, null);
    }

    public SalesContractRequest(BigDecimal salePrice, String terms, String sellerSignaturePath,
            String buyerSignaturePath, LocalDate signedOn, ContractStatus status) {
        this(null, salePrice, terms, sellerSignaturePath, buyerSignaturePath, signedOn, status);
    }

}
