package fu.he.training.fall2026.config;

import fu.he.training.fall2026.entity.auth.Role;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.Consignor;
import fu.he.training.fall2026.entity.consignment.TransferCase;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.enums.InquiryStatus;
import fu.he.training.fall2026.entity.enums.ReservationStatus;
import fu.he.training.fall2026.entity.enums.RoleCode;
import fu.he.training.fall2026.entity.enums.TransferStatus;
import fu.he.training.fall2026.entity.sales.Inquiry;
import fu.he.training.fall2026.entity.sales.Reservation;
import fu.he.training.fall2026.entity.sales.SalesContract;
import fu.he.training.fall2026.repository.ConsignmentRepository;
import fu.he.training.fall2026.repository.ConsignorRepository;
import fu.he.training.fall2026.repository.InquiryRepository;
import fu.he.training.fall2026.repository.ReservationRepository;
import fu.he.training.fall2026.repository.RoleRepository;
import fu.he.training.fall2026.repository.SalesContractRepository;
import fu.he.training.fall2026.repository.TransferCaseRepository;
import fu.he.training.fall2026.repository.UserRepository;
import fu.he.training.fall2026.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed chuỗi dữ liệu đủ để test màn Hồ sơ sang tên:
 * Consignor → Vehicle → Consignment(RESERVED) → Inquiry(CONVERTED)
 * → Reservation(CONVERTED) → SalesContract(SIGNED) → TransferCase (các trạng thái).
 * Chạy một lần khi chưa có transfer_cases.
 */
@Slf4j
@Component
@Order(60)
@RequiredArgsConstructor
public class TransferSampleDataLoader implements ApplicationRunner {

    private static final String OTP_PASSWORD_PLACEHOLDER = "{OTP_LOGIN}";

    private final TransferCaseRepository transferCaseRepository;
    private final SalesContractRepository salesContractRepository;
    private final ReservationRepository reservationRepository;
    private final InquiryRepository inquiryRepository;
    private final VehicleRepository vehicleRepository;
    private final ConsignorRepository consignorRepository;
    private final ConsignmentRepository consignmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (transferCaseRepository.count() > 0) {
            return;
        }
        Role docsRole = roleRepository.findByCode(RoleCode.DOCUMENTS).orElse(null);
        Role salesRole = roleRepository.findByCode(RoleCode.SALES).orElse(null);
        if (docsRole == null || salesRole == null) {
            log.warn("[TRANSFER-SEED] Thiếu role DOCUMENTS/SALES — bỏ qua seed.");
            return;
        }

        User sales = ensureUser("sales@garagepro.example", "Demo Sales", "0901000005", salesRole);
        User docs = ensureUser("docs@garagepro.example", "Demo Documents", "0901000008", docsRole);
        User mai = ensureUser("mai.docs@garagepro.example", "Ngọc Mai", "0901000011", docsRole);
        User longUser = ensureUser("long.docs@garagepro.example", "Đức Long", "0901000012", docsRole);
        User khang = ensureUser("khang.docs@garagepro.example", "Phúc Khang", "0901000013", docsRole);

        LocalDate today = LocalDate.now();

        // --- Hồ sơ đã có TransferCase (list KPI: 1 chờ / 2 đang / 4 hoàn tất) ---
        seedDeal(
                "SEED-VIN-ST-208", "Toyota", "Corolla Cross", 2023, "30L-655.02", "KG-001189",
                "Đoàn Quốc Việt", "0916503847", "079123456789", "Q.7, TP.HCM",
                "Trần Văn Chủ", "0902111001", new BigDecimal("765000000"), today.minusDays(10),
                TransferStatus.PENDING, null, today.plusDays(20), null, sales);

        seedDeal(
                "SEED-VIN-ST-207", "Ford", "Everest", 2021, "51H-802.37", "KG-001188",
                "Trương Thị Ngân", "0944278106", "079234567890", "Q.3, TP.HCM",
                "Lê Thị Lan", "0902111002", new BigDecimal("892000000"), today.minusDays(25),
                TransferStatus.IN_PROGRESS, mai, today.plusDays(5), null, sales);

