package fu.he.training.fall2026.config;

import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.ConsignmentAgreement;
import fu.he.training.fall2026.entity.consignment.Consignor;
import fu.he.training.fall2026.entity.consignment.Inspection;
import fu.he.training.fall2026.entity.consignment.TransferCase;
import fu.he.training.fall2026.entity.consignment.Valuation;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.consignment.VehicleSaleNews;
import fu.he.training.fall2026.entity.enums.AgreementStatus;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.enums.HandoverStatus;
import fu.he.training.fall2026.entity.enums.InquiryStatus;
import fu.he.training.fall2026.entity.enums.InspectionStatus;
import fu.he.training.fall2026.entity.enums.ListingStatus;
import fu.he.training.fall2026.entity.enums.PaymentStatus;
import fu.he.training.fall2026.entity.enums.ReservationStatus;
import fu.he.training.fall2026.entity.enums.SettlementStatus;
import fu.he.training.fall2026.entity.enums.TransferStatus;
import fu.he.training.fall2026.entity.enums.ViewingStatus;
import fu.he.training.fall2026.entity.payment.Payment;
import fu.he.training.fall2026.entity.payment.PaymentRequest;
import fu.he.training.fall2026.entity.payment.Settlement;
import fu.he.training.fall2026.entity.sales.Handover;
import fu.he.training.fall2026.entity.sales.Inquiry;
import fu.he.training.fall2026.entity.sales.Reservation;
import fu.he.training.fall2026.entity.sales.SalesContract;
import fu.he.training.fall2026.entity.sales.Viewing;
import fu.he.training.fall2026.repository.ConsignmentAgreementRepository;
import fu.he.training.fall2026.repository.ConsignmentRepository;
import fu.he.training.fall2026.repository.ConsignorRepository;
import fu.he.training.fall2026.repository.HandoverRepository;
import fu.he.training.fall2026.repository.InquiryRepository;
import fu.he.training.fall2026.repository.InspectionRepository;
import fu.he.training.fall2026.repository.PaymentRepository;
import fu.he.training.fall2026.repository.PaymentRequestRepository;
import fu.he.training.fall2026.repository.ReservationRepository;
import fu.he.training.fall2026.repository.SalesContractRepository;
import fu.he.training.fall2026.repository.SettlementRepository;
import fu.he.training.fall2026.repository.TransferCaseRepository;
import fu.he.training.fall2026.repository.UserRepository;
import fu.he.training.fall2026.repository.ValuationRepository;
import fu.he.training.fall2026.repository.VehicleRepository;
import fu.he.training.fall2026.repository.VehicleSaleNewsRepository;
import fu.he.training.fall2026.repository.ViewingRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Một bộ dữ liệu mẫu cho cả luồng ký gửi → kiểm định → đăng tin → tư vấn → cọc → hợp đồng
 * → thu tiền → sang tên → bàn giao. Chạy một lần khi chưa có xe GP-VIN-01.
 */
@Slf4j
@Component
@Order(30)
@RequiredArgsConstructor
public class GarageProDataSeed implements ApplicationRunner {

    private static final String MARKER_VIN = "GP-VIN-01";

    private final UserRepository userRepository;
    private final ConsignorRepository consignorRepository;
    private final VehicleRepository vehicleRepository;
    private final ConsignmentRepository consignmentRepository;
    private final ConsignmentAgreementRepository agreementRepository;
    private final InspectionRepository inspectionRepository;
    private final ValuationRepository valuationRepository;
    private final VehicleSaleNewsRepository newsRepository;
    private final InquiryRepository inquiryRepository;
    private final ViewingRepository viewingRepository;
    private final ReservationRepository reservationRepository;
    private final SalesContractRepository salesContractRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final TransferCaseRepository transferCaseRepository;
    private final HandoverRepository handoverRepository;
    private final SettlementRepository settlementRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (vehicleRepository.existsByVinIgnoreCase(MARKER_VIN)) {
            return;
        }
        // Mã mẫu đánh cố định từ 1 (KG-000001, TV-000001...), nên chỉ nạp được vào DB chưa có dữ liệu nghiệp vụ.
        // DB đã có hồ sơ ký gửi thì bỏ qua để không trùng mã và không làm app dừng lúc khởi động.
        if (consignmentRepository.count() > 0) {
            log.info("[SEED] DB đã có dữ liệu ký gửi, bỏ qua bộ dữ liệu mẫu (chỉ nạp vào DB chưa có dữ liệu).");
            return;
        }
        User sales = user("lanh@gmail.com");
        User inspector = user("annvt@gmail.com");
        User finance = user("cuongvm@gmail.com");
        User handoverStaff = user("cuongvmhandover@gmail.com");
        User docs = user("khangnp@gmail.com");
        if (sales == null || inspector == null || finance == null || handoverStaff == null || docs == null) {
            log.warn("[SEED] Chưa có tài khoản nhóm, bỏ qua dữ liệu nghiệp vụ.");
            return;
        }

