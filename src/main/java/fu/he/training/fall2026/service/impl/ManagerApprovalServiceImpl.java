package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.entity.auth.Approval;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.ConsignmentAgreement;
import fu.he.training.fall2026.entity.consignment.Consignor;
import fu.he.training.fall2026.entity.consignment.Inspection;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.consignment.VehicleSaleNews;
import fu.he.training.fall2026.entity.sales.SalesContract;
import fu.he.training.fall2026.entity.enums.AgreementStatus;
import fu.he.training.fall2026.entity.enums.ApprovalStatus;
import fu.he.training.fall2026.entity.enums.ApprovalType;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.enums.InspectionStatus;
import fu.he.training.fall2026.entity.enums.ListingStatus;
import fu.he.training.fall2026.entity.enums.ReservationStatus;
import fu.he.training.fall2026.entity.enums.RoleCode;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.ApprovalRepository;
import fu.he.training.fall2026.repository.ConsignmentAgreementRepository;
import fu.he.training.fall2026.repository.ConsignmentRepository;
import fu.he.training.fall2026.repository.InspectionRepository;
import fu.he.training.fall2026.repository.SalesContractRepository;
import fu.he.training.fall2026.repository.UserRepository;
import fu.he.training.fall2026.repository.VehicleSaleNewsRepository;
import fu.he.training.fall2026.service.AuditService;
import fu.he.training.fall2026.service.ManagerApprovalService;
import fu.he.training.fall2026.service.SettlementService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nguyễn Việt Hải: hàng đợi phê duyệt chạy trên DB.
 *
 * <p>Nguồn việc chờ duyệt (thêm: Giá dưới sàn do Sales gửi khi bấm Ký hợp đồng bán với giá thấp hơn giá sàn):
 * <ul>
 *   <li>Hồ sơ ký gửi SUBMITTED và hợp đồng ký gửi PENDING_SIGNATURE: luồng ký gửi chỉ đổi trạng thái, không tạo yêu cầu,
 *       nên khi mở hàng đợi thì tạo dòng Approval PENDING cho cái nào chưa có.</li>
 *   <li>Rút ký gửi: dòng Approval do màn Rút ký gửi tạo sẵn.</li>
 *   <li>Quyết toán: lấy từ SettlementService, Quản lý duyệt ở màn Duyệt quyết toán.</li>
 * </ul>
 * Approval.reason giữ lý do của người gửi, ghi chú của Quản lý nằm ở decisionNote.
 */
@Service
@RequiredArgsConstructor
public class ManagerApprovalServiceImpl implements ManagerApprovalService {

    private static final String CONSIGNMENT_TARGET = "Consignment";
    private static final String AGREEMENT_TARGET = "ConsignmentAgreement";
    /** Giá dưới sàn do Sales gửi khi ký hợp đồng bán (SalesContractServiceImpl.FLOOR_TARGET). */
    private static final String SALES_CONTRACT_TARGET = "SalesContract";
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Hồ sơ ở các trạng thái này coi như đã khép lại, không tính là ký gửi đang mở. */
    private static final Set<ConsignmentStatus> ENDED = EnumSet.of(
            ConsignmentStatus.WITHDRAWN, ConsignmentStatus.CLOSED, ConsignmentStatus.COMPLETED, ConsignmentStatus.SOLD);
    /** Tin chưa bán, chưa dừng: rút ký gửi được duyệt thì phải dừng các tin này. */
    private static final Set<ListingStatus> OPEN_LISTINGS = EnumSet.of(
            ListingStatus.DRAFT, ListingStatus.READY, ListingStatus.LISTED, ListingStatus.PAUSED);

    private final ApprovalRepository approvalRepository;
    private final ConsignmentRepository consignmentRepository;
    private final ConsignmentAgreementRepository agreementRepository;
    private final VehicleSaleNewsRepository newsRepository;
    private final InspectionRepository inspectionRepository;
    private final SalesContractRepository salesContractRepository;
    private final UserRepository userRepository;
    private final SettlementService settlementService;
    private final AuditService auditService;

