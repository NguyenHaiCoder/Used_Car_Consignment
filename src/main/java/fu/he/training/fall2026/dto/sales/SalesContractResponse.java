package fu.he.training.fall2026.dto.sales;

import lombok.Getter;
import lombok.Setter;

/** DTO hợp đồng bán xe — field khớp template sales/contracts. */
@Getter
@Setter
public class SalesContractResponse {

    private Long id;
    private String code;
    private Long reservationId;
    private String reservationCode;
    private Long inquiryId;
    private String inquiryCode;
    private String buyer;
    private String phone;
    private String email;
    private String car;
    private String file;
    private String plate;
    private int km;
    private String color;
    private String fuel;
    private String owner;
    private String vin;
    private String staff;
    private String make;
    private String model;
    private String year;
    private long listedPrice;
    private long salePrice;
    private long deposit;
    private long remaining;
    private String status;
    private String created;
    private String signedOn;
    private String buyerIdNumber;
    private String buyerAddress;
    private String paymentTerms;
    private String handoverTerms;
    private String terminationTerms;
    private String sellerSignature;
    private String buyerSignature;
    private boolean paid;
    private boolean transferred;
    private boolean handedOver;
    /** PENDING, READY, COMPLETED, FINALIZED — dùng cho bước Bàn giao xe trên tiến độ. */
    private String handoverStatus = "";
    private boolean paymentRequested;
}
