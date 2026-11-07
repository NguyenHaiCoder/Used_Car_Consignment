package fu.he.training.fall2026.dto.documents;

import lombok.Builder;
import lombok.Value;

/** Một dòng timeline “Hồ sơ đạt chuẩn” / “Mốc đóng sale”. */
@Value
@Builder
public class TransferCheckResponse {

    String label;
    String note;
    /** Chuỗi "true"/"false" — template parts.html so sánh == 'true'. */
    String passed;
}
