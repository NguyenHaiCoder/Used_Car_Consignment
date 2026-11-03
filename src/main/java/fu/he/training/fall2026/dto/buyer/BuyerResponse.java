package fu.he.training.fall2026.dto.buyer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuyerResponse {
    private Long buyerId;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private Instant createdAt;
    private Instant revision;
    private boolean active;
    private String deleteReason;

    public String getStatus() {
        return active ? "Active" : "Inactive";
    }

    public String getDeletionReasonDisplay() {
        return hasDeletionReason() ? deleteReason : "Chưa ghi nhận";
    }

    public boolean hasDeletionReason() {
        return deleteReason != null && !deleteReason.isBlank();
    }

    public LocalDate getCreatedDate() {
        return createdAt == null ? null : createdAt.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
    }
}
