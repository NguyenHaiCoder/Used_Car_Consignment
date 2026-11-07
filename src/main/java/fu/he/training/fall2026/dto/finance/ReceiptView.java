package fu.he.training.fall2026.dto.finance;

import lombok.Builder;
import lombok.Value;

/** Vũ Mạnh Cường: một dòng khoản đã thu trên chi tiết thanh toán. */
@Value
@Builder
public class ReceiptView {
    String type;
    long amount;
    String method;
    String reference;
    String paidOn;
    String staff;
}