        Car[] cars = cars();
        Consignment[] files = new Consignment[cars.length];
        ConsignmentAgreement[] agreements = new ConsignmentAgreement[cars.length];
        for (int i = 0; i < cars.length; i++) {
            Consignor owner = owner(i, cars[i]);
            Vehicle vehicle = vehicle(i, cars[i]);
            files[i] = consignment(i, owner, vehicle, cars[i]);
            agreements[i] = agreement(i, files[i], cars[i]);
            if (cars[i].inspected) {
                inspection(i, files[i], vehicle);
                valuation(i, files[i], cars[i]);
            }
            if (cars[i].listing != null) {
                news(i, files[i], cars[i], inspector);
            }
        }

        seedInquiries(files, cars, sales);
        seedDeals(files, agreements, cars, sales, finance, docs, handoverStaff);
        log.info("[SEED] Đã tạo 10 hồ sơ ký gửi, tin đăng và luồng bán mẫu.");
    }

    private void seedInquiries(Consignment[] files, Car[] cars, User sales) {
        int n = 0;
        for (int car = 0; car < cars.length; car++) {
            if (cars[car].status == ConsignmentStatus.RESERVED || cars[car].status == ConsignmentStatus.COMPLETED) {
                inquiry(n, car, files, cars, sales, true);
                n++;
            }
        }
        for (int car = 0; car < cars.length && n < 10; car++) {
            if (cars[car].listing != ListingStatus.LISTED) {
                continue;
            }
            while (n < 10 && inquiriesOn(files[car]) < 3) {
                inquiry(n, car, files, cars, sales, false);
                n++;
            }
        }
    }

    private int inquiriesOn(Consignment file) {
        return (int) inquiryRepository.findAll().stream()
                .filter(i -> i.getVehicle().getId().equals(file.getVehicle().getId()))
                .count();
    }

    private void inquiry(int n, int car, Consignment[] files, Car[] cars, User sales, boolean closedDeal) {
        String[] names = {
            "Nguyễn Minh Khang", "Trần Gia Hân", "Lê Quốc Bảo", "Phạm Thu Hà", "Hoàng Đức Anh",
            "Võ Ngọc Lan", "Đặng Thanh Tùng", "Bùi Khánh Linh", "Đỗ Hải Nam", "Ngô Phương Thảo"
        };
        String[] phones = {
            "0912345601", "0988123402", "0903456703", "0936789404", "0977123405",
            "0918654306", "0909123407", "0944567808", "0966234509", "0922345610"
        };
        if (n >= names.length) {
            return;
        }
        Inquiry inquiry = new Inquiry();
        inquiry.setCode(String.format("TV-%06d", n + 1));
        inquiry.setVehicle(files[car].getVehicle());
        inquiry.setAssignedTo(sales);
        inquiry.setStatus(closedDeal ? InquiryStatus.CONVERTED : (n % 4 == 0 ? InquiryStatus.SCHEDULED : InquiryStatus.OPEN));
        inquiry.setChannel(n % 2 == 0 ? "WEBSITE" : "PHONE");
        inquiry.setFullName(names[n]);
        inquiry.setPhone(phones[n]);
        inquiry.setEmail("khach" + (n + 1) + "@gmail.com");
        inquiry.setNeed(n % 3 == 0 ? "VIEWING" : "PRICE");
        inquiry.setAddress("Quận " + (n + 1) + ", TP. Hồ Chí Minh");
        inquiry.setNotes("Khách hỏi xe " + cars[car].title + ", tiện nghe máy sau 18h.");
        inquiry.setConsentStored(true);
        inquiryRepository.save(inquiry);
        if (!closedDeal) {
            viewing(n, inquiry, cars[car]);
        }
    }

    private void viewing(int n, Inquiry inquiry, Car car) {
        Viewing viewing = new Viewing();
        viewing.setCode(String.format("LX-%06d", n + 1));
        viewing.setInquiry(inquiry);
        viewing.setScheduledAt(Instant.now().plus(n + 1L, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS));
        viewing.setDurationMinutes(n % 2 == 0 ? 45 : 60);
        viewing.setStatus(n % 5 == 0 ? ViewingStatus.DONE : (n % 2 == 0 ? ViewingStatus.CONFIRMED : ViewingStatus.BOOKED));
        viewing.setViewingType("VIEWING");
        viewing.setLocation("Showroom GaragePro, Khu Công nghệ cao, TP. Thủ Đức");
        viewing.setNotes("Xem " + car.title + " tại showroom.");
        viewingRepository.save(viewing);
    }

    private void seedDeals(
            Consignment[] files, ConsignmentAgreement[] agreements, Car[] cars,
            User sales, User finance, User docs, User handoverStaff) {
        int reserved = indexOf(cars, ConsignmentStatus.RESERVED);
        int done = indexOf(cars, ConsignmentStatus.COMPLETED);
        if (reserved >= 0) {
            Inquiry buyer = inquiryRepository.findAll().stream()
                    .filter(i -> i.getVehicle().getId().equals(files[reserved].getVehicle().getId()))
                    .findFirst().orElseThrow();
            Reservation hold = reservation(1, files[reserved], buyer, sales, ReservationStatus.ACTIVE, 50_000_000L, 7);
            payment(1, null, hold, buyer.getFullName(), 50_000_000L, PaymentStatus.PAID, finance.getFullName());
        }
        if (done >= 0) {
            deal(3, files[done], agreements[done], sales, finance, docs, handoverStaff, true);
        }
    }

    private void deal(
            int n, Consignment file, ConsignmentAgreement agreement,
            User sales, User finance, User docs, User handoverStaff, boolean finished) {
        Inquiry buyer = inquiryRepository.findAll().stream()
                .filter(i -> i.getVehicle().getId().equals(file.getVehicle().getId()))
                .findFirst().orElseThrow();
        long price = file.getExpectedPrice().longValue();
        long deposit = 80_000_000L;
        Reservation hold = reservation(n, file, buyer, sales, ReservationStatus.CONVERTED, deposit, -2);
        SalesContract contract = new SalesContract();
        contract.setCode(String.format("HDBX-%06d", n));
        contract.setReservation(hold);
        contract.setStatus(finished ? ContractStatus.COMPLETED : ContractStatus.ACTIVE);
        contract.setSalePrice(BigDecimal.valueOf(price));
        contract.setDepositAmount(BigDecimal.valueOf(deposit));
        contract.setRemainingAmount(BigDecimal.valueOf(price - deposit));
        contract.setSignedOn(LocalDate.now().minusDays(5));
        contract.setPaymentTerms("Khách thanh toán phần còn lại trước khi nhận xe.");
        contract.setHandoverTerms("Bàn giao tại showroom sau khi sang tên xong.");
        contract.setTerminationTerms("Hủy sau khi ký thì xử lý cọc theo phiếu đặt cọc.");
        contract.setSaleClosed(finished);
        salesContractRepository.save(contract);

        payment(n * 10, contract, hold, buyer.getFullName(), deposit, PaymentStatus.PAID, finance.getFullName());
        PaymentRequest request = new PaymentRequest();
        request.setCode(String.format("PT-%06d", n));
        request.setSalesContract(contract);
        request.setAmount(BigDecimal.valueOf(price - deposit));
        request.setPurpose("Thanh toán hợp đồng " + contract.getCode());
        request.setStatus(finished ? PaymentStatus.PAID : PaymentStatus.PARTIAL);
        paymentRequestRepository.save(request);
        if (finished) {
            payment(n * 10 + 1, contract, null, buyer.getFullName(), price - deposit, PaymentStatus.PAID, finance.getFullName());
        }

        TransferCase transfer = new TransferCase();
        transfer.setCode(String.format("ST-%06d", n));
        transfer.setSalesContract(contract);
        transfer.setStatus(finished ? TransferStatus.COMPLETED : TransferStatus.IN_PROGRESS);
        transfer.setStage(finished ? "Đã sang tên" : "Nộp hồ sơ");
        transfer.setResponsibleUser(docs);
        transfer.setDeadline(LocalDate.now().plusDays(finished ? -1 : 10));
        transfer.setCompletedOn(finished ? LocalDate.now().minusDays(1) : null);
        transfer.setNotes("Sang tên " + file.getCode() + " cho " + buyer.getFullName());
        transferCaseRepository.save(transfer);

        Handover handover = new Handover();
        handover.setCode(String.format("BG-%06d", n));
        handover.setSalesContract(contract);
        handover.setStatus(finished ? HandoverStatus.FINALIZED : HandoverStatus.READY);
        handover.setOdometerKm(file.getVehicle().getOdometerKm());
        handover.setKeyCount(finished ? 2 : null);
        handover.setReceiptIssued(finished);
        handover.setSaleCloseChecked(finished);
        handover.setHandedOverBy(finished ? handoverStaff : null);
        handover.setHandedOverAt(finished ? Instant.now().minus(1, ChronoUnit.DAYS) : null);
        handover.setNotes(finished ? "Đã giao xe và 2 chìa khóa." : "Chờ khách đến nhận xe.");
        handoverRepository.save(handover);

        if (finished && agreement != null) {
            Settlement settlement = new Settlement();
            settlement.setCode(String.format("QT-%06d", n));
            settlement.setSalesContract(contract);
            settlement.setConsignmentAgreement(agreement);
            settlement.setStatus(SettlementStatus.PENDING_REVIEW);
            BigDecimal commission = BigDecimal.valueOf(price).multiply(new BigDecimal("0.015"));
            settlement.setBaseAmount(BigDecimal.valueOf(price));
            settlement.setCommissionAmount(commission);
            settlement.setFeeAmount(BigDecimal.ZERO);
            settlement.setApprovedCostAmount(BigDecimal.ZERO);
            settlement.setRefundHeldAmount(BigDecimal.ZERO);
            settlement.setDepositHeldAmount(BigDecimal.valueOf(deposit));
            settlement.setNetPayoutAmount(BigDecimal.valueOf(price).subtract(commission));
            settlement.setNotes("Chờ Manager duyệt chi cho chủ xe.");
            settlementRepository.save(settlement);
        }
    }

    private Reservation reservation(
            int n, Consignment file, Inquiry buyer, User sales,
            ReservationStatus status, long deposit, int holdDays) {
        Reservation reservation = new Reservation();
        reservation.setCode(String.format("DC-%06d", n));
        reservation.setInquiry(buyer);
        reservation.setVehicle(file.getVehicle());
        reservation.setAssignedTo(sales);
        reservation.setStatus(status);
        reservation.setDepositAmount(BigDecimal.valueOf(deposit));
        reservation.setHoldUntil(LocalDate.now().plusDays(holdDays));
        reservation.setTerms("Giữ xe đến hết hạn, tiền cọc trừ vào giá bán.");
        reservation.setConsentStored(true);
        return reservationRepository.save(reservation);
    }

    private void payment(
            int n, SalesContract contract, Reservation reservation, String payer,
            long amount, PaymentStatus status, String staff) {
        Payment payment = new Payment();
        payment.setCode(String.format("TT-%06d", n));
        payment.setSalesContract(contract);
        payment.setReservation(reservation);
        payment.setAmount(BigDecimal.valueOf(amount));
        payment.setStatus(status);
        payment.setPayerName(payer);
        payment.setReferenceCode("VCB" + (100000 + n));
        payment.setMethod("Chuyển khoản");
        payment.setPaidAt(status == PaymentStatus.PAID ? Instant.now().minus(n, ChronoUnit.HOURS) : null);
        payment.setRecordedByName(staff);
        paymentRepository.save(payment);
    }

    private Consignor owner(int i, Car car) {
        Consignor owner = new Consignor();
        owner.setFullName(car.owner);
        owner.setPhone("0903" + String.format("%06d", 110000 + i));
        owner.setEmail("chuxe" + (i + 1) + "@gmail.com");
        owner.setIdNumber("079" + String.format("%09d", 200000000 + i));
        owner.setAddress(car.area);
        owner.setBank("Vietcombank");
        owner.setAccountNumber("007100" + String.format("%06d", 420000 + i));
        owner.setAccountName(car.owner.toUpperCase());
        owner.setConsentStored(true);
        return consignorRepository.save(owner);
    }

    private Vehicle vehicle(int i, Car car) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVin(car.vin);
        vehicle.setMake(car.make);
        vehicle.setModel(car.model);
        vehicle.setYear(car.year);
        vehicle.setOdometerKm(car.km);
        vehicle.setColor(car.color);
        vehicle.setLicensePlate(car.plate);
        vehicle.setFuelType(car.fuel);
        vehicle.setTransmission("Tự động");
        vehicle.setVehicleType(car.type);
        vehicle.setLocation("Showroom GaragePro, TP. Thủ Đức");
        vehicle.setAskingPrice(BigDecimal.valueOf(car.price));
        vehicle.setIntakeCondition("Xe " + car.title + ", nội thất sạch, không ngập nước.");
        return vehicleRepository.save(vehicle);
    }

    private Consignment consignment(int i, Consignor owner, Vehicle vehicle, Car car) {
        Consignment file = new Consignment();
        file.setCode(String.format("KG-%06d", i + 1));
        file.setConsignor(owner);
        file.setVehicle(vehicle);
        file.setStatus(car.status);
        file.setExpectedPrice(BigDecimal.valueOf(car.price));
        file.setIntakeDate(LocalDate.now().minusDays(20L - i));
        file.setCustodyLocation("Kho xe đã kiểm tra, Thủ Đức");
        file.setOwnerConsent(true);
        file.setTermNotes("Ký gửi " + car.title + " trong 60 ngày.");
        return consignmentRepository.save(file);
    }

    private ConsignmentAgreement agreement(int i, Consignment file, Car car) {
        ConsignmentAgreement agreement = new ConsignmentAgreement();
        agreement.setCode(String.format("HDKG-%06d", i + 1));
        agreement.setConsignment(file);
        agreement.setStatus(car.agreement);
        agreement.setCommissionRate(new BigDecimal("1.5000"));
        agreement.setFixedFee(BigDecimal.ZERO);
        agreement.setPriceFloor(BigDecimal.valueOf(car.price));
        agreement.setValidFrom(file.getIntakeDate());
        agreement.setValidTo(file.getIntakeDate().plusDays(60));
        agreement.setCustodyTerms("Bên nhận ký gửi giữ xe tại showroom, không cho mượn lái ngoài lịch hẹn.");
        agreement.setTerminationTerms("Chủ xe rút xe trước hạn phải báo trước 3 ngày.");
        agreement.setPayoutTerms("Chi tiền cho chủ xe sau khi bán và trừ hoa hồng 1,5%.");
        return agreementRepository.save(agreement);
    }

    private void inspection(int i, Consignment file, Vehicle vehicle) {
        Inspection inspection = new Inspection();
        inspection.setCode(String.format("KD-%06d", i + 1));
        inspection.setVehicle(vehicle);
        inspection.setConsignment(file);
        inspection.setStatus(InspectionStatus.COMPLETED);
        inspection.setOverallResult("PASS");
        inspection.setInspectedOn(LocalDate.now().minusDays(8L - (i % 5)));
        inspection.setNotes("Khung gầm, động cơ và nội thất " + vehicle.getMake() + " " + vehicle.getModel() + " đạt.");
        inspectionRepository.save(inspection);
    }

    private void valuation(int i, Consignment file, Car car) {
        Valuation valuation = new Valuation();
        valuation.setCode(String.format("DG-%06d", i + 1));
        valuation.setConsignment(file);
        valuation.setMarketReference(BigDecimal.valueOf(car.price + 20_000_000L));
        valuation.setFloorPrice(BigDecimal.valueOf(car.price - 30_000_000L));
        valuation.setProposedPrice(BigDecimal.valueOf(car.price));
        valuation.setValuedOn(LocalDate.now().minusDays(7));
        valuation.setRationale("Đối chiếu tin " + car.title + " cùng năm, cùng số km trên thị trường TP.HCM.");
        valuationRepository.save(valuation);
    }

    private void news(int i, Consignment file, Car car, User inspector) {
        VehicleSaleNews news = new VehicleSaleNews();
        news.setCode(String.format("TD-%06d", i + 1));
        news.setConsignment(file);
        news.setAssignedTo(inspector);
        news.setTitle(car.title);
        news.setBody(car.title + " " + car.year + ", " + car.color + ", " + car.km + " km, " + car.fuel
                + ". Xe một chủ, bảo dưỡng đúng hạn, xem xe tại showroom Thủ Đức.");
        news.setListPrice(BigDecimal.valueOf(car.price));
        news.setStatus(car.listing);
        news.setImagePaths(car.image);
        news.setPublishedAt(Instant.now().minus(10L - i, ChronoUnit.DAYS));
        newsRepository.save(news);
    }

    private User user(String email) {
        return userRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    private static int indexOf(Car[] cars, ConsignmentStatus status) {
        for (int i = 0; i < cars.length; i++) {
            if (cars[i].status == status) {
                return i;
            }
        }
        return -1;
    }

    private static Car[] cars() {
        return new Car[] {
            car("GP-VIN-01", "Toyota", "Vios", 2022, 28500, "Trắng", "Xăng", "Sedan", "51A-128.36",
                    "Phường 12, Quận Bình Thạnh", "Lê Hoàng Nam", 538_000_000L,
                    ConsignmentStatus.DRAFT, AgreementStatus.DRAFT, null, false,
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/2/25/Toyota_Vios_%28XP150%29_sedan_front_view.jpg/1280px-Toyota_Vios_%28XP150%29_sedan_front_view.jpg"),
            car("GP-VIN-02", "Toyota", "Camry", 2021, 41200, "Đen", "Xăng", "Sedan", "30G-552.18",
                    "Phường Thảo Điền, TP. Thủ Đức", "Trần Thị Mai", 920_000_000L,
                    ConsignmentStatus.SUBMITTED, AgreementStatus.PENDING_SIGNATURE, null, false,
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ef/Toyota_Camry_%28XV70%29_IMG_9081.jpg/1280px-Toyota_Camry_%28XV70%29_IMG_9081.jpg"),
            car("GP-VIN-03", "Honda", "CR-V", 2021, 31200, "Trắng", "Xăng", "SUV", "30G-678.90",
                    "Phường 7, Quận 3", "Nguyễn Văn Khôi", 868_000_000L,
                    ConsignmentStatus.ACTIVE, AgreementStatus.ACTIVE, null, false,
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c3/2018_Honda_CR-V_%28RW_MY18%29_%2BSport_2WD_wagon_%282018-10-22%29_01.jpg/1280px-2018_Honda_CR-V_%28RW_MY18%29_%2BSport_2WD_wagon_%282018-10-22%29_01.jpg"),
            car("GP-VIN-04", "Hyundai", "Tucson", 2022, 21000, "Xám", "Xăng", "SUV", "51K-246.80",
                    "Phường An Phú, TP. Thủ Đức", "Phạm Quốc Huy", 835_000_000L,
                    ConsignmentStatus.INSPECTION_RECEIVED, AgreementStatus.ACTIVE, null, false,
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f1/Hyundai_Tucson_%28NX4%29_1X7A0424.jpg/1280px-Hyundai_Tucson_%28NX4%29_1X7A0424.jpg"),
            car("GP-VIN-05", "Mazda", "CX-5", 2023, 9800, "Đỏ", "Xăng", "SUV", "51H-246.80",
                    "Phường 25, Quận Bình Thạnh", "Ngô Thanh Hà", 865_000_000L,
                    ConsignmentStatus.INSPECTED, AgreementStatus.ACTIVE, null, true,
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f8/Mazda_CX-5_%28KF%29_Facelift_1X7A0331_%282%29.jpg/1280px-Mazda_CX-5_%28KF%29_Facelift_1X7A0331_%282%29.jpg"),
            car("GP-VIN-06", "Honda", "Civic", 2022, 25600, "Xanh", "Xăng", "Sedan", "30H-555.66",
                    "Phường Bến Nghé, Quận 1", "Lý Minh Châu", 735_000_000L,
                    ConsignmentStatus.LISTED, AgreementStatus.ACTIVE, ListingStatus.LISTED, true,
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/4/48/Honda_Civic_%281Y7A1724%29.jpg/1280px-Honda_Civic_%281Y7A1724%29.jpg"),
            car("GP-VIN-07", "Mazda", "6", 2020, 54800, "Đỏ", "Xăng", "Sedan", "30E-330.21",
                    "Phường 15, Quận 10", "Đinh Công Vinh", 655_000_000L,
                    ConsignmentStatus.LISTED, AgreementStatus.ACTIVE, ListingStatus.LISTED, true,
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/9/90/2018-2019_Mazda_Atenza_Wagon_XD.jpg/1280px-2018-2019_Mazda_Atenza_Wagon_XD.jpg"),
            car("GP-VIN-08", "Hyundai", "Santa Fe", 2021, 38900, "Trắng", "Dầu", "SUV", "30A-991.05",
                    "Phường Hiệp Bình Chánh, TP. Thủ Đức", "Vũ Thị Hồng", 980_000_000L,
                    ConsignmentStatus.LISTED, AgreementStatus.ACTIVE, ListingStatus.LISTED, true,
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Hyundai_Santa_Fe%2C_GIMS_2018%2C_Le_Grand-Saconnex_%281X7A1735%29.jpg/1280px-Hyundai_Santa_Fe%2C_GIMS_2018%2C_Le_Grand-Saconnex_%281X7A1735%29.jpg"),
            car("GP-VIN-09", "Kia", "Carnival", 2022, 27400, "Đen", "Dầu", "MPV", "51A-777.01",
                    "Phường Tân Phong, Quận 7", "Trịnh Gia Bảo", 1_150_000_000L,
                    ConsignmentStatus.RESERVED, AgreementStatus.ACTIVE, ListingStatus.RESERVED, true,
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1b/KIA_CARNIVAL_%28YP%29_China.jpg/1280px-KIA_CARNIVAL_%28YP%29_China.jpg"),
            car("GP-VIN-10", "Ford", "Everest", 2021, 46300, "Bạc", "Dầu", "SUV", "51H-802.37",
                    "Phường Thạnh Mỹ Lợi, TP. Thủ Đức", "Phan Đức Trọng", 968_000_000L,
                    ConsignmentStatus.COMPLETED, AgreementStatus.ACTIVE, ListingStatus.SOLD, true,
                    "https://upload.wikimedia.org/wikipedia/commons/thumb/7/79/FORD_EVEREST_%28U375%2CUA%29_China_%282%29.jpg/1280px-FORD_EVEREST_%28U375%2CUA%29_China_%282%29.jpg")
        };
    }

    private static Car car(
            String vin, String make, String model, int year, int km, String color, String fuel, String type,
            String plate, String area, String owner, long price, ConsignmentStatus status,
            AgreementStatus agreement, ListingStatus listing, boolean inspected, String image) {
        Car row = new Car();
        row.vin = vin;
        row.make = make;
        row.model = model;
        row.year = year;
        row.km = km;
        row.color = color;
        row.fuel = fuel;
        row.type = type;
        row.plate = plate;
        row.area = area;
        row.owner = owner;
        row.price = price;
        row.status = status;
        row.agreement = agreement;
        row.listing = listing;
        row.inspected = inspected;
        row.image = image;
        row.title = make + " " + model + " " + year;
        return row;
    }

    private static final class Car {
        private String vin;
        private String make;
        private String model;
        private String title;
        private int year;
        private int km;
        private String color;
        private String fuel;
        private String type;
        private String plate;
        private String area;
        private String owner;
        private long price;
        private ConsignmentStatus status;
        private AgreementStatus agreement;
        private ListingStatus listing;
        private boolean inspected;
        private String image;
    }
}
