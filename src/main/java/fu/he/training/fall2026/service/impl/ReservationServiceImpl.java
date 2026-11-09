package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import fu.he.training.fall2026.constant.sales.SalesDealCatalog;
import fu.he.training.fall2026.constant.sales.SalesInquiryCatalog;
import fu.he.training.fall2026.dto.sales.DealListFilter;
import fu.he.training.fall2026.dto.sales.ReservationForm;
import fu.he.training.fall2026.dto.sales.ReservationResponse;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.Consignor;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.entity.enums.InquiryStatus;
import fu.he.training.fall2026.entity.enums.ListingStatus;
import fu.he.training.fall2026.entity.enums.ReservationStatus;
import fu.he.training.fall2026.entity.sales.Inquiry;
import fu.he.training.fall2026.entity.sales.Reservation;
import fu.he.training.fall2026.entity.sales.SalesContract;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.ConsignmentRepository;
import fu.he.training.fall2026.repository.InquiryRepository;
import fu.he.training.fall2026.repository.ReservationRepository;
import fu.he.training.fall2026.repository.SalesContractRepository;
import fu.he.training.fall2026.repository.UserRepository;
import fu.he.training.fall2026.repository.VehicleSaleNewsRepository;
import fu.he.training.fall2026.service.ReservationService;
import fu.he.training.fall2026.service.SalesInquiryService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<InquiryStatus> HOLDABLE =
            Set.of(InquiryStatus.OPEN, InquiryStatus.SCHEDULED, InquiryStatus.VIEWED);

    private final ReservationRepository reservationRepository;
    private final SalesContractRepository salesContractRepository;
    private final InquiryRepository inquiryRepository;
    private final ConsignmentRepository consignmentRepository;
    private final VehicleSaleNewsRepository newsRepository;
    private final UserRepository userRepository;
    private final SalesInquiryService salesInquiryService;

    @Override
    @Transactional
    public void bindList(Model model, String base, DealListFilter filter) {
        expireOverdue();
        List<ReservationResponse> all = reservationRepository.findAllDetail().stream()
                .map(this::toDto)
                .toList();
        bindFiltered(model, base, all, "deposit", "holdUntil", SalesDealCatalog.HOLD_STATUSES, filter);
        model.addAttribute("contractIds", contractIdsByReservation());
    }

    @Override
    @Transactional
    public Page<ReservationResponse> search(DealListFilter filter) {
        expireOverdue();
        List<ReservationResponse> all = reservationRepository.findAllDetail().stream()
                .map(this::toDto)
                .toList();
        return filterPage(all, "deposit", "holdUntil", filter);
    }

    @Override
    @Transactional
    public Map<String, Long> countByStatus() {
        expireOverdue();
        Map<String, Long> counts = new LinkedHashMap<>();
        SalesDealCatalog.HOLD_STATUSES.keySet().forEach(k -> counts.put(k, 0L));
        reservationRepository.findAll().forEach(r ->
                counts.merge(r.getStatus().name(), 1L, Long::sum));
        return counts;
    }

    @Override
    @Transactional
    public ReservationResponse findById(Long id) {
        expireOverdue();
        return toDto(require(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Map<String, Object>> availableCars() {
        Map<String, Map<String, Object>> cars = new LinkedHashMap<>();
        newsRepository
                .searchPublic(
                        "%",
                        ListingStatus.LISTED,
                        null,
                        null,
                        null,
                        PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "id")))
                .forEach(n -> {
                    Vehicle v = n.getConsignment().getVehicle();
                    if (isVehicleHeld(v.getId())) {
                        return;
                    }
                    Consignment consignment = consignmentRepository
                            .findByVehicleIdOrderByIdDesc(v.getId())
                            .stream()
                            .findFirst()
                            .orElse(n.getConsignment());
                    if (consignment == null || consignment.getStatus() != ConsignmentStatus.LISTED) {
                        return;
                    }
                    Consignor owner = consignment.getConsignor();
                    Map<String, Object> row = new LinkedHashMap<>();
                    long price = n.getListPrice() != null
                            ? n.getListPrice().longValue()
                            : (v.getAskingPrice() == null ? 0L : v.getAskingPrice().longValue());
                    row.put("price", price);
                    row.put("km", v.getOdometerKm() == null ? 0 : v.getOdometerKm());
                    row.put("fuel", nullToEmpty(v.getFuelType()));
                    row.put("color", nullToEmpty(v.getColor()));
                    row.put("file", consignment == null ? "" : nullToEmpty(consignment.getCode()));
                    row.put("plate", nullToEmpty(v.getLicensePlate()));
                    row.put("owner", owner == null ? "" : nullToEmpty(owner.getFullName()));
                    row.put("vin", nullToEmpty(v.getVin()));
                    row.put("title", carTitle(v));
                    cars.put(String.valueOf(v.getId()), row);
                });
        return cars;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> holdableBuyers(Long vehicleId) {
        if (vehicleId == null) {
            return List.of();
        }
        return inquiryRepository.findHoldableByVehicle(vehicleId, new ArrayList<>(HOLDABLE)).stream()
                .filter(i -> !reservationRepository.existsByInquiryIdAndStatus(i.getId(), ReservationStatus.ACTIVE))
                .map(this::buyerCard)
                .toList();
    }

    @Override
    public Long create(ReservationForm form, Long staffUserId) {
        Long vehicleId = parseId(form.getCar());
        Long inquiryId = parseId(form.getInquiryId());
        if (vehicleId == null || inquiryId == null) {
            throw new BusinessException("Vui lòng chọn xe và khách hàng.", HttpStatus.BAD_REQUEST);
        }
        Inquiry inquiry = inquiryRepository.findDetailById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu tư vấn", inquiryId));
        if (!HOLDABLE.contains(inquiry.getStatus())) {
            throw new BusinessException("Tư vấn này đã kết thúc nên không đặt cọc được.", HttpStatus.CONFLICT);
        }
        if (!vehicleId.equals(inquiry.getVehicle().getId())) {
            throw new BusinessException("Vui lòng chọn khách hàng đang được tư vấn xe này.", HttpStatus.BAD_REQUEST);
        }
        if (!salesInquiryService.canHold(inquiryId)) {
            throw new BusinessException("Tư vấn này đã có phiếu đặt cọc đang hiệu lực.", HttpStatus.CONFLICT);
        }
        if (isVehicleHeld(vehicleId)) {
            throw new BusinessException("Xe này đã được giữ hoặc đã bán, vui lòng chọn xe khác.", HttpStatus.CONFLICT);
        }

        Map<String, Map<String, Object>> cars = availableCars();
        Map<String, Object> car = cars.get(String.valueOf(vehicleId));
        if (car == null) {
            throw new BusinessException("Xe này đã được giữ hoặc đã bán, vui lòng chọn xe khác.", HttpStatus.CONFLICT);
        }
        long price = ((Number) car.get("price")).longValue();
        Long deposit = parseMoney(form.getDeposit());
        LocalDate holdUntil = parseDate(form.getHoldUntil());
        String depositError = checkDeposit(deposit, holdUntil, price);
        if (depositError != null) {
            throw new BusinessException(depositError, HttpStatus.BAD_REQUEST);
        }
        if (form.getConsent() == null) {
            throw new BusinessException("Khách cần đồng ý lưu thông tin và điều khoản giữ xe.", HttpStatus.BAD_REQUEST);
        }

        Reservation reservation = new Reservation();
        reservation.setInquiry(inquiry);
        reservation.setVehicle(inquiry.getVehicle());
        reservation.setAssignedTo(requireUser(staffUserId));
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setDepositAmount(BigDecimal.valueOf(deposit));
        reservation.setHoldUntil(holdUntil);
        reservation.setTerms(blankTo(form.getTerms(), SalesDealCatalog.HOLD_TERMS));
        reservation.setConsentStored(true);
        reservation.setCode("TMP");
        reservation = reservationRepository.save(reservation);
        reservation.setCode(String.format("DC-%06d", reservation.getId()));

        markConsignment(vehicleId, ConsignmentStatus.RESERVED);
        markListing(vehicleId, ListingStatus.RESERVED);
        salesInquiryService.markConverted(inquiryId);
        return reservation.getId();
    }

    @Override
    public void update(Long id, ReservationForm form) {
        Reservation reservation = require(id);
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new BusinessException("Phiếu đặt cọc này đã kết thúc.", HttpStatus.CONFLICT);
        }
        long price = priceOf(reservation);
        Long deposit = parseMoney(form.getDeposit());
        LocalDate holdUntil = parseDate(form.getHoldUntil());
        String error = checkDeposit(deposit, holdUntil, price);
        if (error != null) {
            throw new BusinessException(error, HttpStatus.BAD_REQUEST);
        }
        reservation.setDepositAmount(BigDecimal.valueOf(deposit));
        reservation.setHoldUntil(holdUntil);
        reservation.setTerms(blankTo(form.getTerms(), ""));
    }

    @Override
    public void cancel(Long id, String reason, String decision) {
        Reservation reservation = require(id);
        boolean canDecide = reservation.getStatus() == ReservationStatus.ACTIVE
                || reservation.getStatus() == ReservationStatus.EXPIRED;
        if (!canDecide) {
            throw new BusinessException("Phiếu đặt cọc này đã kết thúc.", HttpStatus.CONFLICT);
        }
        if (reason == null || reason.isBlank() || !SalesDealCatalog.DEPOSIT_DECISIONS.containsKey(decision)) {
            throw new BusinessException("Vui lòng nhập lý do và chọn hoàn hay giữ cọc.", HttpStatus.BAD_REQUEST);
        }
        reservation.setCancelReason(reason.trim());
        reservation.setDepositDecision(decision);
        if (reservation.getStatus() == ReservationStatus.ACTIVE) {
            Long inquiryId = reservation.getInquiry().getId();
            Long vehicleId = reservation.getVehicle().getId();
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservation.setCancelledAt(Instant.now());
            cancelDraftContract(reservation.getId());
            markConsignment(vehicleId, ConsignmentStatus.LISTED);
            relistNews(vehicleId);
            salesInquiryService.reopenAfterHoldCancel(inquiryId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> contractIdsByReservation() {
        Map<Long, Long> map = new LinkedHashMap<>();
        salesContractRepository.findAllDetail().forEach(c ->
                map.put(c.getReservation().getId(), c.getId()));
        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> contractIdOf(Long reservationId) {
        return salesContractRepository.findByReservationId(reservationId).map(SalesContract::getId);
    }

    private void expireOverdue() {
        LocalDate today = LocalDate.now(ZONE);
        for (Reservation r : reservationRepository.findAllActive()) {
            if (r.getHoldUntil() != null && r.getHoldUntil().isBefore(today)) {
                Long inquiryId = r.getInquiry().getId();
                Long vehicleId = r.getVehicle().getId();
                r.setStatus(ReservationStatus.EXPIRED);
                cancelDraftContract(r.getId());
                markConsignment(vehicleId, ConsignmentStatus.LISTED);
                relistNews(vehicleId);
                salesInquiryService.reopenAfterHoldCancel(inquiryId);
            }
        }
    }

    private boolean isVehicleHeld(Long vehicleId) {
        if (reservationRepository.existsByVehicleIdAndStatus(vehicleId, ReservationStatus.ACTIVE)) {
            return true;
        }
        return salesContractRepository.findAllDetail().stream()
                .anyMatch(c -> c.getReservation().getVehicle().getId().equals(vehicleId)
                        && isHoldingContract(c.getStatus()));
    }

    /** HĐ nháp không giữ xe vĩnh viễn — chỉ HĐ đã ký / đang thực hiện / hoàn tất. */
    private static boolean isHoldingContract(fu.he.training.fall2026.entity.enums.ContractStatus status) {
        return status == fu.he.training.fall2026.entity.enums.ContractStatus.SIGNED
                || status == fu.he.training.fall2026.entity.enums.ContractStatus.ACTIVE
                || status == fu.he.training.fall2026.entity.enums.ContractStatus.COMPLETED;
    }

    private void cancelDraftContract(Long reservationId) {
        salesContractRepository.findByReservationId(reservationId).ifPresent(contract -> {
            if (contract.getStatus() == fu.he.training.fall2026.entity.enums.ContractStatus.DRAFT) {
                contract.setStatus(fu.he.training.fall2026.entity.enums.ContractStatus.CANCELLED);
            }
        });
    }

    private void markConsignment(Long vehicleId, ConsignmentStatus status) {
        List<Consignment> rows = consignmentRepository.findByVehicleIdOrderByIdDesc(vehicleId);
        if (!rows.isEmpty()) {
            rows.get(0).setStatus(status);
        }
    }

    private void markListing(Long vehicleId, ListingStatus status) {
        newsRepository.findByVehicleId(vehicleId).forEach(news -> {
            if (news.getStatus() == ListingStatus.LISTED || news.getStatus() == ListingStatus.PAUSED) {
                news.setStatus(status);
            }
        });
    }

    /** Hủy/hết hạn phiếu thì mở lại tin đang giữ cọc hoặc đã gỡ khi lập hợp đồng nháp. */
    private void relistNews(Long vehicleId) {
        newsRepository.findByVehicleId(vehicleId).forEach(news -> {
            if (news.getStatus() == ListingStatus.RESERVED || news.getStatus() == ListingStatus.SOLD) {
                news.setStatus(ListingStatus.LISTED);
            }
        });
    }

    private Reservation require(Long id) {
        return reservationRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu đặt cọc", id));
    }

    private User requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException("Phiên đăng nhập không hợp lệ.", HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));
    }

    private ReservationResponse toDto(Reservation reservation) {
        Inquiry inquiry = reservation.getInquiry();
        Vehicle vehicle = reservation.getVehicle();
        User staff = reservation.getAssignedTo();
        Consignment consignment = consignmentRepository.findByVehicleIdOrderByIdDesc(vehicle.getId()).stream()
                .findFirst()
                .orElse(null);

        ReservationResponse dto = new ReservationResponse();
        dto.setId(reservation.getId());
        dto.setCode(reservation.getCode());
        dto.setInquiryId(inquiry.getId());
        dto.setInquiryCode(inquiry.getCode());
        dto.setIni(SalesInquiryCatalog.initials(inquiry.getFullName()));
        dto.setBuyer(nullToEmpty(inquiry.getFullName()));
        dto.setPhone(formatPhone(inquiry.getPhone()));
        dto.setEmail(nullToEmpty(inquiry.getEmail()));
        dto.setVehicleId(vehicle.getId());
        dto.setCar(carTitle(vehicle));
        dto.setFile(consignment == null ? "" : consignment.getCode());
        dto.setPlate(nullToEmpty(vehicle.getLicensePlate()));
        dto.setKm(vehicle.getOdometerKm() == null ? 0 : vehicle.getOdometerKm());
        dto.setPrice(priceOf(reservation));
        dto.setColor(nullToEmpty(vehicle.getColor()));
        dto.setFuel(nullToEmpty(vehicle.getFuelType()));
        dto.setOwner(consignment == null || consignment.getConsignor() == null
                ? ""
                : nullToEmpty(consignment.getConsignor().getFullName()));
        dto.setVin(nullToEmpty(vehicle.getVin()));
        dto.setStaff(staff == null ? "" : nullToEmpty(staff.getFullName()));
        dto.setStatus(reservation.getStatus().name());
        dto.setDeposit(reservation.getDepositAmount() == null ? 0L : reservation.getDepositAmount().longValue());
        dto.setHoldUntil(reservation.getHoldUntil() == null ? "" : reservation.getHoldUntil().toString());
        dto.setDaysLeft(reservation.getHoldUntil() == null
                ? 0L
                : ChronoUnit.DAYS.between(LocalDate.now(ZONE), reservation.getHoldUntil()));
        dto.setTerms(nullToEmpty(reservation.getTerms()));
        dto.setCreated(LocalDateTime.ofInstant(reservation.getCreatedAt(), ZONE).withSecond(0).withNano(0).toString());
        dto.setCancelReason(nullToEmpty(reservation.getCancelReason()));
        dto.setCancelledAt(reservation.getCancelledAt() == null
                ? ""
                : LocalDateTime.ofInstant(reservation.getCancelledAt(), ZONE).withSecond(0).withNano(0).toString());
        dto.setDepositDecision(nullToEmpty(reservation.getDepositDecision()));
        return dto;
    }

    /** Card rút gọn cho dropdown chọn khách. */
    private ReservationResponse buyerCard(Inquiry inquiry) {
        ReservationResponse dto = new ReservationResponse();
        dto.setId(inquiry.getId());
        dto.setCode(inquiry.getCode());
        dto.setBuyer(nullToEmpty(inquiry.getFullName()));
        dto.setPhone(formatPhone(inquiry.getPhone()));
        dto.setCar(carTitle(inquiry.getVehicle()));
        return dto;
    }

    /** Ưu tiên giá tin đăng (listPrice), không có thì lấy giá chào bán của xe — dùng thống nhất tạo/sửa/HĐ. */
    long priceOf(Reservation reservation) {
        return listedPriceOf(reservation.getVehicle());
    }

    long listedPriceOf(Vehicle vehicle) {
        long fallback = vehicle.getAskingPrice() == null ? 0L : vehicle.getAskingPrice().longValue();
        return newsRepository.findByVehicleId(vehicle.getId()).stream()
                .filter(n -> n.getStatus() == ListingStatus.LISTED
                        || n.getStatus() == ListingStatus.RESERVED
                        || n.getStatus() == ListingStatus.PAUSED
                        || n.getStatus() == ListingStatus.SOLD)
                .map(n -> n.getListPrice() != null ? n.getListPrice().longValue() : fallback)
                .findFirst()
                .orElse(fallback);
    }

    static void bindFiltered(
            Model model,
            String base,
            List<? extends Object> rows,
            String amountKey,
            String dateKey,
            Map<String, Map<String, String>> statuses,
            DealListFilter filter) {
        Page<?> page = filterPage(rows, amountKey, dateKey, filter);
        Map<String, Long> counts = new LinkedHashMap<>();
        statuses.keySet().forEach(key -> counts.put(key, rows.stream()
                .filter(r -> key.equals(read(r, "status")))
                .count()));

        model.addAttribute("base", base);
        model.addAttribute("page", page);
        model.addAttribute("counts", counts);
        model.addAttribute("staffs", rows.stream()
                .map(r -> String.valueOf(read(r, "staff")))
                .filter(s -> !s.isBlank())
                .distinct()
                .sorted()
                .toList());
        model.addAttribute("q", blank(filter.getQ()));
        model.addAttribute("status", blank(filter.getStatus()));
        model.addAttribute("staff", blank(filter.getStaff()));
        model.addAttribute("dateFrom", isoDate(filter.getFrom()));
        model.addAttribute("dateTo", isoDate(filter.getTo()));
        model.addAttribute("amountMin", blank(filter.getAmountMin()));
        model.addAttribute("amountMax", blank(filter.getAmountMax()));
        model.addAttribute("size", CatalogPageSupport.normalizeSize(parseSize(filter.getSize())));
    }

    @SuppressWarnings("unchecked")
    private static <T> Page<T> filterPage(
            List<? extends Object> rows, String amountKey, String dateKey, DealListFilter filter) {
        String keyword = blank(filter.getQ()).toLowerCase(Locale.ROOT);
        String status = blank(filter.getStatus());
        String staff = blank(filter.getStaff());
        String dateFrom = isoDate(filter.getFrom());
        String dateTo = isoDate(filter.getTo());
        Long amountMin = million(filter.getAmountMin());
        Long amountMax = million(filter.getAmountMax());

        List<Object> shown = rows.stream()
                .filter(r -> keyword.isEmpty() || haystack(r).contains(keyword))
                .filter(r -> status.isEmpty() || status.equals(read(r, "status")))
                .filter(r -> staff.isEmpty() || staff.equals(read(r, "staff")))
                .filter(r -> dateFrom.isEmpty() || day(r, dateKey).compareTo(dateFrom) >= 0)
                .filter(r -> dateTo.isEmpty() || day(r, dateKey).compareTo(dateTo) <= 0)
                .filter(r -> amountMin == null || ((Number) read(r, amountKey)).longValue() >= amountMin)
                .filter(r -> amountMax == null || ((Number) read(r, amountKey)).longValue() <= amountMax)
                .map(Object.class::cast)
                .toList();

        int page = Math.max(parsePage(filter.getPage()), 0);
        return (Page<T>) CatalogPageSupport.pageOf(shown, page, parseSize(filter.getSize()));
    }

    private static int parsePage(String text) {
        Integer value = parseSize(text);
        return value == null ? 0 : value;
    }

    private static Object read(Object bean, String field) {
        try {
            String getter = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
            return bean.getClass().getMethod(getter).invoke(bean);
        } catch (ReflectiveOperationException ex) {
            if (bean instanceof Map<?, ?> map) {
                return map.get(field);
            }
            throw new IllegalStateException(ex);
        }
    }

    private static String haystack(Object row) {
        return String.join(" ", List.of("code", "file", "car", "plate", "buyer", "phone").stream()
                .map(k -> read(row, k))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList()).toLowerCase(Locale.ROOT);
    }

    private static String day(Object row, String dateKey) {
        String value = String.valueOf(read(row, dateKey));
        return value.length() >= 10 ? value.substring(0, 10) : value;
    }

    private static String checkDeposit(Long deposit, LocalDate holdUntil, long price) {
        if (deposit == null || deposit <= 0) {
            return "Vui lòng nhập số tiền cọc.";
        }
        if (deposit > price) {
            return "Tiền cọc không được lớn hơn giá xe.";
        }
        if (holdUntil == null || holdUntil.isBefore(LocalDate.now(ZONE))) {
            return "Hạn giữ xe phải từ hôm nay trở đi.";
        }
        return null;
    }

    public static Long parseMoney(String text) {
        String digits = text == null ? "" : text.replaceAll("\\D", "");
        return digits.isEmpty() || digits.length() > 13 ? null : Long.valueOf(digits);
    }

    public static String money(long value) {
        return String.format(Locale.US, "%,d", value).replace(',', '.');
    }

    public static Long parseId(String text) {
        return text == null || !text.matches("\\d{1,18}") ? null : Long.valueOf(text);
    }

    public static LocalDate parseDate(String text) {
        String iso = isoDate(text);
        return iso.isEmpty() ? null : LocalDate.parse(iso);
    }

    private static Integer parseSize(String text) {
        return text == null || !text.matches("\\d{1,5}") ? null : Integer.valueOf(text);
    }

    private static Long million(String text) {
        Integer value = parseSize(text);
        return value == null ? null : value * 1_000_000L;
    }

    private static String isoDate(String text) {
        if (text == null || !text.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return "";
        }
        try {
            LocalDate.parse(text);
            return text;
        } catch (Exception ex) {
            return "";
        }
    }

    private static String carTitle(Vehicle vehicle) {
        String year = vehicle.getYear() == null ? "" : " " + vehicle.getYear();
        return vehicle.getDisplayLabel() + year;
    }

    private static String formatPhone(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        if (digits.length() == 10) {
            return digits.substring(0, 4) + " " + digits.substring(4, 7) + " " + digits.substring(7);
        }
        return nullToEmpty(phone);
    }

    private static String blank(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankTo(String value, String fallback) {
        String t = blank(value);
        return t.isEmpty() ? fallback : t;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
