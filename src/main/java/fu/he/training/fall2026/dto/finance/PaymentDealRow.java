package fu.he.training.fall2026.dto.finance;

import lombok.Builder;
import lombok.Value;

/** Vũ Mạnh Cường: một dòng list Giao dịch thanh toán (payment hoặc refund). */
@Value
@Builder
public class PaymentDealRow {
    Long id;
    String kind;
    String code;
    String reference;
    String contractCode;
    String buyer;
    String phone;
    String car;
    String plate;
    String file;
    String staff;
    String owner;
    long amount;
    long collected;
    long remaining;
    String status;
    String created;
    String reason;
}