        seedDeal(
                "SEED-VIN-ST-205", "Peugeot", "3008", 2021, "30G-441.90", "KG-001186",
                "Lại Đức Anh", "0963815402", "079345678901", "Hà Đông, Hà Nội",
                "Phạm Văn Bình", "0902111003", new BigDecimal("715000000"), today.minusDays(32),
                TransferStatus.IN_PROGRESS, longUser, today.minusDays(2), null, sales);

        seedDeal(
                "SEED-VIN-ST-206", "Hyundai", "Elantra", 2022, "51K-228.14", "KG-001187",
                "Mạc Thu Hiền", "0908926531", "079456789012", "Thủ Đức, TP.HCM",
                "Ngô Văn An", "0902111004", new BigDecimal("548000000"), today.minusDays(40),
                TransferStatus.COMPLETED, khang, today.minusDays(10), today.minusDays(12), sales);

        seedDeal(
                "SEED-VIN-ST-204", "Hyundai", "Santa Fe", 2021, "30A-991.05", "KG-001185",
                "Vương Gia Huy", "0989137620", "079567890123", "Cầu Giấy, Hà Nội",
                "Đỗ Thị Hoa", "0902111005", new BigDecimal("980000000"), today.minusDays(45),
                TransferStatus.COMPLETED, mai, today.minusDays(15), today.minusDays(18), sales);

        seedDeal(
                "SEED-VIN-ST-203", "Mercedes-Benz", "C200", 2020, "51F-112.88", "KG-001184",
                "Quách Ngọc Trâm", "0932470158", "079678901234", "Q.1, TP.HCM",
                "Hoàng Minh", "0902111006", new BigDecimal("1250000000"), today.minusDays(50),
                TransferStatus.COMPLETED, khang, today.minusDays(20), today.minusDays(22), sales);

        seedDeal(
                "SEED-VIN-ST-201", "Mazda", "6", 2020, "30E-330.21", "KG-001182",
                "Giang Văn Hoàng", "0919382744", "079789012345", "Thanh Xuân, Hà Nội",
                "Vũ Thị Nga", "0902111007", new BigDecimal("690000000"), today.minusDays(55),
                TransferStatus.COMPLETED, docs, today.minusDays(25), today.minusDays(28), sales);

        // --- Ứng viên tạo mới (đủ 4 điều kiện, chưa có TransferCase) ---
        seedContractOnly(
                "SEED-VIN-ST-CAND-1", "Kia", "Carnival", 2022, "51A-777.01", "KG-001200",
                "Hà Minh Quân", "0907341228", "079890123456", "Q.10, TP.HCM",
                "Bùi Văn Chủ", "0902111008", new BigDecimal("1050000000"), today.minusDays(3), sales);

        seedContractOnly(
                "SEED-VIN-ST-CAND-2", "Honda", "Civic", 2022, "30H-555.66", "KG-001201",
                "Nguyễn Thị Bích", "0986114372", "079901234567", "Đống Đa, Hà Nội",
                "Cao Văn Sở", "0902111009", new BigDecimal("620000000"), today.minusDays(5), sales);

