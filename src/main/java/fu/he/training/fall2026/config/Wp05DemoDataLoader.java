package fu.he.training.fall2026.config;

import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.ConsignmentAgreement;
import fu.he.training.fall2026.entity.consignment.Consignor;
import fu.he.training.fall2026.entity.consignment.TransferCase;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.consignment.VehicleSaleNews;
import fu.he.training.fall2026.entity.enums.AgreementStatus;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.enums.InquiryStatus;
import fu.he.training.fall2026.entity.enums.ListingStatus;
import fu.he.training.fall2026.entity.enums.PaymentStatus;
import fu.he.training.fall2026.entity.enums.ReservationStatus;
import fu.he.training.fall2026.entity.enums.TransferStatus;
import fu.he.training.fall2026.entity.payment.Payment;
import fu.he.training.fall2026.entity.sales.Buyer;
import fu.he.training.fall2026.entity.sales.Inquiry;
import fu.he.training.fall2026.entity.sales.Reservation;
import fu.he.training.fall2026.entity.sales.SalesContract;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dev-only demo data for WP05 (payments, settlement, handover) while WP04 sales contracts and WP06 transfer cases
 * have no screens yet. Seeds three signed contracts once, only when the database has no sales contract:
 * <ul>
 *   <li>HDB-D1 Camry: deposit paid, balance still to collect, transfer in progress.</li>
 *   <li>HDB-D2 CX-5: fully paid, transfer completed — can go settlement → payout → handover → close sale.</li>
 *   <li>HDB-D3 City: sold below the price floor, payOS receipt without webhook, transfer blocked.</li>
 * </ul>
 * Delete this class once the real WP04/WP06 flows create these records.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Wp05DemoDataLoader implements ApplicationRunner {

    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Long contracts = entityManager.createQuery("select count(sc) from SalesContract sc", Long.class).getSingleResult();
        if (contracts > 0) {
            return;
        }
        SalesContract camry = scenario(1, "Đỗ Minh Đức", "Toyota", "Camry 2.5Q", 2021, 42_000, "Trần Hoàng Nam",
                865, 50, new BigDecimal("5.00"), null, 830, TransferStatus.IN_PROGRESS);
        payment(camry, "TT-D1A", 50, "TRANSFER", "VCB-DEMO-0001", PaymentStatus.PAID);

        SalesContract cx5 = scenario(2, "Lê Thị Mai", "Mazda", "CX-5 Premium", 2020, 55_000, "Phạm Ngọc Anh",
                785, 50, null, 25, 760, TransferStatus.COMPLETED);
        payment(cx5, "TT-D2A", 50, "TRANSFER", "VCB-DEMO-0002", PaymentStatus.PAID);
        payment(cx5, "TT-D2B", 735, "TRANSFER", "VCB-DEMO-0003", PaymentStatus.PAID);

        SalesContract city = scenario(3, "Nguyễn Quang Huy", "Honda", "City RS", 2022, 18_000, "Bùi Đức Long",
                535, 50, new BigDecimal("6.00"), null, 540, TransferStatus.BLOCKED);
        payment(city, "TT-D3A", 50, "CASH", "PT-DEMO-0004", PaymentStatus.PAID);
        payment(city, "TT-D3B", 485, "PAYOS", "PAYOS-DEMO-0005", PaymentStatus.PENDING);

        log.info("[WP05] Demo data seeded: HDB-D1, HDB-D2, HDB-D3 (dev profile only).");
    }

    private SalesContract scenario(int n, String ownerName, String make, String model, int year, int odo, String buyerName,
                                   int salePriceM, int depositM, BigDecimal commissionRate, Integer fixedFeeM,
                                   int floorM, TransferStatus transferStatus) {
        LocalDate today = LocalDate.now();

        Consignor owner = new Consignor();
        owner.setFullName(ownerName);
        owner.setPhone("09120000" + n + "0");
        owner.setIdNumber("0012030000" + n + "0");
        owner.setBank("Vietcombank");
        owner.setAccountNumber("00710000" + n + "2345");
        owner.setAccountName(ownerName.toUpperCase());
        owner.setConsentStored(true);
        entityManager.persist(owner);

        Vehicle vehicle = new Vehicle();
        vehicle.setVin("WP05DEMOVIN00000" + n);
        vehicle.setMake(make);
        vehicle.setModel(model);
        vehicle.setYear(year);
        vehicle.setOdometerKm(odo);
        vehicle.setLocation("Showroom GaragePro");
        entityManager.persist(vehicle);

        Consignment consignment = new Consignment();
        consignment.setCode("KG-D" + n);
        consignment.setConsignor(owner);
        consignment.setVehicle(vehicle);
        consignment.setStatus(ConsignmentStatus.RESERVED);
        consignment.setExpectedPrice(million(floorM + 20));
        consignment.setIntakeDate(today.minusDays(40));
        consignment.setCustodyLocation("Showroom GaragePro");
        consignment.setOwnerConsent(true);
        entityManager.persist(consignment);

        ConsignmentAgreement agreement = new ConsignmentAgreement();
        agreement.setCode("HDKG-D" + n);
        agreement.setVersionNo(1);
        agreement.setConsignment(consignment);
        agreement.setStatus(AgreementStatus.ACTIVE);
        agreement.setCommissionRate(commissionRate);
        agreement.setFixedFee(fixedFeeM != null ? million(fixedFeeM) : null);
        agreement.setPriceFloor(million(floorM));
        agreement.setValidFrom(today.minusDays(35));
        agreement.setValidTo(today.plusDays(55));
        entityManager.persist(agreement);

        VehicleSaleNews listing = new VehicleSaleNews();
        listing.setCode("TD-D" + n);
        listing.setConsignment(consignment);
        listing.setTitle(make + " " + model + " " + year);
        listing.setListPrice(million(salePriceM + 10));
        listing.setStatus(ListingStatus.LISTED);
        listing.setPublishedAt(Instant.now().minus(30, ChronoUnit.DAYS));
        entityManager.persist(listing);

        Buyer buyer = new Buyer();
        buyer.setFullName(buyerName);
        buyer.setPhone("09880000" + n + "0");
        buyer.setConsentStored(true);
        entityManager.persist(buyer);

        Inquiry inquiry = new Inquiry();
        inquiry.setCode("TV-D" + n);
        inquiry.setBuyer(buyer);
        inquiry.setVehicle(vehicle);
        inquiry.setStatus(InquiryStatus.CONVERTED);
        inquiry.setChannel("Showroom");
        entityManager.persist(inquiry);

        Reservation reservation = new Reservation();
        reservation.setCode("RS-D" + n);
        reservation.setInquiry(inquiry);
        reservation.setVehicle(vehicle);
        reservation.setBuyer(buyer);
        reservation.setStatus(ReservationStatus.CONVERTED);
        reservation.setDepositAmount(million(depositM));
        reservation.setHoldUntil(today.plusDays(7));
        reservation.setConsentStored(true);
        entityManager.persist(reservation);

        SalesContract contract = new SalesContract();
        contract.setCode("HDB-D" + n);
        contract.setReservation(reservation);
        contract.setStatus(ContractStatus.SIGNED);
        contract.setSalePrice(million(salePriceM));
        contract.setDepositAmount(million(depositM));
        contract.setRemainingAmount(million(salePriceM - depositM));
        contract.setSignedOn(today.minusDays(3));
        entityManager.persist(contract);

        TransferCase transfer = new TransferCase();
        transfer.setCode("ST-D" + n);
        transfer.setSalesContract(contract);
        transfer.setStatus(transferStatus);
        transfer.setStage(transferStatus == TransferStatus.COMPLETED ? "Đã sang tên" : "Nộp hồ sơ");
        transfer.setResponsibleParty("Nhân viên hồ sơ");
        transfer.setDeadline(today.plusDays(14));
        transfer.setNotes(transferStatus == TransferStatus.BLOCKED ? "Thiếu giấy ủy quyền của chủ xe." : null);
        entityManager.persist(transfer);
        return contract;
    }

    private void payment(SalesContract contract, String code, int amountM, String method, String reference, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setCode(code);
        payment.setSalesContract(contract);
        payment.setAmount(million(amountM));
        payment.setStatus(status);
        payment.setMethod(method);
        payment.setReferenceCode(reference);
        payment.setPayerName(contract.getReservation().getBuyer().getFullName());
        payment.setPaidAt(Instant.now().minus(2, ChronoUnit.DAYS));
        payment.setEvidencePath(status == PaymentStatus.PAID ? "chung-tu/" + reference + ".pdf" : null);
        entityManager.persist(payment);
    }

    private static BigDecimal million(int value) {
        return BigDecimal.valueOf(value).multiply(MILLION);
    }
}
