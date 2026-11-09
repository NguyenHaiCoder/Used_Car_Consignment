package fu.he.training.fall2026.dto.sales;

import lombok.Getter;
import lombok.Setter;

/** DTO phiếu đặt cọc — field khớp template sales/reservations. */
@Getter
@Setter
public class ReservationResponse {

    private Long id;
    private String code;
    private Long inquiryId;
    private String inquiryCode;
    private String ini;
    private String buyer;
    private String phone;
    private String email;
    private Long vehicleId;
    private String car;
    private String file;
    private String plate;
    private int km;
    private long price;
    private String color;
    private String fuel;
    private String owner;
    private String vin;
    private String staff;
    private String status;
    private long deposit;
    private String holdUntil;
    private long daysLeft;
    private String terms;
    private String created;
    private String cancelReason;
    private String cancelledAt;
    private String depositDecision;
}
