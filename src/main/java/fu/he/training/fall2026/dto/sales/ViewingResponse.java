package fu.he.training.fall2026.dto.sales;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViewingResponse {
    private Long id;
    private Long inquiryId;
    private String buyer;
    private String vehicle;
    private String type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String salesperson;
    private String status;
    private String location;
    private String notes;

}
