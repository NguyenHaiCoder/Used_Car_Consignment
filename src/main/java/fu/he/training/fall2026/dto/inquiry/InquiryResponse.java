package fu.he.training.fall2026.dto.inquiry;
import fu.he.training.fall2026.dto.sales.ViewingResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InquiryResponse {
    private Long id;
    private String code;
    private Long buyerId;
    private String buyer;
    private String phone;
    private String email;
    private String vehicle;
    private String vin;
    private String salesperson;
    private Long salespersonId;
    private LocalDate date;
    private String status;
    private String notes;
    private String channel;
    private String need;
    private List<ViewingResponse> viewings = List.of();
    private List<InquiryActivityResponse> activities = List.of();

    public InquiryResponse(Long id, String code, Long buyerId, String buyer, String phone, String email, String vehicle, String vin, String salesperson, Long salespersonId, LocalDate date, String status, String notes) {
        this.id = id;
        this.code = code;
        this.buyerId = buyerId;
        this.buyer = buyer;
        this.phone = phone;
        this.email = email;
        this.vehicle = vehicle;
        this.vin = vin;
        this.salesperson = salesperson;
        this.salespersonId = salespersonId;
        this.date = date;
        this.status = status;
        this.notes = notes;
    }

}
