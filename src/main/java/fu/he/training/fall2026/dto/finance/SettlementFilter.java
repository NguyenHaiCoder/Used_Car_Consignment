package fu.he.training.fall2026.dto.finance;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/** Vũ Mạnh Cường: bộ lọc danh sách Quyết toán (GET form). */
@Getter
@Setter
public class SettlementFilter {

    private String q;
    private String status;
    private String staff;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    /** Đơn vị triệu đồng. */
    private Integer amountMin;

    /** Đơn vị triệu đồng. */
    private Integer amountMax;
}
