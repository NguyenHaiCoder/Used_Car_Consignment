package fu.he.training.fall2026.dto.consignment;

import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

/** Một hồ sơ ký gửi kèm đủ thông tin chủ xe và xe (phẳng, không tách đối tượng con). */
@Value
@Builder
public class ConsignmentRequestResponse {

    // ── Hồ sơ ký gửi ──
    Long id;
    String code;
    ConsignmentStatus status;
    BigDecimal expectedPrice;
    LocalDate intakeDate;
    String termNotes;
    String custodyLocation;
    boolean ownerConsent;

    // ── Chủ xe ──
    Long consignorId;
    String consignorName;
    String phone;
    String email;
    String idNumber;
    String address;
    String bank;
    String accountNumber;
    String accountName;
    boolean consentStored;

    // ── Xe ──
    Long vehicleId;
    /** Hãng + dòng xe, ví dụ "Hyundai Tucson". */
    String displayLabel;
    String make;
    String model;
    String vin;
    String engineNumber;
    String licensePlate;
    Integer year;
    String color;
    Integer odometerKm;
    String vehicleType;
    String transmission;
    String fuelType;
    String location;
    String intakeCondition;
}
