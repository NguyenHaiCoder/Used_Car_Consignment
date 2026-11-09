package fu.he.training.fall2026.dto.sales;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Form lưu / ký hợp đồng bán xe. */
@Getter
@Setter
public class SalesContractForm {

    @NotBlank(message = "Vui lòng nhập giá bán.")
    private String salePrice = "";

    private String buyerIdNumber = "";
    private String buyerAddress = "";
    private String paymentTerms = "";
    private String handoverTerms = "";
    private String terminationTerms = "";
    private String sellerSignature = "";
    private String buyerSignature = "";
    /** "sign" = ký hợp đồng; khác = chỉ lưu nháp. */
    private String intent = "";
}
