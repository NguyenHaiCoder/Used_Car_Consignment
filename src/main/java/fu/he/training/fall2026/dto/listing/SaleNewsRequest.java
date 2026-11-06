package fu.he.training.fall2026.dto.listing;

import fu.he.training.fall2026.entity.enums.ListingStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class SaleNewsRequest {

    /** Hidden field: empty when creating, the listing id when editing. */
    private Long id;

    @NotNull(message = "Vui lòng chọn xe đã kiểm định.")
    private Long consignmentId;

    @NotBlank(message = "Vui lòng nhập tiêu đề.")
    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự.")
    private String title;

    @NotBlank(message = "Vui lòng nhập mô tả.")
    @Size(max = 8000, message = "Mô tả tối đa 8000 ký tự.")
    private String body;

    @NotNull(message = "Vui lòng nhập giá niêm yết.")
    @DecimalMin(value = "0", inclusive = false, message = "Giá niêm yết phải lớn hơn 0.")
    private BigDecimal listPrice;

    @NotNull(message = "Vui lòng chọn trạng thái tin.")
    private ListingStatus status = ListingStatus.LISTED;

    /** New photos; when editing, leaving this empty keeps the current photos. */
    private List<MultipartFile> images = new ArrayList<>();
}