    @Override
    public Map<String, Map<String, String>> statusMeta() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put(PENDING, meta("Chờ duyệt", "amber", "hourglass", "pending"));
        map.put("NEED_MORE_INFO", meta("Cần bổ sung", "blue", "info", "signed"));
        map.put("APPROVED", meta("Đã duyệt", "green", "shield-check", "approved"));
        map.put("REJECTED", meta("Từ chối", "red", "close", "closed-red"));
        return map;
    }

    @Override
    public Map<String, Map<String, String>> typeMeta() {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        map.put("CONSIGNMENT", Map.of("label", "Hồ sơ ký gửi", "icon", "car"));
        map.put("AGREEMENT", Map.of("label", "Hợp đồng ký gửi", "icon", "handshake"));
        map.put("WITHDRAWAL", Map.of("label", "Rút ký gửi", "icon", "undo"));
        map.put("SETTLEMENT", Map.of("label", "Quyết toán", "icon", "clipboard"));
        map.put("BELOW_FLOOR_PRICE", Map.of("label", "Giá dưới sàn", "icon", "coins"));
        map.put("LISTING_ELIGIBILITY", Map.of("label", "Tin đăng", "icon", "tag"));
        map.put("OTHER", Map.of("label", "Khác", "icon", "info"));
        return map;
    }

    @Override
    @Transactional
    public List<Map<String, Object>> queue(Long viewerUserId) {
        openPendingRequests(viewerUserId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Approval approval : approvalRepository.findAllFetched()) {
            // Quyết toán đã có hàng riêng lấy từ SettlementService, các dòng Approval của nó chỉ là lịch sử.
            if (approval.getType() != ApprovalType.SETTLEMENT) {
                rows.add(row(approval, target(approval)));
            }
        }
        rows.addAll(settlementService.approvalQueueRows());
        rows.sort(Comparator.<Map<String, Object>, Boolean>comparing(r -> !PENDING.equals(r.get("status")))
                .thenComparing(r -> String.valueOf(r.get("created")), Comparator.reverseOrder()));
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id) {
        Approval approval = load(id);
        Target target = target(approval);
        Map<String, Object> item = row(approval, target);
        item.put("reason", blankTo(approval.getReason(), "—"));
        item.put("sections", sections(approval, target));
        item.put("checks", checks(approval, target));
        item.put("effects", effects(approval.getType()));
        // Key luôn có mặt (kể cả null) để template đọc item.decidedAt không bị lỗi.
        item.put("decidedAt", approval.getDecidedAt() == null ? null : time(approval.getDecidedAt()));
        item.put("decidedBy", approval.getApprovedBy() == null ? "" : approval.getApprovedBy().getFullName());
        item.put("decisionNote", blankTo(approval.getDecisionNote(), ""));
        return item;
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalType typeOf(Long id) {
        return load(id).getType();
    }

    @Override
    @Transactional
    public String decide(Long id, String decision, String note, Long actorUserId) {
        Approval approval = load(id);
        if (approval.getType() == ApprovalType.SETTLEMENT) {
            throw new BusinessException("Quyết toán được duyệt ở màn Duyệt quyết toán.", HttpStatus.CONFLICT);
        }
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException("Yêu cầu này đã được quyết định.", HttpStatus.CONFLICT);
        }
        ApprovalStatus next = parseDecision(decision);
        String cleanNote = note == null ? "" : note.trim();
        Target target = target(approval);
        if (next == ApprovalStatus.APPROVED) {
            long blockers = checks(approval, target).stream().filter(c -> !"true".equals(c.get("passed"))).count();
            if (blockers > 0) {
                throw new BusinessException("Còn " + blockers + " điều kiện chưa đạt nên chưa duyệt được.", HttpStatus.CONFLICT);
            }
        } else if (cleanNote.isEmpty()) {
            throw new BusinessException("Từ chối hoặc yêu cầu bổ sung thì phải nhập lý do.", HttpStatus.BAD_REQUEST);
        }

        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new BusinessException("Phiên đăng nhập không hợp lệ.", HttpStatus.UNAUTHORIZED));
        applyEffect(approval, target, next);
        close(approval, next, cleanNote, actor);
        auditService.record(actor, "DECIDE_APPROVAL", "Approval", approval.getId(),
                approval.getCode() + " → " + next.name() + (cleanNote.isEmpty() ? "" : ": " + cleanNote));
        return approval.getCode();
    }

    // ---------------------------------------------------------------- tạo dòng chờ

    private void openPendingRequests(Long viewerUserId) {
        User requester = null;
        for (Consignment consignment : consignmentRepository.findAllByStatusWithVehicle(ConsignmentStatus.SUBMITTED)) {
            if (!hasPending(ApprovalType.CONSIGNMENT, CONSIGNMENT_TARGET, consignment.getId())) {
                requester = requester != null ? requester : requester(viewerUserId);
                open(ApprovalType.CONSIGNMENT, CONSIGNMENT_TARGET, consignment.getId(),
                        "Hồ sơ ký gửi mới, cần duyệt trước khi tiếp nhận xe.", requester);
            }
        }
        for (ConsignmentAgreement agreement : agreementRepository.findAllWithConsignment()) {
            if (agreement.getStatus() == AgreementStatus.PENDING_SIGNATURE
                    && !hasPending(ApprovalType.AGREEMENT, AGREEMENT_TARGET, agreement.getId())) {
                requester = requester != null ? requester : requester(viewerUserId);
                open(ApprovalType.AGREEMENT, AGREEMENT_TARGET, agreement.getId(),
                        "Hợp đồng ký gửi đã gửi duyệt, cần duyệt trước khi có hiệu lực.", requester);
            }
        }
    }

    private boolean hasPending(ApprovalType type, String targetType, Long targetId) {
        return approvalRepository.existsByTypeAndTargetTypeAndTargetIdAndStatus(
                type, targetType, targetId, ApprovalStatus.PENDING);
    }

    private void open(ApprovalType type, String targetType, Long targetId, String reason, User requester) {
        Approval approval = new Approval();
        approval.setCode("TMP-" + UUID.randomUUID().toString().substring(0, 12));
        approval.setType(type);
        approval.setStatus(ApprovalStatus.PENDING);
        approval.setTargetType(targetType);
        approval.setTargetId(targetId);
        approval.setReason(reason);
        approval.setRequestedBy(requester);
        approvalRepository.save(approval);
        approval.setCode(String.format("PD-%06d", approval.getId()));
    }

    /**
     * Hồ sơ và hợp đồng ký gửi không lưu người gửi duyệt, nên ghi người gửi là nhân viên ký gửi đang hoạt động.
     * Hệ thống chưa có nhân viên ký gửi thì ghi người đang mở hàng đợi.
     */
    private User requester(Long viewerUserId) {
        return userRepository.findAllWithRole().stream()
                .filter(u -> u.isActive() && u.getRole() != null && u.getRole().getCode() == RoleCode.CONSIGNMENT)
                .findFirst()
                .orElseGet(() -> userRepository.findById(viewerUserId)
                        .orElseThrow(() -> new BusinessException("Phiên đăng nhập không hợp lệ.", HttpStatus.UNAUTHORIZED)));
    }

    // ---------------------------------------------------------------- quyết định

    private static ApprovalStatus parseDecision(String decision) {
        String value = decision == null ? "" : decision.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "APPROVED" -> ApprovalStatus.APPROVED;
            case "REJECTED" -> ApprovalStatus.REJECTED;
            case "NEED_MORE_INFO" -> ApprovalStatus.NEED_MORE_INFO;
            default -> throw new BusinessException("Quyết định không hợp lệ.", HttpStatus.BAD_REQUEST);
        };
    }

    private static void close(Approval approval, ApprovalStatus status, String note, User actor) {
        approval.setStatus(status);
        approval.setDecisionNote(note.isEmpty() ? null : note);
        approval.setApprovedBy(actor);
        approval.setDecidedAt(Instant.now());
    }

    /** Đổi trạng thái của đối tượng theo quyết định. Đối tượng đã đổi trạng thái ở nơi khác thì không cho quyết định. */
    private void applyEffect(Approval approval, Target target, ApprovalStatus next) {
        switch (approval.getType()) {
            case CONSIGNMENT -> {
                Consignment consignment = requireConsignment(target, ConsignmentStatus.SUBMITTED);
                if (next == ApprovalStatus.APPROVED) {
                    consignment.setStatus(ConsignmentStatus.ACTIVE);
                } else if (next == ApprovalStatus.NEED_MORE_INFO) {
                    consignment.setStatus(ConsignmentStatus.DRAFT);
                } else {
                    consignment.setStatus(ConsignmentStatus.CLOSED);
                    terminateOpenAgreement(consignment, approval);
                }
            }
            case AGREEMENT -> {
                ConsignmentAgreement agreement = target.agreement();
                if (agreement == null || agreement.getStatus() != AgreementStatus.PENDING_SIGNATURE) {
                    throw new BusinessException("Hợp đồng không còn ở trạng thái chờ duyệt.", HttpStatus.CONFLICT);
                }
                if (next == ApprovalStatus.APPROVED) {
                    agreement.setStatus(AgreementStatus.ACTIVE);
                } else if (next == ApprovalStatus.NEED_MORE_INFO) {
                    agreement.setStatus(AgreementStatus.DRAFT);
                } else {
                    agreement.setStatus(AgreementStatus.TERMINATED);
                    Consignment consignment = agreement.getConsignment();
                    if (!ENDED.contains(consignment.getStatus())) {
                        consignment.setStatus(ConsignmentStatus.CLOSED);
                    }
                }
            }
            case WITHDRAWAL -> {
                Consignment consignment = requireConsignment(target, ConsignmentStatus.WITHDRAW_REQUESTED);
                if (next == ApprovalStatus.APPROVED) {
                    consignment.setStatus(ConsignmentStatus.WITHDRAWN);
                    agreementRepository.findByConsignmentId(consignment.getId())
                            .filter(a -> a.getStatus() == AgreementStatus.ACTIVE)
                            .ifPresent(a -> a.setStatus(AgreementStatus.TERMINATED));
                    newsOf(consignment).stream()
                            .filter(n -> OPEN_LISTINGS.contains(n.getStatus()))
                            .forEach(n -> n.setStatus(ListingStatus.STOPPED));
                } else {
                    consignment.setStatus(statusBeforeWithdraw(consignment));
                }
            }
            case BELOW_FLOOR_PRICE -> {
                // Không đổi dữ liệu: Sales bấm Ký thì SalesContractService đọc quyết định này để cho ký hay không.
                SalesContract contract = target.salesContract();
                if (contract != null && contract.getStatus() != ContractStatus.DRAFT) {
                    throw new BusinessException("Hợp đồng bán đã ký, không còn chờ duyệt giá.", HttpStatus.CONFLICT);
                }
            }
            default -> {
                // Loại chưa nối luồng (tin đăng, khác): chỉ ghi nhận quyết định.
            }
        }
    }

    private static Consignment requireConsignment(Target target, ConsignmentStatus expected) {
        Consignment consignment = target.consignment();
        if (consignment == null || consignment.getStatus() != expected) {
            throw new BusinessException("Hồ sơ không còn ở trạng thái chờ duyệt.", HttpStatus.CONFLICT);
        }
        return consignment;
    }

    /** Từ chối hồ sơ thì hợp đồng ký gửi chưa có hiệu lực của hồ sơ đó cũng chấm dứt, yêu cầu duyệt hợp đồng đóng theo. */
    private void terminateOpenAgreement(Consignment consignment, Approval source) {
        agreementRepository.findByConsignmentId(consignment.getId())
                .filter(a -> a.getStatus() == AgreementStatus.DRAFT || a.getStatus() == AgreementStatus.PENDING_SIGNATURE)
                .ifPresent(agreement -> {
                    agreement.setStatus(AgreementStatus.TERMINATED);
                    approvalRepository.findRequests(ApprovalType.AGREEMENT, AGREEMENT_TARGET, agreement.getId()).stream()
                            .filter(a -> a.getStatus() == ApprovalStatus.PENDING)
                            .forEach(a -> {
                                a.setStatus(ApprovalStatus.REJECTED);
                                a.setDecisionNote("Hồ sơ ký gửi " + consignment.getCode() + " bị từ chối ở yêu cầu "
                                        + source.getCode() + ".");
                                a.setDecidedAt(Instant.now());
                            });
                });
    }

    /**
     * Trạng thái của hồ sơ trước khi xin rút. Hệ thống không lưu trạng thái cũ, nên suy từ dữ liệu,
     * ưu tiên bước đi xa nhất: đã bán, đã đặt cọc, đang đăng (kể cả tạm ẩn), đã kiểm định xong,
     * đang kiểm định, đã duyệt, còn lại là bản nháp.
     */
    private ConsignmentStatus statusBeforeWithdraw(Consignment consignment) {
        List<VehicleSaleNews> news = newsOf(consignment);
        if (news.stream().anyMatch(n -> n.getStatus() == ListingStatus.SOLD)) {
            return ConsignmentStatus.SOLD;
        }
        if (news.stream().anyMatch(n -> n.getStatus() == ListingStatus.RESERVED)) {
            return ConsignmentStatus.RESERVED;
        }
        if (news.stream().anyMatch(n -> n.getStatus() == ListingStatus.LISTED || n.getStatus() == ListingStatus.PAUSED)) {
            return ConsignmentStatus.LISTED;
        }
        Inspection inspection = inspectionRepository.findFirstByConsignmentIdOrderByIdDesc(consignment.getId())
                .orElse(null);
        if (inspection != null) {
            InspectionStatus inspectionStatus = inspection.getStatus();
            if (inspectionStatus == InspectionStatus.COMPLETED
                    || inspectionStatus == InspectionStatus.PASSED
                    || inspectionStatus == InspectionStatus.FAILED) {
                return ConsignmentStatus.INSPECTED;
            }
            return ConsignmentStatus.INSPECTION_RECEIVED;
        }
        boolean approved = approvalRepository.existsByTypeAndTargetTypeAndTargetIdAndStatus(
                ApprovalType.CONSIGNMENT, CONSIGNMENT_TARGET, consignment.getId(), ApprovalStatus.APPROVED)
                || agreementRepository.findByConsignmentId(consignment.getId())
                        .map(a -> a.getStatus() == AgreementStatus.ACTIVE)
                        .orElse(false);
        return approved ? ConsignmentStatus.ACTIVE : ConsignmentStatus.DRAFT;
    }

    private List<VehicleSaleNews> newsOf(Consignment consignment) {
        return newsRepository.findByVehicleId(consignment.getVehicle().getId()).stream()
                .filter(n -> n.getConsignment() != null && consignment.getId().equals(n.getConsignment().getId()))
                .toList();
    }

    // ---------------------------------------------------------------- đối tượng của yêu cầu

    /** Hồ sơ, hợp đồng mà yêu cầu trỏ tới. found = false khi đối tượng không còn trong hệ thống. */
    private record Target(String code, String car, String plate, long amount,
                          Consignment consignment, ConsignmentAgreement agreement, SalesContract salesContract,
                          boolean found) {

        static Target missing(Approval approval) {
            return new Target(approval.getTargetType() + " #" + approval.getTargetId(), "—", "", 0L, null, null, null,
                    false);
        }
    }

    private Target target(Approval approval) {
        return switch (approval.getType()) {
            case CONSIGNMENT, WITHDRAWAL -> consignmentRepository.findDetailById(approval.getTargetId())
                    .map(c -> new Target(c.getCode(), carLabel(c.getVehicle()), plate(c.getVehicle()),
                            money(c.getExpectedPrice()), c, null, null, true))
                    .orElse(Target.missing(approval));
            case AGREEMENT -> agreementRepository.findByIdWithConsignment(approval.getTargetId())
                    .map(a -> new Target(a.getCode(), carLabel(a.getConsignment().getVehicle()),
                            plate(a.getConsignment().getVehicle()), money(a.getPriceFloor()), a.getConsignment(), a, null,
                            true))
                    .orElse(Target.missing(approval));
            case BELOW_FLOOR_PRICE -> SALES_CONTRACT_TARGET.equals(approval.getTargetType())
                    ? salesContractRepository.findDetailById(approval.getTargetId())
                            .map(this::salesTarget)
                            .orElse(Target.missing(approval))
                    : Target.missing(approval);
            default -> Target.missing(approval);
        };
    }

    private Target salesTarget(SalesContract contract) {
        Vehicle vehicle = contract.getReservation().getVehicle();
        Consignment consignment = consignmentRepository.findByVehicleIdOrderByIdDesc(vehicle.getId()).stream()
                .findFirst().orElse(null);
        ConsignmentAgreement agreement = consignment == null ? null
                : agreementRepository.findByConsignmentId(consignment.getId()).orElse(null);
        return new Target(contract.getCode(), carLabel(vehicle), plate(vehicle), money(contract.getSalePrice()),
                consignment, agreement, contract, true);
    }

    private Map<String, Object> row(Approval approval, Target target) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", approval.getId());
        row.put("code", approval.getCode());
        row.put("type", approval.getType().name());
        row.put("target", target.code());
        row.put("car", target.car());
        row.put("plate", target.plate());
        row.put("staff", approval.getRequestedBy() == null ? "" : approval.getRequestedBy().getFullName());
        row.put("amount", target.amount());
        row.put("created", time(approval.getCreatedAt()));
        row.put("status", approval.getStatus().name());
        row.put("url", "/manager/approvals/" + approval.getId());
        return row;
    }

    private List<Map<String, Object>> sections(Approval approval, Target target) {
        if (!target.found()) {
            return List.of(section("Đối tượng", "info",
                    pair("Loại đối tượng", approval.getTargetType()),
                    pair("Mã đối tượng", String.valueOf(approval.getTargetId())),
                    pair("Ghi chú", "Loại yêu cầu này chưa nối dữ liệu, chỉ ghi nhận quyết định.")));
        }
        Consignment consignment = target.consignment();
        return switch (approval.getType()) {
            case CONSIGNMENT -> List.of(ownerSection(consignment.getConsignor()), carSection(consignment),
                    section("Hồ sơ", "file",
                            pair("Mã hồ sơ", consignment.getCode()),
                            pair("Ngày nhận xe", day(consignment.getIntakeDate())),
                            pair("Nơi giữ xe", blankTo(consignment.getCustodyLocation(), "Chưa nhập")),
                            pair("Ghi chú điều khoản", blankTo(consignment.getTermNotes(), "Không có"))));
            case AGREEMENT -> {
                ConsignmentAgreement agreement = target.agreement();
                yield List.of(section("Hợp đồng", "handshake",
                                pair("Mã hợp đồng", agreement.getCode()),
                                pair("Hoa hồng", percent(agreement.getCommissionRate())),
                                pair("Phí cố định", money(agreement.getFixedFee()) == 0 ? "Không có"
                                        : format(money(agreement.getFixedFee())) + " ₫"),
                                pair("Giá sàn", format(money(agreement.getPriceFloor())) + " ₫"),
                                pair("Hiệu lực", day(agreement.getValidFrom()) + " – " + day(agreement.getValidTo())),
                                pair("Bằng chứng chủ xe ký", hasText(agreement.getSignatureEvidencePath())
                                        ? "Đã tải lên" : "Chưa có")),
                        ownerSection(consignment.getConsignor()), carSection(consignment));
            }
            case WITHDRAWAL -> List.of(section("Yêu cầu rút", "undo",
                            pair("Lý do", blankTo(consignment.getWithdrawReason(), blankTo(approval.getReason(), "—"))),
                            pair("Ngày trả xe dự kiến", day(consignment.getWithdrawReturnDate())),
                            pair("Tình trạng khi trả", blankTo(consignment.getWithdrawCondition(), "Chưa nhập")),
                            pair("Bằng chứng", hasText(consignment.getWithdrawEvidencePath()) ? "Đã có" : "Chưa có")),
                    ownerSection(consignment.getConsignor()), carSection(consignment));
            case BELOW_FLOOR_PRICE -> {
                SalesContract contract = target.salesContract();
                Vehicle vehicle = contract.getReservation().getVehicle();
                long sale = money(contract.getSalePrice());
                long floor = target.agreement() == null ? 0L : money(target.agreement().getPriceFloor());
                long listed = money(vehicle.getAskingPrice());
                List<Map<String, Object>> list = new ArrayList<>();
                list.add(section("So sánh giá", "coins",
                        pair("Giá bán Sales đề xuất", format(sale) + " ₫"),
                        pair("Giá sàn của chủ xe", floor > 0 ? format(floor) + " ₫" : "Chưa có"),
                        pair("Thấp hơn giá sàn", floor > sale ? format(floor - sale) + " ₫" : "Không thấp hơn"),
                        pair("Giá niêm yết sau kiểm định", listed > 0 ? format(listed) + " ₫" : "Chưa có")));
                list.add(section("Hợp đồng bán", "handshake",
                        pair("Mã hợp đồng", contract.getCode()),
                        pair("Khách mua", blankTo(contract.getReservation().getInquiry().getFullName(), "—")),
                        pair("Phiếu đặt cọc", contract.getReservation().getCode()),
                        pair("Tiền cọc", format(money(contract.getDepositAmount())) + " ₫")));
                if (consignment != null) {
                    list.add(ownerSection(consignment.getConsignor()));
                }
                yield list;
            }
            default -> List.of();
        };
    }

    private List<Map<String, String>> checks(Approval approval, Target target) {
        if (!target.found()) {
            boolean wired = approval.getType() == ApprovalType.CONSIGNMENT
                    || approval.getType() == ApprovalType.AGREEMENT
                    || approval.getType() == ApprovalType.WITHDRAWAL
                    || SALES_CONTRACT_TARGET.equals(approval.getTargetType());
            return wired
                    ? List.of(check("Còn đối tượng cần duyệt", "Không tìm thấy " + target.code(), false))
                    : List.of();
        }
        Consignment consignment = target.consignment();
        return switch (approval.getType()) {
            case CONSIGNMENT -> {
                String other = otherOpenConsignment(consignment);
                yield List.of(
                        check("Chủ xe đồng ý ký gửi", consignment.isOwnerConsent() ? "Đã xác nhận" : "Chưa xác nhận",
                                consignment.isOwnerConsent()),
                        check("Đã nhập giá kỳ vọng", money(consignment.getExpectedPrice()) > 0
                                ? format(money(consignment.getExpectedPrice())) + " ₫" : "Chưa nhập",
                                money(consignment.getExpectedPrice()) > 0),
                        check("Có số VIN của xe", blankTo(consignment.getVehicle().getVin(), "Chưa nhập"),
                                hasText(consignment.getVehicle().getVin())),
                        check("Xe không có ký gửi khác đang mở", other.isEmpty() ? "Không trùng" : "Đang mở: " + other,
                                other.isEmpty()));
            }
            case AGREEMENT -> {
                ConsignmentAgreement agreement = target.agreement();
                boolean signed = hasText(agreement.getSignatureEvidencePath());
                boolean terms = positive(agreement.getCommissionRate()) && positive(agreement.getPriceFloor());
                boolean period = agreement.getValidFrom() != null && agreement.getValidTo() != null
                        && !agreement.getValidTo().isBefore(agreement.getValidFrom())
                        && !agreement.getValidTo().isBefore(LocalDate.now(ZONE));
                yield List.of(
                        check("Có bằng chứng chủ xe đã ký", signed ? "Đã tải lên" : "Chưa tải bằng chứng chữ ký", signed),
                        check("Có hoa hồng và giá sàn", percent(agreement.getCommissionRate()) + " · "
                                + format(money(agreement.getPriceFloor())) + " ₫", terms),
                        check("Thời hạn hợp lệ", "Đến " + day(agreement.getValidTo()), period));
            }
            case WITHDRAWAL -> {
                String hold = buyerHold(consignment);
                yield List.of(
                        check("Hồ sơ đang chờ rút", consignment.getStatus() == ConsignmentStatus.WITHDRAW_REQUESTED
                                ? "Đúng trạng thái" : "Trạng thái hiện tại: " + consignment.getStatus(),
                                consignment.getStatus() == ConsignmentStatus.WITHDRAW_REQUESTED),
                        check("Xe chưa có khách đặt cọc hoặc hợp đồng bán", hold.isEmpty() ? "Không có" : hold,
                                hold.isEmpty()),
                        check("Có ngày trả xe", day(consignment.getWithdrawReturnDate()),
                                consignment.getWithdrawReturnDate() != null));
            }
            case BELOW_FLOOR_PRICE -> {
                SalesContract contract = target.salesContract();
                long sale = money(contract.getSalePrice());
                long floor = target.agreement() == null ? 0L : money(target.agreement().getPriceFloor());
                boolean draft = contract.getStatus() == ContractStatus.DRAFT;
                yield List.of(
                        check("Hợp đồng bán còn là bản nháp", draft ? "Chưa ký" : "Trạng thái hiện tại: " + contract.getStatus(),
                                draft),
                        check("Có giá sàn trong hợp đồng ký gửi để so", floor > 0 ? format(floor) + " ₫" : "Chưa có", floor > 0),
                        check("Giá bán vẫn thấp hơn giá sàn", floor > sale ? "Thấp hơn " + format(floor - sale) + " ₫"
                                : "Sales đã sửa giá từ giá sàn trở lên, không cần duyệt", floor > sale));
            }
            default -> List.of();
        };
    }

    private String otherOpenConsignment(Consignment consignment) {
        return consignmentRepository.findByVehicleIdOrderByIdDesc(consignment.getVehicle().getId()).stream()
                .filter(c -> !c.getId().equals(consignment.getId()) && !ENDED.contains(c.getStatus()))
                .map(Consignment::getCode)
                .findFirst()
                .orElse("");
    }

    private String buyerHold(Consignment consignment) {
        Long vehicleId = consignment.getVehicle().getId();
        List<String> codes = new ArrayList<>(consignmentRepository.findReservationCodes(vehicleId, ReservationStatus.ACTIVE));
        codes.addAll(consignmentRepository.findContractCodes(vehicleId, ContractStatus.CANCELLED));
        return String.join(", ", codes);
    }

    private static List<Map<String, String>> effects(ApprovalType type) {
        return switch (type) {
            case CONSIGNMENT -> effects("Hồ sơ thành Đã duyệt, Kiểm định viên tiếp nhận được xe.",
                    "Hồ sơ đóng lại; hợp đồng ký gửi chưa có hiệu lực của hồ sơ này chấm dứt theo.",
                    "Hồ sơ về Bản nháp để Nhân viên ký gửi sửa rồi gửi duyệt lại.");
            case AGREEMENT -> effects("Hợp đồng có hiệu lực, hoa hồng và giá sàn được dùng khi quyết toán.",
                    "Hợp đồng chấm dứt và hồ sơ ký gửi đóng lại.",
                    "Hợp đồng về Bản nháp để sửa rồi gửi duyệt lại.");
            case WITHDRAWAL -> effects("Hồ sơ thành Đã rút, hợp đồng ký gửi chấm dứt, tin đăng dừng để trả xe cho chủ.",
                    "Hồ sơ quay lại đúng bước trước khi xin rút, kể cả đã đặt cọc hoặc đã bán, xe tiếp tục giao dịch.",
                    "Hồ sơ quay lại đúng bước trước khi xin rút để Nhân viên ký gửi bổ sung rồi gửi lại.");
            case BELOW_FLOOR_PRICE -> effects("Sales được ký hợp đồng bán với đúng mức giá này.",
                    "Sales phải sửa giá bán từ giá sàn trở lên mới ký được.",
                    "Sales bổ sung lý do rồi bấm Ký lại để gửi lại yêu cầu.");
            default -> effects("Chỉ ghi nhận quyết định, không tự đổi dữ liệu khác.",
                    "Chỉ ghi nhận quyết định, không tự đổi dữ liệu khác.",
                    "Chỉ ghi nhận quyết định, không tự đổi dữ liệu khác.");
        };
    }

    private static List<Map<String, String>> effects(String approved, String rejected, String needMore) {
        return List.of(pair("Duyệt", approved), pair("Từ chối", rejected), pair("Yêu cầu bổ sung", needMore));
    }

    private static Map<String, Object> ownerSection(Consignor owner) {
        return section("Chủ xe", "user",
                pair("Họ tên", owner.getFullName()),
                pair("SĐT", blankTo(owner.getPhone(), "Chưa nhập")),
                pair("Số CCCD", blankTo(owner.getIdNumber(), "Chưa nhập")));
    }

    private static Map<String, Object> carSection(Consignment consignment) {
        Vehicle vehicle = consignment.getVehicle();
        return section("Xe ký gửi", "car",
                pair("Xe", carLabel(vehicle)),
                pair("Biển số", blankTo(vehicle.getLicensePlate(), "Chưa có")),
                pair("VIN", blankTo(vehicle.getVin(), "Chưa nhập")),
                pair("Số km", vehicle.getOdometerKm() == null ? "Chưa nhập" : format(vehicle.getOdometerKm()) + " km"),
                pair("Giá kỳ vọng", format(money(consignment.getExpectedPrice())) + " ₫"));
    }

    @SafeVarargs
    private static Map<String, Object> section(String title, String icon, Map<String, String>... rows) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("title", title);
        section.put("icon", icon);
        section.put("rows", List.of(rows));
        return section;
    }

    private static Map<String, String> pair(String label, String value) {
        return Map.of("label", label, "value", value == null ? "" : value);
    }

    private static Map<String, String> check(String label, String note, boolean passed) {
        return Map.of("label", label, "note", note == null ? "" : note, "passed", String.valueOf(passed));
    }

    /** tone là màu thẻ KPI trong sales/deal-parts, pill là màu nhãn trạng thái. */
    private static Map<String, String> meta(String label, String tone, String icon, String pill) {
        return Map.of("label", label, "tone", tone, "icon", icon, "pill", pill);
    }

    private Approval load(Long id) {
        return approvalRepository.findByIdFetched(id)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu phê duyệt", id));
    }

    private static String carLabel(Vehicle vehicle) {
        String label = vehicle.getDisplayLabel();
        return vehicle.getYear() == null ? label : label + " " + vehicle.getYear();
    }

    private static String plate(Vehicle vehicle) {
        return blankTo(vehicle.getLicensePlate(), "");
    }

    /** Ngày giờ địa phương không kèm múi giờ, để template đọc được bằng #temporals.createDateTime. */
    private static String time(Instant instant) {
        return instant == null ? "" : LocalDateTime.ofInstant(instant, ZONE).withNano(0).toString();
    }

    private static String day(LocalDate date) {
        return date == null ? "Chưa nhập" : DAY.format(date);
    }

    private static long money(BigDecimal value) {
        return value == null ? 0L : value.longValue();
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static String percent(BigDecimal rate) {
        return rate == null ? "Chưa nhập" : rate.stripTrailingZeros().toPlainString().replace('.', ',') + "%";
    }

    private static String format(long value) {
        return String.format(Locale.US, "%,d", value).replace(',', '.');
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankTo(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }
}
