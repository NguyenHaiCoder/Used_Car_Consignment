package fu.he.training.fall2026.dto.consignment;

import fu.he.training.fall2026.entity.enums.AgreementStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

/** Một phiên bản hợp đồng ký gửi kèm mã hồ sơ và tên chủ xe (phẳng, không tách đối tượng con). */
@Value
@Builder
public class ConsignmentContractResponse {

    // ── Hợp đồng ──
    Long id;
    String code;
    AgreementStatus status;
    BigDecimal commissionRate;
    BigDecimal fixedFee;
    BigDecimal priceFloor;
    LocalDate validFrom;
    LocalDate validTo;
    /** Hết hạn = đang ACTIVE nhưng đã quá ngày kết thúc (validTo); suy ra khi đọc, không lưu thành trạng thái. */
    boolean expired;
    String custodyTerms;
    String terminationTerms;
    String payoutTerms;
    String signatureEvidencePath;

    // ── Hồ sơ ký gửi ──
    Long consignmentId;
    String consignmentCode;
    String consignorName;
}
