package fu.he.training.fall2026.dto.handover;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/** Vũ Mạnh Cường: bàn giao (list + detail + biên nhận). */
@Value
@Builder
public class HandoverView {
    Long id;
    String code;
    String contractCode;
    String car;
    String plate;
    String file;
    Integer km;
    String color;
    String vin;
    String buyer;
    String phone;
    String buyerIdNumber;
    String buyerAddress;
    String signedOn;
    long salePrice;
    boolean paid;
    boolean transferred;
    boolean handedOver;
    String status;
    String staff;
    int odo;
    int keys;
    List<String> items;
    String notes;
    String handedOn;
    String staffSignature;
    String receiverSignature;
}
