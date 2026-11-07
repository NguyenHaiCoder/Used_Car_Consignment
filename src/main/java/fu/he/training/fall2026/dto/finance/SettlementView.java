package fu.he.training.fall2026.dto.finance;

import lombok.Builder;
import lombok.Value;

/** Vũ Mạnh Cường: quyết toán (list + detail) — tên field khớp template. */
@Value
@Builder
public class SettlementView {
    Long id;
    String code;
    String contractCode;
    String car;
    String plate;
    String file;
    String owner;
    String buyer;
    String staff;
    long salePrice;
    String commissionRate;
    long commission;
    long fee;
    long net;
    String status;
    String created;
    String bank;
    String account;
    String rejectReason;
    String reviewedBy;
    String payoutCode;
    String payoutReference;
    String paidOn;
}
