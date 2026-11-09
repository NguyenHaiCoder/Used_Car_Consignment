package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.constant.sales.SalesInquiryCatalog;
import fu.he.training.fall2026.dto.sales.SalesInquiryForm;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.enums.InquiryStatus;
import fu.he.training.fall2026.entity.enums.ListingStatus;
import fu.he.training.fall2026.entity.enums.ReservationStatus;
import fu.he.training.fall2026.entity.enums.ViewingStatus;
import fu.he.training.fall2026.entity.sales.Inquiry;
import fu.he.training.fall2026.entity.sales.Viewing;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.InquiryRepository;
import fu.he.training.fall2026.repository.ReservationRepository;
import fu.he.training.fall2026.repository.UserRepository;
import fu.he.training.fall2026.repository.VehicleSaleNewsRepository;
import fu.he.training.fall2026.repository.ViewingRepository;
import fu.he.training.fall2026.service.SalesInquiryService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SalesInquiryServiceImpl implements SalesInquiryService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<ViewingStatus> ACTIVE = EnumSet.of(ViewingStatus.BOOKED, ViewingStatus.CONFIRMED);
    private static final Set<InquiryStatus> HOLDABLE =
            EnumSet.of(InquiryStatus.OPEN, InquiryStatus.SCHEDULED, InquiryStatus.VIEWED);

    private final InquiryRepository inquiryRepository;
    private final ViewingRepository viewingRepository;
    private final ReservationRepository reservationRepository;
    private final VehicleSaleNewsRepository newsRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> search(
            String q,
            InquiryStatus status,
            ViewingStatus viewing,
            String channel,
            String need,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {
        String pattern = "%" + (q == null ? "" : q.trim().toLowerCase(Locale.ROOT)) + "%";
        Instant fromAt = from == null ? null : from.atStartOfDay(ZONE).toInstant();
        Instant toAt = to == null ? null : to.plusDays(1).atStartOfDay(ZONE).toInstant();
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id"));
        return inquiryRepository
                .search(pattern, status, blankToNull(channel), blankToNull(need), viewing, fromAt, toAt, sorted)
                .map(this::toCard);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByStatus() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String key : SalesInquiryCatalog.STATUSES.keySet()) {
            counts.put(key, inquiryRepository.countByStatus(InquiryStatus.valueOf(key)));
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCard(Long id) {
        return toCard(requireInquiry(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> viewingsOf(Long inquiryId) {
        requireInquiry(inquiryId);
        return viewingRepository.findByInquiryIdOrderByLatest(inquiryId).stream()
                .map(v -> toViewingCard(v, inquiryId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Map<String, Object>> latestViewings(Iterable<Long> inquiryIds) {
        Map<Long, Map<String, Object>> latest = new LinkedHashMap<>();
        for (Long id : inquiryIds) {
            List<Viewing> rows = viewingRepository.findByInquiryIdOrderByLatest(id);
            if (!rows.isEmpty()) {
                latest.put(id, toViewingCard(rows.get(0), id));
            }
        }
        return latest;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getViewing(Long inquiryId, Long viewingId) {
        return toViewingCard(requireViewing(inquiryId, viewingId), inquiryId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Map<String, Object>> listedCars() {
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
                    Map<String, Object> row = vehicleFacts(v, n.getListPrice());
                    row.put("title", carTitle(v));
                    cars.put(String.valueOf(v.getId()), row);
                });
        return cars;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> findBuyerByPhone(String phone) {
        String normalized = normalizePhone(phone);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return inquiryRepository.findByPhoneOrderByIdDesc(normalized).stream()
                .findFirst()
                .map(this::toCard);
    }

    @Override
    @Transactional
    public Long createByStaff(SalesInquiryForm form, Long staffUserId) {
        String phone = normalizePhone(form.getPhone());
        if (!isValidVnMobile(phone)) {
            throw new BusinessException(
                    "Số điện thoại chưa đúng. Nhập 10 số bắt đầu bằng 0.", HttpStatus.BAD_REQUEST);
        }
        String fullName = trim(form.getFullName());
        if (fullName.length() < 2) {
            throw new BusinessException("Vui lòng nhập họ và tên khách hàng.", HttpStatus.BAD_REQUEST);
        }
        Long vehicleId = parseLong(form.getCar());
        Vehicle vehicle = requireListedVehicle(vehicleId);
        boolean known = !inquiryRepository.findByPhoneOrderByIdDesc(phone).isEmpty();
        if (!known && !Boolean.TRUE.equals(form.getConsent())) {
            throw new BusinessException("Khách mới cần đồng ý cho lưu thông tin.", HttpStatus.BAD_REQUEST);
        }
        Optional<Inquiry> existing = inquiryRepository.findOpenByPhoneAndVehicle(phone, vehicle.getId())
                .stream()
                .findFirst();
        if (existing.isPresent()) {
            throw new BusinessException(
                    "Số điện thoại này đã có tư vấn cho xe " + carTitle(vehicle) + ", mở lại để cập nhật tiếp.",
                    HttpStatus.CONFLICT,
                    existing.get().getId());
        }

        Inquiry inquiry = new Inquiry();
        inquiry.setCode(temporaryCode());
        inquiry.setVehicle(vehicle);
        inquiry.setStatus(InquiryStatus.OPEN);
        inquiry.setAssignedTo(requireUser(staffUserId));
        inquiry.setChannel(blankTo(form.getChannel(), "PHONE"));
        inquiry.setNeed(blankTo(form.getNeed(), "PRICE"));
        inquiry.setFullName(fullName);
        inquiry.setPhone(phone);
        inquiry.setEmail(blankToNull(form.getEmail()));
        inquiry.setAddress(blankToNull(form.getArea()));
        inquiry.setNotes(buildStaffNotes(form));
        inquiry.setConsentStored(true);
        inquiryRepository.save(inquiry);
        inquiry.setCode(String.format("TV-%06d", inquiry.getId()));
        return inquiry.getId();
    }

    @Override
    @Transactional
    public void update(Long id, SalesInquiryForm form) {
        Inquiry inquiry = requireInquiry(id);
        assertEditable(inquiry);
        String fullName = trim(form.getFullName());
        if (fullName.length() < 2) {
            throw new BusinessException("Vui lòng nhập họ và tên khách hàng.", HttpStatus.BAD_REQUEST);
        }
        inquiry.setFullName(fullName);
        inquiry.setNeed(blankTo(form.getNeed(), inquiry.getNeed()));
        inquiry.setEmail(blankToNull(form.getEmail()));
        inquiry.setAddress(blankToNull(form.getArea()));
        inquiry.setNotes(buildStaffNotes(form));
    }

    @Override
    @Transactional
    public void claim(Long id, Long staffUserId) {
        Inquiry inquiry = requireInquiry(id);
        if (inquiry.getStatus() != InquiryStatus.NEW) {
            return;
        }
        inquiry.setAssignedTo(requireUser(staffUserId));
        inquiry.setStatus(InquiryStatus.OPEN);
    }

    @Override
    @Transactional
    public void saveNote(Long id, String note) {
        Inquiry inquiry = requireInquiry(id);
        assertEditable(inquiry);
        String meta = keepMetaLines(inquiry.getNotes());
        String body = stripMetaLines(note == null ? "" : note.trim());
        if (body.isEmpty()) {
            inquiry.setNotes(meta);
        } else if (meta.isEmpty()) {
            inquiry.setNotes(body);
        } else {
            inquiry.setNotes(body + "\n" + meta);
        }
    }

    @Override
    @Transactional
    public void close(Long id, String closeReason, String note) {
        Inquiry inquiry = requireInquiry(id);
        assertEditable(inquiry);
        cancelActiveViewings(id);
        inquiry.setStatus(InquiryStatus.CLOSED);
        inquiry.setCloseReason(trim(closeReason));
        if (note != null && !note.isBlank()) {
            inquiry.setNotes(note.trim());
        }
    }

    @Override
    @Transactional
    public void scheduleViewing(
            Long inquiryId, LocalDateTime start, int minutes, String type, String place, String note) {
        Inquiry inquiry = requireInquiry(inquiryId);
        assertEditable(inquiry);
        if (inquiry.getStatus() == InquiryStatus.NEW) {
            throw new BusinessException("Cần nhận tư vấn trước khi đặt lịch.", HttpStatus.CONFLICT);
        }
        assertSchedule(inquiry.getVehicle().getId(), start, minutes, null);
        Viewing viewing = new Viewing();
        viewing.setCode(temporaryCode());
        viewing.setInquiry(inquiry);
        applySchedule(viewing, start, minutes, type, place, note);
        viewingRepository.save(viewing);
        viewing.setCode(String.format("LX-%06d", viewing.getId()));
        inquiry.setStatus(InquiryStatus.SCHEDULED);
    }

    @Override
    @Transactional
    public void rescheduleViewing(
            Long inquiryId,
            Long viewingId,
            LocalDateTime start,
            int minutes,
            String type,
            String place,
            String note) {
        Inquiry inquiry = requireInquiry(inquiryId);
        Viewing viewing = requireViewing(inquiryId, viewingId);
        assertEditable(inquiry);
        assertActive(viewing);
        assertSchedule(inquiry.getVehicle().getId(), start, minutes, viewingId);
        applySchedule(viewing, start, minutes, type, place, note);
    }

    @Override
    @Transactional
    public void confirmViewing(Long inquiryId, Long viewingId) {
        moveViewing(inquiryId, viewingId, ViewingStatus.CONFIRMED);
    }

    @Override
    @Transactional
    public void finishViewing(Long inquiryId, Long viewingId) {
        Inquiry inquiry = requireInquiry(inquiryId);
        Viewing viewing = requireViewing(inquiryId, viewingId);
        assertEditable(inquiry);
        assertActive(viewing);
        viewing.setStatus(ViewingStatus.DONE);
        inquiry.setStatus(InquiryStatus.VIEWED);
    }

    @Override
    @Transactional
    public void cancelViewing(Long inquiryId, Long viewingId) {
        Inquiry inquiry = requireInquiry(inquiryId);
        Viewing viewing = requireViewing(inquiryId, viewingId);
        assertEditable(inquiry);
        assertActive(viewing);
        viewing.setStatus(ViewingStatus.CANCELLED);
        if (!viewingRepository.existsByInquiryIdAndStatusIn(inquiryId, ACTIVE)) {
            inquiry.setStatus(statusWithoutActiveStep(inquiryId));
        }
    }

    @Override
    @Transactional
    public void markConverted(Long inquiryId) {
        Inquiry inquiry = requireInquiry(inquiryId);
        cancelActiveViewings(inquiryId);
        inquiry.setStatus(InquiryStatus.CONVERTED);
    }

    @Override
    @Transactional
    public void reopenAfterHoldCancel(Long inquiryId) {
        Inquiry inquiry = requireInquiry(inquiryId);
        if (inquiry.getStatus() == InquiryStatus.CONVERTED) {
            inquiry.setStatus(statusWithoutActiveStep(inquiryId));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canHold(Long inquiryId) {
        Inquiry inquiry = requireInquiry(inquiryId);
        return HOLDABLE.contains(inquiry.getStatus())
                && !reservationRepository.existsByInquiryIdAndStatus(inquiryId, ReservationStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> latestReservationCard(Long inquiryId) {
        return reservationRepository.findFirstByInquiryIdOrderByIdDesc(inquiryId).map(r -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", r.getId());
            row.put("code", r.getCode());
            row.put("status", r.getStatus().name());
            return row;
        });
    }

    private void moveViewing(Long inquiryId, Long viewingId, ViewingStatus next) {
        Inquiry inquiry = requireInquiry(inquiryId);
        Viewing viewing = requireViewing(inquiryId, viewingId);
        assertEditable(inquiry);
        assertActive(viewing);
        viewing.setStatus(next);
    }

    private void cancelActiveViewings(Long inquiryId) {
        viewingRepository.findByInquiryIdOrderByLatest(inquiryId).stream()
                .filter(v -> ACTIVE.contains(v.getStatus()))
                .forEach(v -> v.setStatus(ViewingStatus.CANCELLED));
    }

    private InquiryStatus statusWithoutActiveStep(Long inquiryId) {
        return viewingRepository.existsByInquiryIdAndStatus(inquiryId, ViewingStatus.DONE)
                ? InquiryStatus.VIEWED
                : InquiryStatus.OPEN;
    }

    private void assertSchedule(Long vehicleId, LocalDateTime start, int minutes, Long exceptId) {
        if (!start.isAfter(LocalDateTime.now(ZONE))) {
            throw new BusinessException("Giờ hẹn phải ở tương lai.", HttpStatus.BAD_REQUEST);
        }
        Instant startAt = start.atZone(ZONE).toInstant();
        Instant endAt = start.plusMinutes(minutes).atZone(ZONE).toInstant();
        boolean busy = viewingRepository.findActiveByVehicle(vehicleId, ACTIVE).stream()
                .filter(v -> exceptId == null || !exceptId.equals(v.getId()))
                .anyMatch(v -> overlaps(v, startAt, endAt));
        if (busy) {
            throw new BusinessException(
                    "Xe đã có lịch khác trong khung giờ này, vui lòng chọn giờ khác.", HttpStatus.CONFLICT);
        }
    }

    private static boolean overlaps(Viewing viewing, Instant startAt, Instant endAt) {
        Instant otherStart = viewing.getScheduledAt();
        Instant otherEnd = otherStart.plusSeconds(viewing.getDurationMinutes() * 60L);
        return startAt.isBefore(otherEnd) && otherStart.isBefore(endAt);
    }

    private void applySchedule(
            Viewing viewing, LocalDateTime start, int minutes, String type, String place, String note) {
        viewing.setScheduledAt(start.atZone(ZONE).toInstant());
        viewing.setDurationMinutes(normalizeMinutes(minutes));
        viewing.setViewingType(blankTo(type, "VIEWING"));
        viewing.setLocation(blankTo(place, SalesInquiryCatalog.PLACES.get(0)));
        viewing.setNotes(note == null ? "" : note.trim());
        viewing.setStatus(ViewingStatus.BOOKED);
    }

    private Map<String, Object> toCard(Inquiry inquiry) {
        Vehicle vehicle = inquiry.getVehicle();
        User staff = inquiry.getAssignedTo();
        String staffName = staff == null ? "" : nullToEmpty(staff.getFullName());
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", inquiry.getId());
        row.put("code", inquiry.getCode());
        row.put("buyer", nullToEmpty(inquiry.getFullName()));
        row.put("phone", formatPhone(inquiry.getPhone()));
        row.put("ini", SalesInquiryCatalog.initials(inquiry.getFullName()));
        row.put("email", nullToEmpty(inquiry.getEmail()));
        row.put("area", nullToEmpty(inquiry.getAddress()));
        row.put("buyerNote", "");
        row.put("car", carTitle(vehicle));
        row.put("vehicleId", vehicle.getId());
        row.putAll(vehicleFacts(vehicle, vehicle.getAskingPrice()));
        row.put("status", inquiry.getStatus().name());
        row.put("channel", nullToEmpty(inquiry.getChannel()));
        row.put("need", nullToEmpty(inquiry.getNeed()));
        row.put("staff", staffName);
        row.put("staffIni", SalesInquiryCatalog.initials(staffName));
        row.put("created", LocalDateTime.ofInstant(inquiry.getCreatedAt(), ZONE).withSecond(0).withNano(0).toString());
        row.put("note", nullToEmpty(inquiry.getNotes()));
        row.put("budgetMin", "");
        row.put("budgetMax", "");
        row.put("priorities", List.of());
        row.put("buyTime", "");
        row.put("closeReason", nullToEmpty(inquiry.getCloseReason()));
        return row;
    }

    private Map<String, Object> toViewingCard(Viewing viewing, Long inquiryId) {
        LocalDateTime start = LocalDateTime.ofInstant(viewing.getScheduledAt(), ZONE);
        String typeKey = blankTo(viewing.getViewingType(), "VIEWING");
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", viewing.getId());
        row.put("inquiryId", inquiryId);
        row.put("start", start.toString());
        row.put("minutes", viewing.getDurationMinutes());
        row.put("end", start.plusMinutes(viewing.getDurationMinutes()).toLocalTime().toString());
        row.put("place", nullToEmpty(viewing.getLocation()));
        row.put("typeKey", typeKey);
        row.put("type", SalesInquiryCatalog.VIEWING_TYPES.getOrDefault(typeKey, "Xem xe"));
        row.put("status", viewing.getStatus().name());
        row.put("note", nullToEmpty(viewing.getNotes()));
        return row;
    }

    private static Map<String, Object> vehicleFacts(Vehicle vehicle, BigDecimal price) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("price", price == null ? 0L : price.longValue());
        row.put("km", vehicle.getOdometerKm() == null ? 0 : vehicle.getOdometerKm());
        row.put("fuel", nullToEmpty(vehicle.getFuelType()));
        row.put("color", nullToEmpty(vehicle.getColor()));
        row.put("plate", nullToEmpty(vehicle.getLicensePlate()));
        return row;
    }

    private Vehicle requireListedVehicle(Long vehicleId) {
        if (vehicleId == null) {
            throw new BusinessException("Vui lòng chọn xe khách quan tâm.", HttpStatus.BAD_REQUEST);
        }
        return newsRepository
                .findByStatusAndVehicleId(ListingStatus.LISTED, vehicleId)
                .map(n -> n.getConsignment().getVehicle())
                .orElseThrow(() -> new BusinessException("Xe không còn đang bán.", HttpStatus.CONFLICT));
    }

    private Inquiry requireInquiry(Long id) {
        return inquiryRepository
                .findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu tư vấn", id));
    }

    private Viewing requireViewing(Long inquiryId, Long viewingId) {
        return viewingRepository
                .findDetail(inquiryId, viewingId)
                .orElseThrow(() -> new ResourceNotFoundException("Lịch xem xe", viewingId));
    }

    private User requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException("Phiên đăng nhập không hợp lệ.", HttpStatus.UNAUTHORIZED);
        }
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));
    }

    private static void assertEditable(Inquiry inquiry) {
        if (inquiry.getStatus() == InquiryStatus.CLOSED || inquiry.getStatus() == InquiryStatus.CONVERTED) {
            throw new BusinessException("Tư vấn đã kết thúc nên không thay đổi được.", HttpStatus.CONFLICT);
        }
    }

    private static void assertActive(Viewing viewing) {
        if (!ACTIVE.contains(viewing.getStatus())) {
            throw new BusinessException("Lịch này đã kết thúc.", HttpStatus.CONFLICT);
        }
    }

    private static String buildStaffNotes(SalesInquiryForm form) {
        String note = trim(form.getNote());
        if (note.isEmpty()) {
            note = trim(form.getBuyerNote());
        }
        String budgetMin = trim(form.getBudgetMin());
        String budgetMax = trim(form.getBudgetMax());
        String buyTime = trim(form.getBuyTime());
        StringBuilder sb = new StringBuilder(note);
        if (!budgetMin.isEmpty() || !budgetMax.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append("Ngân sách: ").append(budgetMin).append(" – ").append(budgetMax).append(" triệu");
        }
        if (!buyTime.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append("Thời gian mua: ").append(buyTime);
        }
        return sb.toString();
    }

    /** Giữ dòng ngân sách / thời gian mua khi nhân viên chỉ sửa ghi chú tư vấn. */
    private static String keepMetaLines(String notes) {
        if (notes == null || notes.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String line : notes.split("\\R")) {
            if (isMetaLine(line)) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(line.trim());
            }
        }
        return sb.toString();
    }

    private static String stripMetaLines(String notes) {
        if (notes == null || notes.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String line : notes.split("\\R")) {
            if (!isMetaLine(line)) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(line);
            }
        }
        return sb.toString().trim();
    }

    private static boolean isMetaLine(String line) {
        String t = line == null ? "" : line.trim();
        return t.startsWith("Ngân sách:") || t.startsWith("Thời gian mua:");
    }

    private static String carTitle(Vehicle vehicle) {
        String label = vehicle.getDisplayLabel();
        if (vehicle.getYear() != null) {
            label = label + " " + vehicle.getYear();
        }
        return label.trim();
    }

    private static int normalizeMinutes(int minutes) {
        return SalesInquiryCatalog.DURATIONS.contains(minutes) ? minutes : 45;
    }

    static String normalizePhone(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.startsWith("84") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }
        return digits;
    }

    static boolean isValidVnMobile(String phone) {
        return phone != null && phone.matches("0[35789]\\d{8}");
    }

    private static String formatPhone(String phone) {
        if (phone == null || phone.length() != 10) {
            return nullToEmpty(phone);
        }
        return phone.substring(0, 4) + " " + phone.substring(4, 7) + " " + phone.substring(7);
    }

    private static Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String temporaryCode() {
        return "TMP-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
