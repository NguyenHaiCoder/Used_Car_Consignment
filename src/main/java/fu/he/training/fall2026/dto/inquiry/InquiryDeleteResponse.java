package fu.he.training.fall2026.dto.inquiry;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InquiryDeleteResponse {
    private final InquiryResponse inquiry;
    private final String revision;
    private final boolean linkedRecords;
}

