package fu.he.training.fall2026.dto.documents;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

/** Hồ sơ sang tên — list / detail / preview create. */
@Value
@Builder
public class TransferResponse {

    Long id;
    String code;
    Long salesContractId;
    String contractCode;
    String salesStaff;
    LocalDate signedOn;
    long salePrice;
    String buyer;
    String phone;
    String buyerIdNumber;
    String buyerAddress;
    String car;
    String file;
    String plate;
    String vin;
    String owner;
    /** PENDING | IN_PROGRESS | COMPLETED */
    String status;
    String staff;
    LocalDate deadline;
    long daysLeft;
    String notes;
    String evidence;
    String evidencePath;
    LocalDate completedOn;
    boolean hasEvidenceFile;
}