        log.info("[TRANSFER-SEED] Đã seed {} hồ sơ sang tên + 2 hợp đồng ứng viên tạo mới.",
                transferCaseRepository.count());
    }

    private void seedDeal(
            String vin,
            String make,
            String model,
            int year,
            String plate,
            String kgCode,
            String buyerName,
            String buyerPhone,
            String buyerId,
            String buyerAddress,
            String ownerName,
            String ownerPhone,
            BigDecimal salePrice,
            LocalDate signedOn,
            TransferStatus status,
            User responsible,
            LocalDate deadline,
            LocalDate completedOn,
            User salesStaff) {
        SalesContract contract = seedContractOnly(
                vin, make, model, year, plate, kgCode,
                buyerName, buyerPhone, buyerId, buyerAddress,
                ownerName, ownerPhone, salePrice, signedOn, salesStaff);

        TransferCase row = new TransferCase();
        row.setCode(String.format("ST-%06d", contract.getId()));
        row.setSalesContract(contract);
        row.setStatus(status);
        row.setDeadline(deadline);
        row.setResponsibleUser(responsible);
        row.setCompletedOn(completedOn);
        if (status == TransferStatus.COMPLETED) {
            row.setEvidenceFileName("dang-ky-xe-moi-" + plate.replace(".", "") + ".pdf");
            row.setEvidencePath("seed/transfers/" + row.getEvidenceFileName());
            row.setNotes("Seed: hoàn tất sang tên mẫu.");
        } else {
            row.setNotes("");
        }
        transferCaseRepository.save(row);
    }

    private SalesContract seedContractOnly(
            String vin,
            String make,
            String model,
            int year,
            String plate,
            String kgCode,
            String buyerName,
            String buyerPhone,
            String buyerId,
            String buyerAddress,
            String ownerName,
            String ownerPhone,
            BigDecimal salePrice,
            LocalDate signedOn,
            User salesStaff) {
        Consignor consignor = new Consignor();
        consignor.setFullName(ownerName);
        consignor.setPhone(ownerPhone);
        consignor.setIdNumber("0" + Math.abs(vin.hashCode() % 1_000_000_000L));
        consignor.setBank("Vietcombank");
        consignor.setAccountNumber("0123456789");
        consignor.setAccountName(ownerName);
        consignor.setConsentStored(true);
        consignorRepository.save(consignor);

        Vehicle vehicle = new Vehicle();
        vehicle.setVin(vin);
        vehicle.setMake(make);
        vehicle.setModel(model);
        vehicle.setYear(year);
        vehicle.setLicensePlate(plate);
        vehicle.setColor("Trắng");
        vehicle.setFuelType("Xăng");
        vehicle.setOdometerKm(25000);
        vehicleRepository.save(vehicle);

        Consignment consignment = new Consignment();
        consignment.setCode(kgCode);
        consignment.setConsignor(consignor);
        consignment.setVehicle(vehicle);
        consignment.setStatus(ConsignmentStatus.RESERVED);
        consignment.setExpectedPrice(salePrice);
        consignment.setIntakeDate(signedOn.minusDays(60));
        consignment.setOwnerConsent(true);
        consignmentRepository.save(consignment);

        Inquiry inquiry = new Inquiry();
        inquiry.setCode("TV-TMP-" + vin);
        inquiry.setVehicle(vehicle);
        inquiry.setAssignedTo(salesStaff);
        inquiry.setStatus(InquiryStatus.CONVERTED);
        inquiry.setChannel("WEBSITE");
        inquiry.setFullName(buyerName);
        inquiry.setPhone(buyerPhone.replace(" ", ""));
        inquiry.setIdNumber(buyerId);
        inquiry.setAddress(buyerAddress);
        inquiry.setConsentStored(true);
        inquiryRepository.save(inquiry);
        inquiry.setCode(String.format("TV-%06d", inquiry.getId()));
        inquiryRepository.save(inquiry);

        Reservation reservation = new Reservation();
        reservation.setCode("DC-TMP-" + vin);
        reservation.setInquiry(inquiry);
        reservation.setVehicle(vehicle);
        reservation.setAssignedTo(salesStaff);
        reservation.setStatus(ReservationStatus.CONVERTED);
        reservation.setDepositAmount(new BigDecimal("50000000"));
        reservation.setHoldUntil(signedOn.plusDays(7));
        reservation.setConsentStored(true);
        reservationRepository.save(reservation);
        reservation.setCode(String.format("DC-%06d", reservation.getId()));
        reservationRepository.save(reservation);

        SalesContract contract = new SalesContract();
        contract.setCode("HDBX-TMP-" + vin);
        contract.setReservation(reservation);
        contract.setStatus(ContractStatus.SIGNED);
        contract.setSalePrice(salePrice);
        contract.setDepositAmount(new BigDecimal("50000000"));
        contract.setRemainingAmount(salePrice.subtract(new BigDecimal("50000000")));
        contract.setSignedOn(signedOn);
        contract.setSaleClosed(false);
        salesContractRepository.save(contract);
        contract.setCode(String.format("HDBX-%06d", contract.getId()));
        salesContractRepository.save(contract);
        return contract;
    }

    private User ensureUser(String email, String fullName, String phone, Role role) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setFullName(fullName);
            user.setPhone(phone);
            user.setPasswordHash(OTP_PASSWORD_PLACEHOLDER);
            user.setEmailVerified(false);
            user.setActive(true);
            user.setRole(role);
            return userRepository.save(user);
        });
    }
}
