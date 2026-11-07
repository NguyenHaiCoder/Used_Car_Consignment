package fu.he.training.fall2026.dto.finance;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

/** Vũ Mạnh Cường: chi tiết phiếu hoàn cọc. */
@Value
@Builder
public class RefundView {
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
    Map<String, Object> done;
}
